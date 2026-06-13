package com.spearhead.ufc.dto;

import lombok.Data;

@Data
public class QuestionWithEmployeeMappingDTO {
    private Integer questionId;
    private String questionText;
    private String questionType;
    private Double weightage;
    private Boolean questionIsActive;

    private Boolean alreadyMapped; // true if employee has a mapping
    private Integer employeeMappingId; // id from employee_question_mapping
    private Boolean employeeMappingIsActive; // mapping is_active
}
