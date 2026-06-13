package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionListDTO {

    private Integer questionId;
    private Integer orgId;
    private String orgName;
    private String questionText;
    private String questionType;
    private Double weightage;
    private Boolean isActive;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer createdBy;
    private OffsetDateTime createdDt;
    private Integer updatedBy;
    private OffsetDateTime updatedDt;
    private String createdEmployeeName;
    private String updatedEmployeeName;
    
    // Role names as array - supports multiple roles per question
    private List<String> roleNames;
    
    // Role IDs as array - for reference
    private List<Integer> roleIds;
}
