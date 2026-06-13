package com.spearhead.ufc.dto;

import java.io.Serializable;

public class OrgLiteDTO implements Serializable {
	private Integer orgId;
	private String orgName;

	private String branchName;

	public OrgLiteDTO() {}

	public Integer getOrgId() {
		return orgId;
	}

	public void setOrgId(Integer orgId) {
		this.orgId = orgId;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getBranchName() {
		return branchName;
	}	
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
}
