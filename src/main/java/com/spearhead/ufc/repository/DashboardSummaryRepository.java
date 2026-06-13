package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.DashboardSummary;
import com.spearhead.ufc.utils.DashboardSummaryProjection;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DashboardSummaryRepository extends JpaRepository<DashboardSummary, Long> {

	// Query for all parameters provided, including date range filtering by
	@Query("""
			SELECT ds FROM DashboardSummary ds
			WHERE ds.orgId = :orgId
			AND ds.summaryDate >= COALESCE(:startDate, ds.summaryDate)
			AND ds.summaryDate <= COALESCE(:endDate, ds.summaryDate)
			AND (:locationId IS NULL OR ds.locationId = :locationId)
			AND (:departmentId IS NULL OR ds.departmentId = :departmentId)
			AND (:employeeId IS NULL OR ds.employeeId = :employeeId)
			""")
	List<DashboardSummary> findByFilters(@Param("orgId") Integer orgId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate, @Param("locationId") Integer locationId,
			@Param("departmentId") Integer departmentId, @Param("employeeId") Integer employeeId);

	@Query("""
			    SELECT
			        ds.summaryId AS summaryId,
			        ds.orgId AS orgId,
			        ds.locationId AS locationId,
			        l.locationName AS locationName,
			        ds.departmentId AS departmentId,
			        d.departmentName AS departmentName,
			        ds.employeeId AS employeeId,
			        ds.summaryDate AS summaryDate,
			        ds.totalEmployees AS totalEmployees,
			        ds.submittedEmployees AS submittedEmployees,
			        ds.compliancePercentage AS compliancePercentage,
			        ds.totalIncentiveAmount AS totalIncentiveAmount,
			        ds.totalDeductionAmount AS totalDeductionAmount,
			        ds.spLeaveCount AS spLeaveCount,
			        ds.topSkippedQuestions AS topSkippedQuestions,
			        ds.createdBy AS createdBy,
			        ds.createdDt AS createdDt,
			        ds.updatedBy AS updatedBy,
			        ds.updatedDt AS updatedDt
			    FROM DashboardSummary ds
			    LEFT JOIN Location l ON ds.locationId = l.locationId
			    LEFT JOIN Department d ON ds.departmentId = d.departmentId
			    WHERE ds.orgId = :orgId
			    AND ds.summaryDate >= COALESCE(:startDate, ds.summaryDate)
			    AND ds.summaryDate <= COALESCE(:endDate, ds.summaryDate)
			    AND (:locationId IS NULL OR ds.locationId = :locationId)
			    AND (:departmentId IS NULL OR ds.departmentId = :departmentId)
			    AND (:employeeId IS NULL OR ds.employeeId = :employeeId)
			""")
	List<DashboardSummaryProjection> findByFiltersWithNames(@Param("orgId") Integer orgId,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
			@Param("locationId") Integer locationId, @Param("departmentId") Integer departmentId,
			@Param("employeeId") Integer employeeId);

	// Other queries for specific combinations of parameters (without date range)
	List<DashboardSummary> findByOrgIdAndLocationIdAndDepartmentIdAndEmployeeId(Integer orgId, Integer locationId,
			Integer departmentId, Integer employeeId);

	List<DashboardSummary> findByOrgIdAndLocationIdAndDepartmentId(Integer orgId, Integer locationId,
			Integer departmentId);

	List<DashboardSummary> findByOrgIdAndLocationIdAndEmployeeId(Integer orgId, Integer locationId, Integer employeeId);

	List<DashboardSummary> findByOrgIdAndDepartmentIdAndEmployeeId(Integer orgId, Integer departmentId,
			Integer employeeId);

	List<DashboardSummary> findByOrgId(Integer orgId);
	
	// ---------------------------------------------------------------------------------------------------------
	
	// Today compliance
    @Query("""
        SELECT ROUND(AVG(ds.compliancePercentage),2)
        FROM DashboardSummary ds
        WHERE ds.summaryDate = CURRENT_DATE
          AND ds.orgId = :orgId
          AND (:locationId IS NULL OR ds.locationId = :locationId)
          AND (:departmentId IS NULL OR ds.departmentId = :departmentId)
    """)
    Double getTodayCompliance(@Param("orgId") Integer orgId,
                              @Param("locationId") Integer locationId,
                              @Param("departmentId") Integer departmentId);

    // Top performers
    @Query("""
        SELECT ds.employeeId AS employeeId, ROUND(AVG(ds.compliancePercentage),2) AS avgCompliance
        FROM DashboardSummary ds
        WHERE ds.summaryDate >= FUNCTION('date_trunc','month',CURRENT_DATE)
          AND ds.summaryDate <= CURRENT_DATE
          AND ds.orgId = :orgId
          AND (:locationId IS NULL OR ds.locationId = :locationId)
          AND (:departmentId IS NULL OR ds.departmentId = :departmentId)
        GROUP BY ds.employeeId
        HAVING AVG(ds.compliancePercentage) >= 95
        ORDER BY avgCompliance DESC
    """)
    List<Object[]> getTopPerformers(@Param("orgId") Integer orgId,
                                    @Param("locationId") Integer locationId,
                                    @Param("departmentId") Integer departmentId);

    // Daily aggregates for charts
    @Query("""
        SELECT ds.summaryDate AS summaryDate,
               ROUND(AVG(ds.compliancePercentage),2) AS avgCompliance,
               SUM(ds.submittedEmployees) AS submitted,
               SUM(ds.totalEmployees) AS total,
               ROUND(SUM(ds.submittedEmployees)/SUM(ds.totalEmployees)*100,2) AS submissionPercentage,
               SUM(ds.totalIncentiveAmount) AS totalIncentive,
               SUM(ds.totalDeductionAmount) AS totalDeduction
        FROM DashboardSummary ds
        WHERE ds.summaryDate >= FUNCTION('date_trunc','month',CURRENT_DATE)
          AND ds.summaryDate <= CURRENT_DATE
          AND ds.orgId = :orgId
          AND (:locationId IS NULL OR ds.locationId = :locationId)
          AND (:departmentId IS NULL OR ds.departmentId = :departmentId)
        GROUP BY ds.summaryDate
        ORDER BY ds.summaryDate
    """)
    List<Object[]> getDailyAggregates(@Param("orgId") Integer orgId,
                                      @Param("locationId") Integer locationId,
                                      @Param("departmentId") Integer departmentId);

    // Department-wise compliance
    @Query("""
        SELECT ds.departmentId AS departmentId,
               d.departmentName AS departmentName,
               ROUND(AVG(ds.compliancePercentage),2) AS avgCompliance
        FROM DashboardSummary ds
        LEFT JOIN Department d ON ds.departmentId = d.departmentId
        WHERE ds.summaryDate >= FUNCTION('date_trunc','month',CURRENT_DATE)
          AND ds.summaryDate <= CURRENT_DATE
          AND ds.orgId = :orgId
          AND (:locationId IS NULL OR ds.locationId = :locationId)
        GROUP BY ds.departmentId, d.departmentName
        ORDER BY avgCompliance DESC
    """)
    List<Object[]> getDepartmentWiseCompliance(@Param("orgId") Integer orgId,
                                               @Param("locationId") Integer locationId);
}
