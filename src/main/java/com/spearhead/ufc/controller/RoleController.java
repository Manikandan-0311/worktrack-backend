package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.EmployeeQuestionMapping;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.service.RoleService;
import com.spearhead.ufc.utils.AuthUtil;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.EmployeeQuestionMappingRepository;
import com.spearhead.ufc.repository.RoleQuestionMappingRepository;
import com.spearhead.ufc.dto.OrgLiteDTO;
import com.spearhead.ufc.dto.RoleBranchAccessDTO;
import com.spearhead.ufc.dto.RoleDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roleController")
@CrossOrigin(origins = "*")
public class RoleController {
    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private EmployeeQuestionMappingRepository employeeQuestionMappingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleQuestionMappingRepository roleQuestionMappingRepository;

    /**
     * Get roles list based on user role
     * - SUPER_ADMIN: All roles
     * - Other roles: Only roles of user's organization
     */
    @GetMapping("/rolesList")
    public ResponseEntity<?> getRolesList(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Request: getRolesList received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in getRolesList");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in getRolesList");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }
            log.debug("Employee {} authenticated for getRolesList", employee.getEmployeeId());
            log.debug("Employee role: {}, Organization ID: {}",
                    employee.getRoleId() != null ? employee.getRoleId().getRoleName() : "N/A",
                    employee.getOrg() != null ? employee.getOrg().getOrgId() : "N/A");

            boolean isSuperAdmin = employee.getRoleId() != null &&
                    "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

            List<Role> roles;
            if (isSuperAdmin) {
                log.info("SUPER_ADMIN employee {} requesting all roles", employee.getEmployeeId());
                roles = roleService.getAllRoles();
                // Filter roles to include only those from active organizations
                roles = roles.stream()
                        .filter(role -> role.getOrg() != null && role.getOrg().getIsActive() != null
                                && role.getOrg().getIsActive())
                        .toList();
                log.info("SUPER_ADMIN filtered roles from active organizations only: {}", roles.size());
            } else {
                if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
                    log.warn("Non-admin employee {} has no organization assigned", employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
                }

                // Check if user's organization is active
                if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
                    log.warn("Non-admin employee {} organization is inactive: {}",
                            employee.getEmployeeId(), employee.getOrg().getOrgId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null,
                                    "Your organization is inactive. You cannot access role data."));
                }

                log.info("Regular employee {} requesting roles for active organization: {}",
                        employee.getEmployeeId(), employee.getOrg().getOrgId());
                roles = roleService.getRolesByOrgId(employee.getOrg().getOrgId());
            }

            log.info("Successfully retrieved {} roles from active organizations", roles.size());
            return ResponseEntity.ok(JsonResponse.of(true, roles, "Role list loaded successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error in getRolesList: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesList: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load role list: " + e.getMessage()));
        }
    }

    /**
     * Get roles list including active and inactive, latest updated first
     * - SUPER_ADMIN: All roles
     * - Other roles: Only roles of user's organization
     */
    @GetMapping("/rolesListWithInactive")
    public ResponseEntity<?> getRolesListWithInactive(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Request: getRolesListWithInactive received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            boolean isSuperAdmin = employee.getRoleId() != null &&
                    "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

            List<Role> roles;
            if (isSuperAdmin) {
                roles = roleService.getAllRolesIncludingInactive();
                roles = roles.stream()
                        .filter(role -> role.getOrg() != null && role.getOrg().getIsActive() != null
                                && role.getOrg().getIsActive())
                        .toList();
            } else {
                if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
                }
                if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null,
                                    "Your organization is inactive. You cannot access role data."));
                }
                roles = roleService.getRolesByOrgIdIncludingInactive(employee.getOrg().getOrgId());
            }

            roles = roles.stream()
                    .sorted((a, b) -> {
                        java.time.OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt() : a.getCreatedDt();
                        java.time.OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt() : b.getCreatedDt();
                        if (aTime == null && bTime == null) {
                            return 0;
                        }
                        if (aTime == null) {
                            return 1;
                        }
                        if (bTime == null) {
                            return -1;
                        }
                        return bTime.compareTo(aTime);
                    })
                    .toList();

            return ResponseEntity.ok(JsonResponse.of(true, roles, "Role list loaded successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesListWithInactive: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load role list: " + e.getMessage()));
        }
    }

    /**
     * Get roles by Organization ID
     * Takes only orgId as parameter and returns roles for that organization
     * 
     * Example: GET /roleController/rolesByOrgId?orgId=1
     */
    @GetMapping("/rolesByOrgId")
    public ResponseEntity<?> getRolesByOrgId(@RequestParam(value = "orgId", required = true) Integer orgId) {
        log.info("Request: getRolesByOrgId received with orgId: {}", orgId);
        try {
            if (orgId == null || orgId <= 0) {
                log.warn("Invalid orgId provided: {}", orgId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Organization ID must be a valid positive integer."));
            }
            List<Role> roles = roleService.getRolesByOrgId(orgId);
            log.info("Successfully retrieved {} roles for organization {}", roles != null ? roles.size() : 0, orgId);
            return ResponseEntity.ok(JsonResponse.of(true, roles, "Role list loaded successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error in getRolesByOrgId: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesByOrgId: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load role list: " + e.getMessage()));
        }
    }

    /**
     * Get role list from role_question_mapping for org + employee branch access
     * Returns: roleId, roleName, orgId, branchId
     *
     * Example: GET /roleController/rolesByOrgAndEmployee?orgId=11&employeeId=58
     */
    @GetMapping("/rolesByOrgAndEmployee")
    public ResponseEntity<?> getRolesByOrgAndEmployee(
            @RequestParam(value = "orgId", required = true) Integer orgId,
            @RequestParam(value = "employeeId", required = true) Integer employeeId) {
        log.info("Request: getRolesByOrgAndEmployee received with orgId: {}, employeeId: {}", orgId, employeeId);
        try {
            if (orgId == null || orgId <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Organization ID must be a valid positive integer."));
            }
            if (employeeId == null || employeeId <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Employee ID must be a valid positive integer."));
            }

            List<RoleBranchAccessDTO> roles = roleService.getRoleListByOrgIdAndEmployeeId(orgId, employeeId);
            return ResponseEntity.ok(JsonResponse.of(true, roles, "Role list loaded successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Validation error in getRolesByOrgAndEmployee: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesByOrgAndEmployee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load role list: " + e.getMessage()));
        }
    }

    /**
     * Add or update a role
     * Sets createdBy/createdDt for new roles
     * Sets updatedBy/updatedDt for updates
     */
    @PostMapping("/addOrUpdateRole")
    public ResponseEntity<?> addOrUpdateRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Role role) {
        log.info("Request: addOrUpdateRole received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in addOrUpdateRole");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in addOrUpdateRole");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }
            log.debug("Employee {} authenticated for addOrUpdateRole", employee.getEmployeeId());

            boolean isSuperAdmin = employee.getRoleId() != null &&
                    "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

            if (!isSuperAdmin) {
                Integer empOrgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
                Integer roleOrgId = (role.getOrg() != null) ? role.getOrg().getOrgId() : null;
                if (empOrgId == null) {
                    log.warn("Non-admin employee {} has no organization assigned", employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
                }
                if (roleOrgId != null && !empOrgId.equals(roleOrgId)) {
                    log.warn("Employee {} attempted to modify role in different organization",
                            employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null,
                                    "You don't have permission to modify roles in this organization."));
                }
            } else {
                // SUPER_ADMIN must specify target org in request
                if (role.getOrg() == null || role.getOrg().getOrgId() == null) {
                    log.warn("SUPER_ADMIN {} attempted role change without target org", employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(JsonResponse.of(false, null,
                                    "orgId is required for SUPER_ADMIN role creation/update."));
                }
            }

            Role result;
            if (role.getRoleId() == null || role.getRoleId() == 0) {
                log.info("Creating new role: {} in organization: {}",
                        role.getRoleName(), employee.getOrg() != null ? employee.getOrg().getOrgId() : null);
                result = roleService.addRole(role, employee);
                log.info("Role created successfully with ID: {}", result.getRoleId());
                return ResponseEntity.ok(JsonResponse.of(true, result, "Role added successfully"));
            } else {
                log.info("Updating role with ID: {}", role.getRoleId());
                result = roleService.updateRole(role, employee);
                log.info("Role updated successfully with ID: {}", result.getRoleId());
                return ResponseEntity.ok(JsonResponse.of(true, result, "Role updated successfully"));
            }

        } catch (IllegalArgumentException e) {
            log.error("Validation error in addOrUpdateRole: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Validation error";
            String lc = msg.toLowerCase();
            // If the message indicates a duplicate, return 409 Conflict for better UX
            if (lc.contains("role name") && lc.contains("already exists") ||
                    lc.contains("role code") && lc.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(JsonResponse.of(false, null, msg));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, msg));
        } catch (DataIntegrityViolationException e) {
            String message = "Data integrity error";
            Throwable cause = e.getCause();
            // Inspect deepest cause message for column hints like (role_name) or
            // (role_code)
            String deepMsg = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                    ? e.getMostSpecificCause().getMessage().toLowerCase()
                    : (e.getMessage() != null ? e.getMessage().toLowerCase() : "");
            if (deepMsg.contains("role_name")) {
                message = "Role name already exists for this organization";
            } else if (deepMsg.contains("role_code")) {
                message = "Role code already exists for this organization";
            } else if (cause instanceof ConstraintViolationException cve) {
                String constraintMsg = cve.getMessage() != null ? cve.getMessage().toLowerCase() : "";
                if (constraintMsg.contains("role_name"))
                    message = "Role name already exists for this organization";
                if (constraintMsg.contains("role_code"))
                    message = "Role code already exists for this organization";
            }
            log.warn("Constraint violation in addOrUpdateRole: {}", message);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(JsonResponse.of(false, null, message));
        } catch (Exception e) {
            log.error("Unexpected error in addOrUpdateRole: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to add/update role: " + e.getMessage()));
        }
    }

    /**
     * Get roles for a specific organization
     * - SUPER_ADMIN: must supply orgId in query
     * - Other roles: orgId is ignored and derived from the employee token
     *
     * Example: GET /roleController/byOrganization?orgId=2
     */
    @GetMapping("/byOrganization")
    public ResponseEntity<?> getRolesByOrganization(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "orgId", required = false) Integer orgId) {
        log.info("Request: getRolesByOrganization received for orgId: {}", orgId);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in getRolesByOrganization");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in getRolesByOrganization");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            boolean isSuperAdmin = employee.getRoleId() != null &&
                    "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

            Integer targetOrgId;
            if (isSuperAdmin) {
                if (orgId == null) {
                    log.warn("SUPER_ADMIN {} called byOrganization without orgId", employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(JsonResponse.of(false, null, "orgId is required for SUPER_ADMIN"));
                }
                targetOrgId = orgId;
            } else {
                targetOrgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
                if (targetOrgId == null) {
                    log.warn("Non-admin employee {} has no organization assigned", employee.getEmployeeId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
                }
            }

            List<Role> roles = roleService.getRolesByOrgId(targetOrgId);
            log.info("Fetched {} roles for orgId {}", roles != null ? roles.size() : 0, targetOrgId);

            java.util.List<RoleDTO> payload = roles.stream().map(r -> {
                OrgLiteDTO orgLite = null;
                if (r.getOrg() != null) {
                    orgLite = new OrgLiteDTO();
                    orgLite.setOrgId(r.getOrg().getOrgId());
                    orgLite.setOrgName(r.getOrg().getOrgName());
                    orgLite.setBranchName(r.getLocation().getLocationName());
                }
                RoleDTO dto = new RoleDTO();
                dto.setRoleId(r.getRoleId());
                dto.setRoleName(r.getRoleName());
                dto.setDescription(r.getDescription());
                dto.setIsActive(r.getIsActive());
                dto.setOrg(orgLite);
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(JsonResponse.of(true, payload, "Roles fetched successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Validation error in getRolesByOrganization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesByOrganization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load roles: " + e.getMessage()));
        }
    }

    /**
     * Get active questions mapped to a role
     * Example: GET /roleController/questionsByRole?roleId=5
     */
    @GetMapping("/questionsByRole")
    public ResponseEntity<?> getQuestionsByRole(
            @RequestParam(value = "roleId", required = true) Integer roleId,
            @RequestParam(value = "employeeId", required = false) Integer employeeId) {
        log.info("Request: getQuestionsByRole received with roleId: {}", roleId);
        try {
            if (roleId == null || roleId <= 0) {
                log.warn("Invalid roleId provided: {}", roleId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Role ID must be a valid positive integer."));
            }

            List<com.spearhead.ufc.model.QuestionBank> questions = roleService.getQuestionsByRoleId(roleId);

            // If employeeId provided, enrich each question with employee mapping status
            if (employeeId != null && employeeId > 0) {
                Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found for employeeId: " + employeeId));

                Integer defaultRoleId = (employee.getRoleId() != null) ? employee.getRoleId().getRoleId() : null;
                boolean isUsingDefaultRole = defaultRoleId != null && defaultRoleId.equals(roleId);

                java.util.List<Integer> questionIds = questions.stream()
                    .map(com.spearhead.ufc.model.QuestionBank::getQuestionId)
                    .filter(java.util.Objects::nonNull)
                    .toList();

                Map<Integer, EmployeeQuestionMapping> mappingByQuestionId = questionIds.isEmpty()
                    ? Collections.emptyMap()
                    : employeeQuestionMappingRepository
                        .findByEmployee_EmployeeIdAndQuestion_QuestionIdIn(employeeId, questionIds)
                        .stream()
                        .filter(m -> m.getQuestion() != null && m.getQuestion().getQuestionId() != null)
                        .collect(Collectors.toMap(m -> m.getQuestion().getQuestionId(), Function.identity(),
                            (existing, replacement) -> replacement));

                java.util.List<com.spearhead.ufc.dto.QuestionWithEmployeeMappingDTO> payload = new java.util.ArrayList<>();
                for (com.spearhead.ufc.model.QuestionBank q : questions) {
                    com.spearhead.ufc.dto.QuestionWithEmployeeMappingDTO dto = new com.spearhead.ufc.dto.QuestionWithEmployeeMappingDTO();
                    dto.setQuestionId(q.getQuestionId());
                    dto.setQuestionText(q.getQuestionText());
                    dto.setQuestionType(q.getQuestionType());
                    dto.setWeightage(q.getWeightage());
                    dto.setQuestionIsActive(q.getIsActive());

                    EmployeeQuestionMapping m = mappingByQuestionId.get(q.getQuestionId());
                    if (m != null) {
                        dto.setAlreadyMapped(true);
                        dto.setEmployeeMappingId(m.getEmployeeQuestionMappingId());
                    } else {
                        dto.setAlreadyMapped(false);
                        dto.setEmployeeMappingId(null);
                    }

                    Boolean employeeMappingIsActive;
                    if (isUsingDefaultRole) {
                        employeeMappingIsActive = (m == null) || !Boolean.FALSE.equals(m.getIsActive());
                    } else {
                        employeeMappingIsActive = (m != null) && Boolean.TRUE.equals(m.getIsActive());
                    }
                    dto.setEmployeeMappingIsActive(employeeMappingIsActive);

                    payload.add(dto);
                }
                log.info("Successfully retrieved {} questions for role {} (enriched with employee mappings)",
                        payload.size(), roleId);
                return ResponseEntity.ok(JsonResponse.of(true, payload, "Questions loaded successfully"));
            }

            log.info("Successfully retrieved {} questions for role {}", questions != null ? questions.size() : 0,
                    roleId);
            return ResponseEntity.ok(JsonResponse.of(true, questions, "Questions loaded successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error in getQuestionsByRole: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getQuestionsByRole: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load questions: " + e.getMessage()));
        }
    }

    /**
     * Get roles that have at least one question mapped
     * - If orgId is provided and valid (not null, not 0): returns roles for that
     * org with question mappings
     * - If orgId is null, 0, or empty: returns all roles with question mappings
     */
    @GetMapping("/rolesWithQuestions")
    public ResponseEntity<?> getRolesWithQuestions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "orgId", required = false) Integer orgId) {
        log.info("Request: getRolesWithQuestions received with orgId: {}", orgId);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in getRolesWithQuestions");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in getRolesWithQuestions");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            List<Role> roles;
            List<com.spearhead.ufc.model.RoleQuestionMapping> mappings;

            // Check if orgId is valid (not null and not 0)
            boolean hasValidOrgId = orgId != null && orgId > 0;

            if (hasValidOrgId) {
                // Fetch roles for specific org
                roles = roleService.getRolesByOrgId(orgId);
                // Get question mappings for this org
                mappings = roleQuestionMappingRepository.findByOrg_OrgIdAndIsActiveTrue(orgId);
                log.info("Fetching roles with questions for orgId: {}", orgId);
            } else {
                // Fetch all roles
                roles = roleService.getAllRoles();
                // Get all active question mappings
                mappings = roleQuestionMappingRepository.findAllActiveRoleQuestionMappings();
                log.info("Fetching all roles with questions (no orgId filter)");
            }

            // Get set of role IDs that have at least one question mapping
            java.util.Set<Integer> rolesWithQuestions = mappings.stream()
                    .filter(m -> m.getRole() != null && m.getRole().getRoleId() != null)
                    .map(m -> m.getRole().getRoleId())
                    .collect(java.util.stream.Collectors.toSet());

            // Filter roles to only include those with question mappings
            List<Role> filteredRoles = roles.stream()
                    .filter(role -> role.getRoleId() != null && rolesWithQuestions.contains(role.getRoleId()))
                    .toList();

            log.info("Successfully retrieved {} roles with question mappings out of {} total roles",
                    filteredRoles.size(), roles.size());
            return ResponseEntity.ok(JsonResponse.of(true, filteredRoles, "Roles with questions loaded successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error in getRolesWithQuestions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in getRolesWithQuestions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load roles: " + e.getMessage()));
        }
    }

}
