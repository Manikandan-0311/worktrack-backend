package com.spearhead.ufc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DashboardSummaryDTO {

    private Integer summaryId;
    private Integer orgId;
    private Integer locationId;
    private String locationName;
    private Integer departmentId;
    private String departmentName;
    private Integer employeeId;
    private LocalDate summaryDate;
    private Integer totalEmployees;
    private Integer submittedEmployees;
    private BigDecimal compliancePercentage;
    private BigDecimal totalIncentiveAmount;
    private BigDecimal totalDeductionAmount;
    private Integer spLeaveCount;
    private String topSkippedQuestions;
    private Integer createdBy;
    private LocalDateTime createdDt;
    private Integer updatedBy;
    private LocalDateTime updatedDt;

    public DashboardSummaryDTO(Integer summaryId, Integer orgId, Integer locationId, String locationName,
                               Integer departmentId, String departmentName, Integer employeeId,
                               LocalDate summaryDate, Integer totalEmployees, Integer submittedEmployees,
                               BigDecimal compliancePercentage, BigDecimal totalIncentiveAmount, BigDecimal totalDeductionAmount,
                               Integer spLeaveCount, String topSkippedQuestions, Integer createdBy,
                               LocalDateTime createdDt, Integer updatedBy, LocalDateTime updatedDt) {
        this.summaryId = summaryId;
        this.orgId = orgId;
        this.locationId = locationId;
        this.locationName = locationName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.employeeId = employeeId;
        this.summaryDate = summaryDate;
        this.totalEmployees = totalEmployees;
        this.submittedEmployees = submittedEmployees;
        this.compliancePercentage = compliancePercentage;
        this.totalIncentiveAmount = totalIncentiveAmount;
        this.totalDeductionAmount = totalDeductionAmount;
        this.spLeaveCount = spLeaveCount;
        this.topSkippedQuestions = topSkippedQuestions;
        this.createdBy = createdBy;
        this.createdDt = createdDt;
        this.updatedBy = updatedBy;
        this.updatedDt = updatedDt;
    }

	public Integer getSummaryId() {
		return summaryId;
	}

	public void setSummaryId(Integer summaryId) {
		this.summaryId = summaryId;
	}

	public Integer getOrgId() {
		return orgId;
	}

	public void setOrgId(Integer orgId) {
		this.orgId = orgId;
	}

	public Integer getLocationId() {
		return locationId;
	}

	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Integer getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Integer employeeId) {
		this.employeeId = employeeId;
	}

	public LocalDate getSummaryDate() {
		return summaryDate;
	}

	public void setSummaryDate(LocalDate summaryDate) {
		this.summaryDate = summaryDate;
	}

	public Integer getTotalEmployees() {
		return totalEmployees;
	}

	public void setTotalEmployees(Integer totalEmployees) {
		this.totalEmployees = totalEmployees;
	}

	public Integer getSubmittedEmployees() {
		return submittedEmployees;
	}

	public void setSubmittedEmployees(Integer submittedEmployees) {
		this.submittedEmployees = submittedEmployees;
	}

	public BigDecimal getCompliancePercentage() {
		return compliancePercentage;
	}

	public void setCompliancePercentage(BigDecimal compliancePercentage) {
		this.compliancePercentage = compliancePercentage;
	}

	public BigDecimal getTotalIncentiveAmount() {
		return totalIncentiveAmount;
	}

	public void setTotalIncentiveAmount(BigDecimal totalIncentiveAmount) {
		this.totalIncentiveAmount = totalIncentiveAmount;
	}

	public BigDecimal getTotalDeductionAmount() {
		return totalDeductionAmount;
	}

	public void setTotalDeductionAmount(BigDecimal totalDeductionAmount) {
		this.totalDeductionAmount = totalDeductionAmount;
	}

	public Integer getSpLeaveCount() {
		return spLeaveCount;
	}

	public void setSpLeaveCount(Integer spLeaveCount) {
		this.spLeaveCount = spLeaveCount;
	}

	public String getTopSkippedQuestions() {
		return topSkippedQuestions;
	}

	public void setTopSkippedQuestions(String topSkippedQuestions) {
		this.topSkippedQuestions = topSkippedQuestions;
	}

	public Integer getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Integer createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(LocalDateTime createdDt) {
		this.createdDt = createdDt;
	}

	public Integer getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Integer updatedBy) {
		this.updatedBy = updatedBy;
	}

	public LocalDateTime getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(LocalDateTime updatedDt) {
		this.updatedDt = updatedDt;
	}

    // Getters & Setters...
    
}
