package com.spearhead.ufc.dto;

import java.util.List;

import com.spearhead.ufc.model.Employee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeGetByIdResponseDTO {
    private Integer employeeId;
    private String firstName;
    private List<EmployeeLocationAccessItemDTO> employeeLocationAccess;
    private OrgBasicDTO organization;
    private BranchBasicDTO branch;
    private Employee employee;
}
