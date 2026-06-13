package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionAnswerDTO {
    private Integer questionId;
    private String answer;   
    private String remarks; 
    private Double weightage; 
    private LocalDate questionDate;
    private String questionText;
    private Boolean isHoliday;
}
