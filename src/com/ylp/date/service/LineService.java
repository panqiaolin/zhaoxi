package com.ylp.date.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.SortType;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ylp.date.login.Login;
import com.ylp.date.mgr.IBaseObj;
import com.ylp.date.mgr.ObjListener;
import com.ylp.date.mgr.relation.IRelation;
import com.ylp.date.mgr.user.IUser;
import com.ylp.date.server.Server;
import com.ylp.date.server.SpringNames;

/**
 * 用于连线的相关服务入口
 * 
 * @author Qiaolin Pan
 * 
 */
@Component(SpringNames.LineService)
@DependsOn({ SpringNames.Server, SpringNames.ServerConfigRation })
@Lazy(false)
public class LineService implements ObjListener, Runnable {
	private static final int ONE_DAY = 1000 * 60 * 60 * 24;
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private Lock read = lock.readLock();
	private Lock write = lock.writeLock();
	/**
	 * 其id 是一个md5值 通过所有的用户id求出的MD5
	 */
	private Map<String, LineUsersObj> userPool;
	private int defaultLength;
	private SortedMap<String, Integer> userDisplay;
	private boolean isMaleFulled;
	private boolean isFemaleFulled;
	private Date today;
	private Random random;

	public void init() {
		write.lock();
		try {
			defaultLength = Server.getInstance().getConfigRation()
					.getLineLength();
			int max = Math.max(16, defaultLength);
			userPool = new HashMap<String, LineUsersObj>(max);
			userDisplay = new TreeMap<String, Integer>();
			run();
		} finally {
			write.unlock();
		}
	}

	/**
	 * 
	 * @param login
	 * @return
	 */
	LineUsersObj getLineUser(Login login) {
		read.lock();
		try {
			if (!login.isLogined()) {
				return null;
			}
			String id = login.getUser().getId();
			// 有一次都还没有展示的 那么 展示此组
			if (userDisplay.size() != userPool.size()) {
				String key = null;
				for (Map.Entry<String, LineUsersObj> entry : userPool
						.entrySet()) {
					String key2 = entry.getKey();
					if (userDisplay.containsKey(key2)) {
						continue;
					}
					if (entry.getValue().contains(id)) {
						continue;
					}
					key = key2;

				}
				if (StringUtils.isNotEmpty(key)) {
					userDisplay.put(key, 1);
					return userPool.get(key);
				}
			}
			// 否则 随机选取一组 有两种情况 ：1.所有的组都未被展示 2.所有的组都已被展示
			if (userDisplay.isEmpty()) {
				for (Map.Entry<String, LineUsersObj> entry : userPool
						.entrySet()) {
					LineUsersObj value = entry.getValue();
					if (value.contains(id)) {
						continue;
					}
					userDisplay.put(entry.getKey(), 1);
					return value;
				}
				return null;
			} else {
				for (Map.Entry<String, Integer> entry : userDisplay.entrySet()) {
					String key2 = entry.getKey();
					LineUsersObj lineUsersObj = userPool.get(key2);
					if (lineUsersObj.contains(id)) {
						continue;
					}
					userDisplay.put(key2, entry.getValue() + 1);
					return lineUsersObj;
				}
			}
		} finally {
			read.unlock();
		}
		return null;
	}

	@Override
	public void fileAdd(IBaseObj obj) {
		write.lock();
		try {
			if (!(obj instanceof IUser)) {
				return;
			}
			IUser user = (IUser) obj;
			String id = obj.getId();
			for (Map.Entry<String, LineUsersObj> entry : userPool.entrySet()) {
				LineUsersObj value = entry.getValue();
				if (value.contains(id)) {
					return;
				}
				int gender = user.getGender();
				int femaleSize = value.getFemale().size();
				int maleSize = value.getMale().size();
				if ((!value.isFemaleFulled() && gender == IUser.FEMALE)
						&& maleSize - femaleSize == 1) {
					value.addUser(user);
					return;
				}
				if ((!value.isMaleFulled() && gender == IUser.MALE)
						&& femaleSize - maleSize == 1) {
					value.addUser(user);
					return;
				}
			}
			if (userPool.size() != defaultLength) {
				LineUsersObj lineobj = new LineUsersObj();
				lineobj.addUser(user);
				userPool.put(lineobj.getKey(), lineobj);
				return;

			}
		} finally {
			write.unlock();
		}
	}

	@Override
	public void fireRemove(String id) {
		write.lock();
		try {
			List<String> keys = new ArrayList<String>();
			for (Map.Entry<String, LineUsersObj> entry : userPool.entrySet()) {
				LineUsersObj value = entry.getValue();
				if (value.contains(id)) {
					keys.add(entry.getKey());
				}
			}
			if (keys.isEmpty()) {
				return;
			}
			for (String string : keys) {
				userPool.remove(string);
				userDisplay.remove(string);
			}
		} finally {
			write.unlock();
		}
	}

