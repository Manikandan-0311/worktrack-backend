package com.spearhead.ufc.service;

import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.dto.RoleBranchAccessDTO;
import com.spearhead.ufc.repository.RoleRepository;
import com.spearhead.ufc.repository.RoleQuestionMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleQuestionMappingRepository roleQuestionMappingRepository;

    @Autowired
    private EmployeeService employeeService;

    public List<Role> getAllRoles() {
        log.info("Fetching all roles");
        List<Role> roles = roleRepository.findByIsActiveTrue();
        // Populate audit employee names for each role
        for (Role role : roles) {
            populateAuditEmployeeNames(role);
        }
        return roles;
    }

    public List<Role> getAllRolesIncludingInactive() {
        log.info("Fetching all roles including inactive");
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            populateAuditEmployeeNames(role);
        }
        return roles;
    }

    public List<Role> getRolesByOrgId(Integer orgId) {
        log.info("Fetching roles for orgId: {}", orgId);
        List<Role> filteredRoles = roleRepository.findByOrgOrgIdAndIsActiveTrue(orgId);
        // Populate audit employee names for each role
        for (Role role : filteredRoles) {
            populateAuditEmployeeNames(role);
        }
        return filteredRoles;
    }

    public List<Role> getRolesByOrgIdIncludingInactive(Integer orgId) {
        log.info("Fetching roles (including inactive) for orgId: {}", orgId);
        List<Role> filteredRoles = roleRepository.findByOrgOrgId(orgId);
        for (Role role : filteredRoles) {
            populateAuditEmployeeNames(role);
        }
        return filteredRoles;
    }

    public Role addRole(Role role, Employee user) {
        log.info("Adding new role: {} for org: {}", role.getRoleName(),
                (role.getOrg() != null ? role.getOrg().getOrgId()
                        : (user != null && user.getOrg() != null ? user.getOrg().getOrgId() : null)));
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        role.setCreatedBy(user.getEmployeeId());
        role.setCreatedDt(OffsetDateTime.now());
        role.setIsActive(true);
        // Set Org: prefer role payload, else user's org
        if (role.getOrg() == null && user.getOrg() != null) {
            role.setOrg(user.getOrg());
        }
        if (role.getOrg() == null) {
            throw new IllegalArgumentException("orgId is required to create a role");
        }

        Integer orgId = role.getOrg().getOrgId();
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required to create a role");
        }

        // Duplicate validation scoped to org (role name must be unique within organization)
        if (role.getRoleName() != null
                && roleRepository.existsByOrgOrgIdAndRoleNameIgnoreCase(orgId, role.getRoleName())) {
            throw new IllegalArgumentException("Role name '" + role.getRoleName() + "' already exists in this organization. Please use a different role name.");
        }
        return roleRepository.save(role);
    }

    public Role updateRole(Role role, Employee user) {
        log.info("Updating role: {} (ID: {})", role.getRoleName(), role.getRoleId());
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        if (role.getRoleId() == null || role.getRoleId() <= 0) {
            throw new IllegalArgumentException("Role ID is required for update");
        }

        Role existing = roleRepository.findById(role.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        role.setCreatedBy(existing.getCreatedBy());
        role.setCreatedDt(existing.getCreatedDt());
        role.setUpdatedBy(user.getEmployeeId());
        role.setUpdatedDt(OffsetDateTime.now());

        Integer orgId;
        if (role.getOrg() != null && role.getOrg().getOrgId() != null) {
            orgId = role.getOrg().getOrgId();
        } else if (existing.getOrg() != null && existing.getOrg().getOrgId() != null) {
            orgId = existing.getOrg().getOrgId();
            role.setOrg(existing.getOrg());
        } else if (user.getOrg() != null && user.getOrg().getOrgId() != null) {
            orgId = user.getOrg().getOrgId();
            role.setOrg(user.getOrg());
        } else {
            throw new IllegalArgumentException("orgId is required to update a role");
        }

        // Duplicate validation scoped to org (role name must be unique within organization, excluding current role)
        if (role.getRoleName() != null && roleRepository.existsByOrgOrgIdAndRoleNameIgnoreCaseAndRoleIdNot(orgId,
                role.getRoleName(), role.getRoleId())) {
            throw new IllegalArgumentException("Role name '" + role.getRoleName() + "' already exists in this organization. Please use a different role name.");
        }
        return roleRepository.save(role);
    }

    /**
     * Get active questions mapped to a specific role
     */
    public List<QuestionBank> getQuestionsByRoleId(Integer roleId) {
        log.info("Fetching active questions for roleId: {}", roleId);
        if (roleId == null || roleId <= 0) {
            throw new IllegalArgumentException("Role ID must be a valid positive integer");
        }
        return roleQuestionMappingRepository.findByRoleIdAndIsActiveTrue(roleId)
                .stream()
                .map(rqm -> rqm.getQuestion())
                .collect(Collectors.toList());
    }

    public List<RoleBranchAccessDTO> getRoleListByOrgIdAndEmployeeId(Integer orgId, Integer employeeId) {
        log.info("Fetching role list from role_question_mapping for orgId: {}, employeeId: {}", orgId, employeeId);
        if (orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("Organization ID must be a valid positive integer");
        }
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be a valid positive integer");
        }

        List<Object[]> rows = roleQuestionMappingRepository
                .findRoleListByOrgIdAndEmployeeIdWithBranchAccess(orgId, employeeId);

        List<RoleBranchAccessDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            RoleBranchAccessDTO dto = new RoleBranchAccessDTO();
            dto.setRoleId(row[0] != null ? ((Number) row[0]).intValue() : null);
            dto.setRoleName(row[1] != null ? row[1].toString() : null);
            dto.setOrgId(row[2] != null ? ((Number) row[2]).intValue() : null);
            dto.setBranchId(row[3] != null ? ((Number) row[3]).intValue() : null);
            result.add(dto);
        }
        return result;
    }

    private void populateAuditEmployeeNames(Role role) {
        if (role == null) {
            return;
        }
        if (role.getUpdatedBy() != null) {
            String employeeName = employeeService.getEmployeeFullName(role.getUpdatedBy());
            role.setUpdatedEmployeeName(employeeName);
        }
        if (role.getCreatedBy() != null) {
            String employeeName = employeeService.getEmployeeFullName(role.getCreatedBy());
            role.setCreatedEmployeeName(employeeName);
        }
    }
}
