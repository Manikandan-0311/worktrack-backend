package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchBasicDTO {
    private Integer branchId;
    private String branchName;
}