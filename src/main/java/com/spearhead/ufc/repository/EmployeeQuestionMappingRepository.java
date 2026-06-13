package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.EmployeeQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeQuestionMappingRepository extends JpaRepository<EmployeeQuestionMapping, Integer> {
    Optional<EmployeeQuestionMapping> findFirstByEmployee_EmployeeIdAndQuestion_QuestionId(Integer employeeId,
            Integer questionId);
    java.util.List<EmployeeQuestionMapping> findByEmployee_EmployeeIdAndIsActiveTrue(Integer employeeId);
    List<EmployeeQuestionMapping> findByEmployee_EmployeeIdAndQuestion_QuestionIdIn(Integer employeeId,
            List<Integer> questionIds);
}
