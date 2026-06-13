package com.spearhead.ufc.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

	private Integer employeeId;

	// For saving
	private Integer orgId;
	private Integer locationId;
	private Integer departmentId;
	private Integer roleId;

	// For listing
	private String locationName;
	private String departmentName;
	private String roleName;

	private String employeeCode;
	private String firstName;
	private String lastName;
	private String emailId;
	private String phoneNumber;
	private Boolean isActive;
	private LocalDate joinDate; // join date
	private LocalDate relieveDate; // relieving date, can be null for new employees
	private String remarks;
	private Boolean specialPermissionFlag;

	// Constructor for your JPQL query (list view)
	public EmployeeDTO(Integer employeeId, String employeeCode, String firstName, String lastName, String emailId,
			String phoneNumber, Boolean isActive, LocalDate joinDate, LocalDate relieveDate, Integer locationId,
			Integer orgId, String locationName, Integer departmentId, String departmentName, Integer roleId,
			String roleName, String remarks, Boolean specialPermissionFlag) {
		this.employeeId = employeeId;
		this.employeeCode = employeeCode;
		this.firstName = firstName;
		this.lastName = lastName;
		this.emailId = emailId;
		this.phoneNumber = phoneNumber;
		this.isActive = isActive;
		this.joinDate = joinDate;
		this.relieveDate = relieveDate;
		this.orgId = orgId;
		this.locationId = locationId;
		this.locationName = locationName;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
		this.roleId = roleId;
		this.roleName = roleName;
		this.remarks = remarks;
		this.specialPermissionFlag = specialPermissionFlag;
	}

}
