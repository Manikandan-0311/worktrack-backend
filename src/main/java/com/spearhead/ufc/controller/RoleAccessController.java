package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.AuthToken;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.RoleMenuAccess;
import com.spearhead.ufc.model.RoleModuleAccess;
import com.spearhead.ufc.service.RoleAccessService;
import com.spearhead.ufc.utils.AuthUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/role-access")
public class RoleAccessController {

    private static final Logger log = LoggerFactory.getLogger(RoleAccessController.class);

    private final RoleAccessService roleAccessService;
    private final AuthUtil authUtil;

    public RoleAccessController(RoleAccessService roleAccessService, AuthUtil authUtil) {
        this.roleAccessService = roleAccessService;
        this.authUtil = authUtil;
    }

    @GetMapping("/roleaccesslist")
    public ResponseEntity<?> listGet(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Integer roleId) {
        log.info("Request: role access list received for roleId={} with Authorization header: {}", roleId, authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in role access list");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in role access list");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }
            log.debug("Employee {} authenticated for role access list", employee.getEmployeeId());
            Map<String, Object> data = roleAccessService.getRoleAccessListByRoleId(employee, roleId);
            log.info("Successfully retrieved role access lists for roleId={}", roleId);
            return ResponseEntity.ok(JsonResponse.of(true, data, "Menus, modules, and role access fetched"));
        } catch (IllegalArgumentException e) {
            log.warn("Role access list forbidden for roleId={}: {}", roleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (EntityNotFoundException e) {
            log.warn("Role access list role not found for roleId={}: {}", roleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in role access list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load role access list: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody RoleMenuAccess request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Role access add requested with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in role access add");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in role access add");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            String roleName = employee.getRoleId() != null ? employee.getRoleId().getRoleName() : null;
            if (employee.getOrg() == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                log.warn("Employee {} is not assigned to any organization", employee.getEmployeeId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
            }
            AuthToken authToken = new AuthToken();
            authToken.setEmployee(employee);

            JsonResponse serviceResult = roleAccessService.addRoleAccessWithAuth(request, authToken);
            if (!serviceResult.isSuccess()) {
                log.warn("Role access add failed: {}", serviceResult.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResult);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("roleMenuAccess", roleAccessService.listRoleMenuAccess(employee));
            data.put("roleModuleAccess", roleAccessService.listRoleModuleAccess(employee));
            log.info("Role access added successfully; returning updated access lists");
            return ResponseEntity.ok(JsonResponse.of(true, data, "Role access created"));
        } catch (Exception e) {
            log.error("Unexpected error adding role access: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to create role access: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody RoleMenuAccess request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Role access update requested for id {} with Authorization header: {}", id, authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in role access update");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in role access update");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            String roleName = employee.getRoleId() != null ? employee.getRoleId().getRoleName() : null;
            if (employee.getOrg() == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                log.warn("Employee {} is not assigned to any organization", employee.getEmployeeId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
            }
            AuthToken authToken = new AuthToken();
            authToken.setEmployee(employee);

            JsonResponse serviceResult = roleAccessService.updateRoleAccessWithAuth(id, request, authToken);
            if (!serviceResult.isSuccess()) {
                log.warn("Role access update failed for id {}: {}", id, serviceResult.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResult);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("roleMenuAccess", roleAccessService.listRoleMenuAccess(employee));
            data.put("roleModuleAccess", roleAccessService.listRoleModuleAccess(employee));
            log.info("Role access updated successfully for id {}; returning updated access lists", id);
            return ResponseEntity.ok(JsonResponse.of(true, data, "Role access updated"));
        } catch (IllegalArgumentException e) {
            log.error("Role access not found for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating role access {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to update role access: " + e.getMessage()));
        }
    }

    @PostMapping("/module/add")
    public ResponseEntity<?> addModule(@RequestBody RoleModuleAccess request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Role module access add requested with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in role module access add");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in role module access add");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            String roleName = employee.getRoleId() != null ? employee.getRoleId().getRoleName() : null;
            if (employee.getOrg() == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                log.warn("Employee {} is not assigned to any organization", employee.getEmployeeId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
            }
            AuthToken authToken = new AuthToken();
            authToken.setEmployee(employee);

            JsonResponse serviceResult = roleAccessService.addRoleModuleAccessWithAuth(request, authToken);
            if (!serviceResult.isSuccess()) {
                log.warn("Role module access add failed: {}", serviceResult.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResult);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("roleMenuAccess", roleAccessService.listRoleMenuAccess(employee));
            data.put("roleModuleAccess", roleAccessService.listRoleModuleAccess(employee));
            log.info("Role module access added successfully; returning updated access lists");
            return ResponseEntity.ok(JsonResponse.of(true, data, "Role module access created"));
        } catch (Exception e) {
            log.error("Unexpected error adding role module access: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to create role module access: " + e.getMessage()));
        }
    }

    @PutMapping("/module/{id}")
    public ResponseEntity<?> updateModule(@PathVariable Integer id, @RequestBody RoleModuleAccess request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Role module access update requested for id {} with Authorization header: {}", id, authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in role module access update");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in role module access update");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            String roleName = employee.getRoleId() != null ? employee.getRoleId().getRoleName() : null;
            if (employee.getOrg() == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                log.warn("Employee {} is not assigned to any organization", employee.getEmployeeId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "User is not assigned to any organization."));
            }
            AuthToken authToken = new AuthToken();
            authToken.setEmployee(employee);

            JsonResponse serviceResult = roleAccessService.updateRoleModuleAccessWithAuth(id, request, authToken);
            if (!serviceResult.isSuccess()) {
                log.warn("Role module access update failed for id {}: {}", id, serviceResult.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResult);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("roleMenuAccess", roleAccessService.listRoleMenuAccess(employee));
            data.put("roleModuleAccess", roleAccessService.listRoleModuleAccess(employee));
            log.info("Role module access updated successfully for id {}; returning updated access lists", id);
            return ResponseEntity.ok(JsonResponse.of(true, data, "Role module access updated"));
        } catch (IllegalArgumentException e) {
            log.error("Role module access not found for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating role module access {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to update role module access: " + e.getMessage()));
        }
    }

    @GetMapping("/role-access")
    public ResponseEntity<?> listGet(@RequestParam Long roleId) {
        log.info("Fetching role access for roleId: {}", roleId);
        try {
            List<RoleModuleAccess> accessList = roleAccessService.listRoleModuleAccess(roleId);
            return ResponseEntity.ok(accessList);
        } catch (EntityNotFoundException e) {
            log.error("Error fetching role access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }
}
