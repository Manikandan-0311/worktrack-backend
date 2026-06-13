package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankDTO {

    private Integer questionId;

    // For saving
    private Integer orgId;
    private Integer locationId;
    private Integer roleId;

    // For listing
    private String locationName;

    private String questionText;
    private String questionType;
    private Double weightage;
    private Boolean isActive;

    // Constructor for your JPQL query (list view)
    public QuestionBankDTO(Integer questionId, String questionText, String questionType,
                           Double weightage, Boolean isActive, Integer locationId,
                           String locationName) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.weightage = weightage;
        this.isActive = isActive;
        this.locationId = locationId;
        this.locationName = locationName;
    }
}
