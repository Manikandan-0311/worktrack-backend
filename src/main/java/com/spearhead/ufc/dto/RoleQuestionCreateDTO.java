package com.spearhead.ufc.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleQuestionCreateDTO {
    // Mapping fields
    private Integer orgId;      // required for SUPER_ADMIN; derived from user otherwise
    private Integer roleId;     // required
    private Integer questionId; // optional: if null/0, create a new question first
    private Boolean isActive;   // optional

    // Question creation fields (used when questionId is null/0)
    private Integer locationId;   // required when creating a question
    private String questionText;  // required when creating a question
    private String questionType;  // required when creating a question (e.g., radio, multiselect, text)
    private Double weightage;     // required when creating a question
    private LocalDate fromDate;   // optional
    private LocalDate toDate;     // optional

    // Options to save for radio/multiselect
    private String[] options;     // optional
}
