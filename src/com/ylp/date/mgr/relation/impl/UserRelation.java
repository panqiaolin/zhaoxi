package com.ylp.date.mgr.relation.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ylp.date.mgr.relation.IRelation;

@Entity
@Table(name = "DATE_RELATION")
public class UserRelation implements IRelation {
	@Column(name = "ID_", nullable = false)
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private String id;
	@Column(name = "PART_", nullable = false)
	private String one;
	@Column(name = "OTHREPART_", nullable = false)
	private String otherOne;
	@Column(name = "TYPE_", nullable = false)
	private int type;
	@Column(name = "RECOGNITION _", nullable = false)
	private int recognition = 1;

	public String getId() {
		return id;
	}

	public String getCaption() {
		return id;
	}

	public void setId(String id) {
		this.id = id;

	}

	public String getOne() {
		return one;
	}

	public String getOtherOne() {
		return otherOne;
	}

	public int getType() {
		return type;
	}

	public int getRecognition() {
		return recognition;
	}

	public void setOne(String one) {
		this.one = one;
	}

	public void setOtherOne(String otherOne) {
		this.otherOne = otherOne;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setRecognition(int recognition) {
		this.recognition = recognition;
	}

}