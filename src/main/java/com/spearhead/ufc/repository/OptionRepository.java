package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionRepository extends JpaRepository<Option, Integer> {
	List<Option> findByQuestion_QuestionId(Integer questionId);
	List<Option> findByQuestion_QuestionIdAndIsActiveTrue(Integer questionId);
}
