package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.AnswerSubmissionTimeExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerSubmissionTimeExtensionRepository extends JpaRepository<AnswerSubmissionTimeExtension, Integer> {
	java.util.List<AnswerSubmissionTimeExtension> findByEmployee_EmployeeIdAndQuestionDateAndIsActiveTrue(Integer employeeId, java.time.LocalDate questionDate);
	java.util.List<AnswerSubmissionTimeExtension> findByEmployee_EmployeeIdAndIsActiveTrue(Integer employeeId);
	java.util.List<AnswerSubmissionTimeExtension> findByEmployee_EmployeeId(Integer employeeId);
}