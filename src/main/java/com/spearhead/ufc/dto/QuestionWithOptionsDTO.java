package com.spearhead.ufc.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuestionWithOptionsDTO {
    private Integer questionId;
    private String questionText;
    private String questionType;
    private Double weightage;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<OptionDTO> options;
    private String answer;
    private Boolean submittedToday; 
    private Boolean canSubmitNow; 
    private Boolean showSubmitButton; 
    private Double cost;
    private Boolean reasonFlag;
    private String validAnswer;

    public QuestionWithOptionsDTO(Integer questionId, String questionText, String questionType, Double weightage,
            LocalDate fromDate, LocalDate toDate, List<OptionDTO> options, String answer,
            Boolean submittedToday, Boolean canSubmitNow, Boolean showSubmitButton, Double cost) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.weightage = weightage;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.options = options;
        this.answer = answer;
        this.submittedToday = submittedToday;
        this.canSubmitNow = canSubmitNow;
        this.showSubmitButton = showSubmitButton;
        this.cost = cost;
    }

    public QuestionWithOptionsDTO(Integer questionId, String questionText, List<OptionDTO> options,String questionType,Double weightage) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.options = options;
        this.questionType = questionType;
        this.weightage = weightage;
    }
}
