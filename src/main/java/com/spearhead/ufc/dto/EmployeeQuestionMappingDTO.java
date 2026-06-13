package com.spearhead.ufc.dto;

import lombok.Data;

@Data
public class EmployeeQuestionMappingDTO {
    private EmployeeRef employee;
    private QuestionRef question;
    private Boolean isActive;

    @Data
    public static class EmployeeRef {
        private Integer employeeId;
    }

    @Data
    public static class QuestionRef {
        private Integer questionId;
    }
}
