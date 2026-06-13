package com.spearhead.ufc.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuestionRequest {
    private Integer questionId;
    private Integer orgId;
    private Integer locationId;
    private String questionText;
    private String questionType;
    private Double weightage;
    private Boolean isActive;
    private LocalDate fromDate;
    private LocalDate toDate;
    // roleIds as array of { roleId, isActive }
    private List<RoleAssignmentDTO> roleIds;
}
