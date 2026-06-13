package com.spearhead.ufc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spearhead.ufc.model.EmployeeLocationAccess;

public interface EmployeeLocationAccessRepository extends JpaRepository<EmployeeLocationAccess, Integer> {
    List<EmployeeLocationAccess> findByEmployeeEmployeeId(Integer employeeId);
    List<EmployeeLocationAccess> findByEmployeeEmployeeIdAndIsActiveTrue(Integer employeeId);
    void deleteByEmployeeEmployeeId(Integer employeeId);
}