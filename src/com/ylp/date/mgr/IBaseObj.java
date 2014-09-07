package com.ylp.date.mgr;

/**
 * �������ڹ������ĸ����󣬴˶����ṩ��������getId��getCaption
 * 
 * @author Qiaolin Pan
 * 
 */
public interface IBaseObj {
	/**
	 * ��ȡ�����id <br/>
	 * ��idӦ�þ���"Ψһ��" �� �ڸö�������У�idӦ���ǿ�����Ϊkey
	 * @return �����ΨһID
	 */
	String getId();

	/**
	 * ��ȡ����ı���,"����"һ������������ʾ<br/>
	 * ��� �ö���û��"����"����ô�Ƽ����ظö����id
	 * @see IBaseObj#getId()
	 * @return ����ı���
	 */
	String getCaption();
}
