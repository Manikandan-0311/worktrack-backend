package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgBasicDTO {
    private Integer orgId;
    private String orgName;
}