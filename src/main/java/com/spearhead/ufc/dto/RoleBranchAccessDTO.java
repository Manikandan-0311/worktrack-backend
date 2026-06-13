package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleBranchAccessDTO {
    private Integer roleId;
    private String roleName;
    private Integer orgId;
    private Integer branchId;
}
