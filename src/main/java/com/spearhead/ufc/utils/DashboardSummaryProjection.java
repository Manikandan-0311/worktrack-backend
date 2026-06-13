package com.spearhead.ufc.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DashboardSummaryProjection {

    Integer getSummaryId();
    Integer getOrgId();
    Integer getLocationId();
    String getLocationName();
    Integer getDepartmentId();
    String getDepartmentName();
    Integer getEmployeeId();
    LocalDate getSummaryDate();
    Integer getTotalEmployees();
    Integer getSubmittedEmployees();
    Double getCompliancePercentage();
    Double getTotalIncentiveAmount();
    Double getTotalDeductionAmount();
    Integer getSpLeaveCount();
    String getTopSkippedQuestions();
    Integer getCreatedBy();
    LocalDateTime getCreatedDt();
    Integer getUpdatedBy();
    LocalDateTime getUpdatedDt();
}
