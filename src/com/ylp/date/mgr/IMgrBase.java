package com.ylp.date.mgr;

import java.util.List;


/**
 * ��������ӿ�
 * 
 * @author Qiaolin Pan
 * 
 */
public interface IMgrBase {
	IBaseObj getObj(String id);
	List<IBaseObj> list();
	List<IBaseObj> list(PageCondition page);
	List<IBaseObj> list(PageCondition  page,Condition cond);
}
