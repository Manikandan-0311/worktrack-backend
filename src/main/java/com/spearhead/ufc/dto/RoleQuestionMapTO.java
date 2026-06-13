package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleQuestionMapTO {
    private Integer roleQuestionMapId;
    private Integer orgId;
    private Integer roleId;
    private Integer questionId;
    private Boolean isActive;
    private Integer createdBy;
    private java.time.OffsetDateTime createdDt;
    private Integer updatedBy;
    private java.time.OffsetDateTime updatedDt;
}
