/**
 * 
 */
package com.spearhead.ufc.dto;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * @author manikandan.m it for showing list of permission permission
 */

@Data
public class SpecialPermissionDTO {

	private Integer permissionId;
	private String reason;
	private Integer employeeId;
	private Date permissionDate;
	private Integer permissionStatusId;
	private String remarks;
	private Integer grantedBy;
	private Integer createdBy;
	private LocalDateTime createdDt;
	private String permissionType;
	private Integer locationId;
	private Integer departmentId;

	// Constructor order must match native query select (12 params)
	public SpecialPermissionDTO(Integer permissionId, String reason, Integer employeeId, Date permissionDate,
			Integer permissionStatusId, String remarks, Integer grantedBy, Integer createdBy, LocalDateTime createdDt,
			String permissionType, Integer locationId, Integer departmentId) {
		this.permissionId = permissionId;
		this.reason = reason;
		this.employeeId = employeeId;
		this.permissionDate = permissionDate;
		this.permissionStatusId = permissionStatusId;
		this.remarks = remarks;
		this.grantedBy = grantedBy;
		this.createdBy = createdBy;
		this.createdDt = createdDt;
		this.permissionType = permissionType;
		this.locationId = locationId;
		this.departmentId = departmentId;
	}

	// ✅ Getters & Setters
	public Integer getPermissionId() {
		return permissionId;
	}

	public String getReason() {
		return reason;
	}

	// removed userId; using employeeId

	public Date getPermissionDate() {
		return permissionDate;
	}

	public Integer getPermissionStatusId() {
		return permissionStatusId;
	}

	public String getRemarks() {
		return remarks;
	}

	public Integer getGrantedBy() {
		return grantedBy;
	}

	public Integer getCreatedBy() {
		return createdBy;
	}

	public LocalDateTime getCreatedDt() {
		return createdDt;
	}

	public String getPermissionType() {
		return permissionType;
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public Integer getLocationId() {
		return locationId;
	}

	public Integer getDepartmentId() {
		return departmentId;
	}
}