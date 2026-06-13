package com.spearhead.ufc.repository;

import com.spearhead.ufc.dto.QuestionBankDTO;
//import com.spearhead.ufc.dto.QuestionBankDTO;
import com.spearhead.ufc.model.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

	@Query("SELECT q FROM QuestionBank q WHERE q.org.orgId = :orgId AND q.isActive = true")
	List<QuestionBank> findActiveQuestions1(@Param("orgId") int orgId);

	@Query("SELECT new com.spearhead.ufc.dto.QuestionBankDTO(" +
			"q.questionId, q.questionText, q.questionType, q.weightage, q.isActive, " +
			"null, null) " +
			"FROM QuestionBank q " +
			"WHERE q.org.orgId = :orgId " +
			"ORDER BY q.questionId DESC")
	List<QuestionBankDTO> findQuestionsByFilters(@Param("orgId") Integer orgId);

	@Query("SELECT q FROM QuestionBank q")
	List<QuestionBank> findAllQuestionsTO();

	@Query("SELECT q FROM QuestionBank q WHERE "
			+ "(:orgId IS NULL OR q.org.orgId = :orgId) AND "
			+ "(:roleId IS NULL OR q.role.roleId = :roleId)")
	List<QuestionBank> findQuestionsByFiltersTO(
			@Param("orgId") Integer orgId,
			@Param("locationId") Integer locationId,
			@Param("roleId") Integer roleId);

	@Query("SELECT q FROM QuestionBank q WHERE q.isActive = true")
	List<QuestionBank> findByIsActiveTrue();
}
