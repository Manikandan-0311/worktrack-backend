/**
 * 
 */
package com.spearhead.ufc.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.spearhead.ufc.model.Submission;

/**
 * @author manikandan.m Submission Data interface
 */

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

	@Query("SELECT s FROM Submission s WHERE s.employee.employeeId = :employeeId AND CAST(s.createdDt AS DATE) = :date")
	List<Submission> findByEmployeeIdAndCreatedDtDate(@Param("employeeId") Integer employeeId,
			@Param("date") java.sql.Date date);

	@Query("SELECT s FROM Submission s WHERE s.employee.employeeId = :employeeId")
	List<Submission> findByEmployeeId(@Param("employeeId") Integer employeeId);

	Submission findFirstByEmployee_EmployeeIdAndQuestionBank_QuestionIdOrderByCreatedDtDesc(Integer employeeId,
			Integer questionId);

	@Query(value = "SELECT s.employee_id as employeeId, e.first_name || ' ' || e.last_name as employeeName, CAST(s.created_dt AS DATE) as date, "
			+
			"SUM(s.marks_awarded) as totalScore, COUNT(DISTINCT s.question_id) as answeredQuestions, " +
			"(SELECT COUNT(*) FROM compliance.role_question_mapping rqm WHERE rqm.role_id = e.role_id) as totalQuestions "
			+
			"FROM compliance.compliance_submission s " +
			"JOIN base.employee e ON s.employee_id = e.employee_id " +
			"WHERE e.is_active = true " +
			"GROUP BY s.employee_id, e.first_name, e.last_name, CAST(s.created_dt AS DATE), e.role_id ", nativeQuery = true)
	List<Object[]> getEmployeeProgressNative();

	@Query(value = "SELECT s.employee_id as employeeId, e.first_name || ' ' || e.last_name as employeeName, CAST(s.created_dt AS DATE) as date, "
			+
			"SUM(s.marks_awarded) as totalScore, COUNT(DISTINCT s.question_id) as answeredQuestions, " +
			"(SELECT COUNT(*) FROM compliance.role_question_mapping rqm WHERE rqm.role_id = e.role_id) as totalQuestions "
			+
			"FROM compliance.compliance_submission s " +
			"JOIN base.employee e ON s.employee_id = e.employee_id " +
			"WHERE e.is_active = true AND CAST(s.created_dt AS DATE) = :date " +
			"GROUP BY s.employee_id, e.first_name, e.last_name, CAST(s.created_dt AS DATE), e.role_id ", nativeQuery = true)
	List<Object[]> getEmployeeProgressByDateNative(@Param("date") java.sql.Date date);

	@Query(value = "SELECT s.employee_id as employeeId, " +
			"e.first_name || ' ' || e.last_name as employeeName, " +
			"CAST(s.question_date AS DATE) as questionDate, " +
			"MAX(s.created_dt) as createdDt, " +
			"SUM(s.marks_awarded) as totalScore, " +
			"COUNT(DISTINCT s.question_id) as answeredQuestions, " +
			"(SELECT COUNT(*) FROM compliance.role_question_mapping rqm WHERE rqm.role_id = e.role_id) as totalQuestions " +
			"FROM compliance.compliance_submission s " +
			"JOIN base.employee e ON s.employee_id = e.employee_id " +
			"WHERE e.is_active = true AND s.employee_id = :employeeId " +
			"AND CAST(s.question_date AS DATE) >= :startDate " +
			"AND CAST(s.question_date AS DATE) < :endDate " +
			"GROUP BY s.employee_id, e.first_name, e.last_name, CAST(s.question_date AS DATE), e.role_id " +
			"ORDER BY CAST(s.question_date AS DATE) ASC", nativeQuery = true)
	List<Object[]> getEmployeeDailyProgressNative(@Param("employeeId") Integer employeeId,
			@Param("startDate") java.sql.Date startDate,
			@Param("endDate") java.sql.Date endDate);

	@Query(value = "SELECT s.employee_id as employeeId, " +
			"e.first_name || ' ' || e.last_name as employeeName, " +
			"SUM(s.marks_awarded) as totalScore, " +
			"e.employee_code as employeeCode " +
			"FROM compliance.compliance_submission s " +
			"JOIN base.employee e ON s.employee_id = e.employee_id " +
			"WHERE e.is_active = true AND s.is_active = true " +
			"AND CAST(s.question_date AS DATE) >= :startDate " +
			"AND CAST(s.question_date AS DATE) < :endDate " +
			"AND (:orgId IS NULL OR e.org_id = :orgId) " +
			"GROUP BY s.employee_id, e.first_name, e.last_name, e.employee_code " +
			"ORDER BY totalScore DESC", nativeQuery = true)
	List<Object[]> getTopEmployeesByQuestionDateRange(
			@Param("startDate") java.sql.Date startDate,
			@Param("endDate") java.sql.Date endDate,
			@Param("orgId") Integer orgId);

	@Query(value = "SELECT s.employee_id as employeeId, " +
			"e.first_name || ' ' || e.last_name as employeeName, " +
			"SUM(s.marks_awarded) as totalScore, " +
			"e.employee_code as employeeCode " +
			"FROM compliance.compliance_submission s " +
			"JOIN base.employee e ON s.employee_id = e.employee_id " +
			"WHERE e.is_active = true AND s.is_active = true " +
			"AND CAST(s.created_dt AS DATE) >= :startDate " +
			"AND CAST(s.created_dt AS DATE) < :endDate " +
			"AND (:orgId IS NULL OR e.org_id = :orgId) " +
			"GROUP BY s.employee_id, e.first_name, e.last_name, e.employee_code " +
			"ORDER BY totalScore DESC", nativeQuery = true)
	List<Object[]> getTopEmployeesByCreatedDateRange(
			@Param("startDate") java.sql.Date startDate,
			@Param("endDate") java.sql.Date endDate,
			@Param("orgId") Integer orgId);

	/**
	 * Sum weightage for submissions within a date range for specific employees
	 */
	@Query(value = "SELECT COALESCE(SUM(q.weightage), 0) " +
			"FROM compliance.compliance_submission s " +
			"JOIN compliance.question_bank q ON s.question_id = q.question_id " +
			"WHERE s.employee_id IN (:employeeIds) " +
			"AND s.question_date >= :startDate " +
			"AND s.question_date < :endDate " +
			"AND s.is_active = true", nativeQuery = true)
	Double sumWeightageByEmployeeIdsAndQuestionDateRange(
			@Param("employeeIds") List<Integer> employeeIds,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Query(value = "SELECT COALESCE(SUM(q.weightage), 0) " +
			"FROM compliance.compliance_submission s " +
			"JOIN compliance.question_bank q ON s.question_id = q.question_id " +
			"WHERE s.employee_id IN (:employeeIds) " +
			"AND CAST(s.created_dt AS DATE) >= :startDate " +
			"AND CAST(s.created_dt AS DATE) < :endDate " +
			"AND s.is_active = true", nativeQuery = true)
	Double sumWeightageByEmployeeIdsAndCreatedDateRange(
			@Param("employeeIds") List<Integer> employeeIds,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	Optional<Submission> findByEmployee_EmployeeIdAndQuestionBank_QuestionIdAndQuestionDate(
			Integer employeeId,
			Integer questionId,
			LocalDate questionDate);

	/**
	 * Get distinct days when employee has active submissions within date range
	 */
	@Query(value = "SELECT COUNT(DISTINCT s.question_date) FROM compliance.compliance_submission s " +
			"WHERE s.employee_id = :employeeId AND s.question_date >= :startDate AND s.question_date < :endDate",
			nativeQuery = true)
	long countEmployeeWorkingDays(@Param("employeeId") Integer employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * Find submissions by employee and question date
	 */
	@Query("SELECT s FROM Submission s WHERE s.employee.employeeId = :employeeId AND s.questionDate = :questionDate")
	List<Submission> findByEmployeeIdAndQuestionDate(
			@Param("employeeId") Integer employeeId,
			@Param("questionDate") LocalDate questionDate);

	List<Submission> findByEmployee_EmployeeIdAndQuestionDateGreaterThanEqualAndQuestionDateLessThanAndIsActiveTrue(
			Integer employeeId,
			LocalDate startDate,
			LocalDate endDate);

	@Query("SELECT s FROM Submission s WHERE s.employee.employeeId = :employeeId " +
			"AND s.isActive = true AND s.createdDt >= :startDateTime AND s.createdDt < :endDateTime")
	List<Submission> findActiveByEmployeeAndCreatedDtRange(
			@Param("employeeId") Integer employeeId,
			@Param("startDateTime") java.time.OffsetDateTime startDateTime,
			@Param("endDateTime") java.time.OffsetDateTime endDateTime);

	@Query("SELECT s FROM Submission s WHERE s.employee.employeeId = :employeeId " +
			"AND s.isActive = true AND s.submittedAt >= :startDateTime AND s.submittedAt < :endDateTime")
	List<Submission> findActiveByEmployeeAndSubmittedAtRange(
			@Param("employeeId") Integer employeeId,
			@Param("startDateTime") java.time.OffsetDateTime startDateTime,
			@Param("endDateTime") java.time.OffsetDateTime endDateTime);

	/**
	 * Check if employee has any active submission on a specific date
	 */
	@Query(value = "SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM compliance.compliance_submission s " +
			"WHERE s.employee_id = :employeeId AND s.question_date = :date",
			nativeQuery = true)
	boolean hasSubmissionOnDate(@Param("employeeId") Integer employeeId,
			@Param("date") LocalDate date);

}
