package com.spearhead.ufc.dto;

import lombok.Data;

import java.util.List;

@Data
public class MappingsRequest {
    private List<EmployeeQuestionMappingDTO> mappings;
}