	@Override
	/**
	 * 1）从注册用户中随机出4男4女编为一组。男女通过性别标签来分辨。
	 * （2）如果该组中某对男女被连线达到3次，则配对成功。同时该组合解散，剩余3男3女返回数据库，重新生成其他组。
	 * （3）每个用户每天可以最多查看并连线3组，每组中只能连线一对男女。
	 * （4）用户自己每天最多被匹配成功一次。如果用户连着3天没有被匹配成功，则在第四天抽取时
	 * ，先只从这批人里抽取。即系统每天凌晨抽取时，要先从连着三天没有匹配成功的人中生成组来匹配。
	 * （4）系统每天凌晨自动生成5组，5组随机展现给用户
	 * 。每匹配成功一组，则系统自动生成新的一组。新生成的一组仍然从没有匹配成功的人中抽取，直至匹配成功的人数占连续3天未匹配人数的一半
	 * 。也可以直接定个数值，如直至匹配成功15人。然后再从整体用户中随机匹配。 
	 * （5）从整体用户中抽取时需排除掉3类人: a.今日已配对成功的；
	 * b.正在其他组中展示的；
	 * c.已配对成功但一直没有查看的。即如果用户配对成功，但是一直没有登录查看，则不再将用户进行新的匹配（抽取时不抽取他
	 * /她）。这样也能保证用户每次打开网站，最多只有一个匹配对象。
	 */
	public void run() {
		write.lock();
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			today = cal.getTime();
			// 设计的随机算法原则如下：
			// 1.根据shownum降序
			List<IBaseObj> firstMale = getFirst(true);
			List<IBaseObj> firstFemale = getFirst(false);
			adjustUsers(firstMale, true);
			adjustUsers(firstFemale, true);
			// 如果上述完成 返回
			if (isMaleFulled && isFemaleFulled) {
				return;
			}
			// 否则 往池中插入男或者女
			if (isMaleFulled) {
				handleWithFemale();
				return;
			} else {
				handWithMale();
				if (!isFemaleFulled) {
					handleWithFemale();
				}
				return;
			}
		} finally {
			write.unlock();
		}
	}

	private void handWithMale() {
		// 查询出 所有没有匹配成功
		List<IBaseObj> users = getUsersSecond(true);
		List<Integer> selectedIndexes = new ArrayList<Integer>();
		boolean needCreate = userPool.size() != defaultLength;
		while (true) {
			int index = genarateIndex(users.size());
			while (!selectedIndexes.contains(index)) {
				selectedIndexes.add(index);
			}
			IUser user = (IUser) users.get(index);
			String userId = user.getId();
			for (Map.Entry<String, LineUsersObj> lineUsers : userPool
					.entrySet()) {
				LineUsersObj value = lineUsers.getValue();
				if (value.contains(userId)) {
					continue;
				}
				if (!value.isMaleFulled()) {
					value.addUser(user);
				}
				String key = lineUsers.getKey();
				userPool.put(key, value);
				break;
			}
			if (allFulled(false)) {
				break;
			}
			if (selectedIndexes.size() == users.size()) {
				break;
			}
		}
		int index = genarateIndex(users.size());
		while (needCreate && selectedIndexes.size() != users.size()) {
			while (!selectedIndexes.contains(index)) {
				selectedIndexes.add(index);
			}
			IUser user = (IUser) users.get(index);
			LineUsersObj obj = new LineUsersObj();
			obj.addUser(user);
			userPool.put(obj.getKey(), obj);
			needCreate = userPool.size() != defaultLength;
		}
	}

	private void handleWithFemale() {
		// 查询出 所有没有匹配成功
		List<IBaseObj> users = getUsersSecond(false);
		List<Integer> selectedIndexes = new ArrayList<Integer>();
		// 是否需要创建
		boolean needCreate = userPool.size() != defaultLength;
		// 循环退出条件是 line池满了 或者所有用户都已经遍历一边
		while (true) {
			int index = genarateIndex(users.size());
			while (!selectedIndexes.contains(index)) {
				selectedIndexes.add(index);
			}
			IUser user = (IUser) users.get(index);
			String userId = user.getId();
			for (Map.Entry<String, LineUsersObj> lineUsers : userPool
					.entrySet()) {
				LineUsersObj value = lineUsers.getValue();
				if (value.contains(userId)) {
					continue;
				}
				if (!value.isFemaleFulled()) {
					value.addUser(user);
				}
				String key = lineUsers.getKey();
				userPool.put(key, value);
				break;
			}
			if (allFulled(false)) {
				break;
			}
			if (selectedIndexes.size() == users.size()) {
				break;
			}
		}
		// 如果需要创建的情况
		int index = genarateIndex(users.size());
		while (needCreate && selectedIndexes.size() != users.size()) {
			while (!selectedIndexes.contains(index)) {
				selectedIndexes.add(index);
			}
			IUser user = (IUser) users.get(index);
			LineUsersObj obj = new LineUsersObj();
			obj.addUser(user);
			userPool.put(obj.getKey(), obj);
			needCreate = userPool.size() != defaultLength;
		}
	}

	private boolean allFulled(boolean b) {
		for (Map.Entry<String, LineUsersObj> lineUsers : userPool.entrySet()) {
			LineUsersObj value = lineUsers.getValue();
			if (!value.isFemaleFulled()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 产生一个随机数
	 * 
	 * @param size
	 * @return
	 */
	private int genarateIndex(int size) {
		random = new java.util.Random();
		return random.nextInt(size);
	}

	private List<IBaseObj> getUsersSecond(boolean b) {
		String hql = "select * from User user_ "
				+ "where user_.gender=? and"
				// 联系三天连线未成功
				+ " (user_.lastLine is not null and  user_.lastLine <?) and"
				+ " user_.id not in "
				+ "("
				// 有连线成功，没有确认的人
				+ "select rel_.one from UserRelation rel_ where rel_.type=? and  rel_.recognition=? and rel_.oneReg is  null "
				+ "union "
				+ "select rel_.otherOne from UserRelation rel_ where rel_.type=? and  rel_.recognition=? and rel_.otherOneReg is  null"
				+ ") order by user_.lastShowNum";
		List<Object> list = new ArrayList<Object>(5);
		list.add(b ? IUser.MALE : IUser.FEMALE);
		list.add(new Date(System.currentTimeMillis() - 2 * ONE_DAY));
		list.add(IRelation.TYPE_LINE);
		list.add(IRelation.RECOG_LINE);
		list.add(IRelation.TYPE_LINE);
		list.add(IRelation.RECOG_LINE);
		return Server.getInstance().userMgr().executeQuery(hql, list.toArray());
	}

	private void adjustUsers(List<IBaseObj> firstMale, boolean b) {
		// 男性时候 进行创建
		if (b) {
			int size = firstMale.size();
			if (userPool.isEmpty()) {
				LineUsersObj obj = new LineUsersObj();
				for (int i = 0; i <= size; i++) {
					if (i != size) {
						obj.addUser((IUser) firstMale.get(i));
					}
					if ((i % 4 == 0 && i != 0) || i == size) {
						userPool.put(obj.getKey(), obj);
						obj = new LineUsersObj();
					}
				}
			}
		} else {
			// 女性时加入
			int count = 0;
			for (Map.Entry<String, LineUsersObj> lineUsers : userPool
					.entrySet()) {
				for (int i = 0; i <= 4; i++) {
					LineUsersObj value = lineUsers.getValue();
					if (count != firstMale.size()) {
						value.addUser((IUser) firstMale.get(count));
					}
					if ((count % 4 == 0 && count != 0)
							|| count == firstMale.size()) {
						userPool.put(lineUsers.getKey(), value);
					}
					count++;
				}
			}
		}

	}

	/**
	 * 获取三天没有匹配成功，且没有任何已成立的关系没有查看的人
	 * 
	 * @param b
	 * @return
	 */
	private List<IBaseObj> getFirst(boolean b) {
		String hql = "select * from User user_ "
				+ "where user_.gender=? and"
				// 联系三天连线未成功
				+ " (user_.lastLine is null or user_.lastLine <?) and"
				+ " user_.id not in "
				+ "("
				// 有连线成功，没有确认的人
				+ "select rel_.one from UserRelation rel_ where rel_.type=? and  rel_.recognition=? and rel_.oneReg is  null "
				+ "union "
				+ "select rel_.otherOne from UserRelation rel_ where rel_.type=? and  rel_.recognition=? and rel_.otherOneReg is  null"
				+ ") order by user_.lastShowNum";
		List<Object> list = new ArrayList<Object>(5);
		list.add(b ? IUser.MALE : IUser.FEMALE);
		list.add(new Date(System.currentTimeMillis() - 2 * ONE_DAY));
		list.add(IRelation.TYPE_LINE);
		list.add(IRelation.RECOG_LINE);
		list.add(IRelation.TYPE_LINE);
		list.add(IRelation.RECOG_LINE);
		return Server.getInstance().userMgr().executeQuery(hql, list.toArray());
	}

	@Override
	public void fireUpdate(String id, IBaseObj old, IBaseObj newObj) {
		write.lock();
		try {
			if (!(old instanceof IUser)) {
				return;
			}
			if (!(newObj instanceof IUser)) {
				return;
			}
			IUser newUser = (IUser) newObj;
			Date lastLine = newUser.getLastLine();
			if (lastLine == null) {
				return;
			}
			if (lastLine.after(today)) {
				this.fireRemove(id);
			}
		} finally {
			write.unlock();
		}

	}
}