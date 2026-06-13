package com.spearhead.ufc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dashboard_summary", schema = "dashboard")
public class DashboardSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "org_id")
    private Integer orgId;
    
    @Column(name = "location_id")
    private Integer locationId;
    
    @Column(name = "department_id")
    private Integer departmentId;
    
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "summary_date")
    private LocalDate summaryDate;

    @Column(name = "total_employees")
    private Integer totalEmployees;

    @Column(name = "submitted_employees")
    private Integer submittedEmployees;

    @Column(name = "compliance_percentage")
    private BigDecimal compliancePercentage;

    @Column(name = "total_incentive_amount")
    private BigDecimal totalIncentiveAmount;

    @Column(name = "total_deduction_amount")
    private BigDecimal totalDeductionAmount;

    @Column(name = "sp_leave_count")
    private Integer spLeaveCount;

    @Column(name = "top_skipped_questions")
    private String topSkippedQuestions;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_dt")
    private LocalDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;
    
}
