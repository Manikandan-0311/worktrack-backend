package com.spearhead.ufc.dto;

public class RoleDTO {
	private Integer roleId;
	private String roleName;
	private String description;
	private Boolean isActive;
	private OrgLiteDTO org;
	private String branchName;

	public Integer getRoleId() {
		return roleId;
	}

	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public OrgLiteDTO getOrg() {
		return org;
	}

	public void setOrg(OrgLiteDTO org) {
		this.org = org;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
