package com.spearhead.ufc.service;

import com.spearhead.ufc.model.AuthToken;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.RoleMenuAccess;
import com.spearhead.ufc.model.RoleModuleAccess;
import com.spearhead.ufc.model.User;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.model.MenuTO;
import com.spearhead.ufc.model.Module;
import com.spearhead.ufc.repository.MenuRepository;
import com.spearhead.ufc.repository.ModuleRepository;
import com.spearhead.ufc.repository.RoleMenuAccessRepository;
import com.spearhead.ufc.repository.RoleModuleAccessRepository;
import com.spearhead.ufc.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

@Service
public class RoleAccessService {

    private static final Logger log = LoggerFactory.getLogger(RoleAccessService.class);

    private final RoleMenuAccessRepository roleMenuAccessRepository;
    private final RoleModuleAccessRepository roleModuleAccessRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final ModuleRepository moduleRepository;

    public RoleAccessService(RoleMenuAccessRepository roleMenuAccessRepository,
            RoleModuleAccessRepository roleModuleAccessRepository, RoleRepository roleRepository,
            MenuRepository menuRepository, ModuleRepository moduleRepository) {
        this.roleMenuAccessRepository = roleMenuAccessRepository;
        this.roleModuleAccessRepository = roleModuleAccessRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.moduleRepository = moduleRepository;
    }

    public List<RoleMenuAccess> listRoleMenuAccess(Employee emp) {
        String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
        if ("SUPER_ADMIN".equalsIgnoreCase(roleName)) {
            log.info("Listing all active RoleMenuAccess for SUPER_ADMIN (role+menu join)");
            try {
                return roleMenuAccessRepository.findAllActiveWithExistingRole();
            } catch (Exception ex) {
                log.error("Error listing RoleMenuAccess for SUPER_ADMIN: {}", ex.getMessage(), ex);
                return List.of();
            }
        }
        Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
        if (orgId == null)
            return List.of();
        log.info("Listing RoleMenuAccess for org {}", orgId);
        try {
            return roleMenuAccessRepository.findAllByOrgId(orgId);
        } catch (Exception ex) {
            log.error("Error listing RoleMenuAccess for org {}: {}", orgId, ex.getMessage(), ex);
            return List.of();
        }
    }

    public List<RoleModuleAccess> listRoleModuleAccess(Employee emp) {
        String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
        if ("SUPER_ADMIN".equalsIgnoreCase(roleName)) {
            log.info("Listing all active RoleModuleAccess for SUPER_ADMIN (role+module join)");
            try {
                return roleModuleAccessRepository.findAllActiveWithExistingRole();
            } catch (Exception ex) {
                log.error("Error listing RoleModuleAccess for SUPER_ADMIN: {}", ex.getMessage(), ex);
                return List.of();
            }
        }
        Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
        if (orgId == null)
            return List.of();
        log.info("Listing RoleModuleAccess for org {}", orgId);
        try {
            return roleModuleAccessRepository.findAllByOrgId(orgId);
        } catch (Exception ex) {
            log.error("Error listing RoleModuleAccess for org {}: {}", orgId, ex.getMessage(), ex);
            return List.of();
        }
    }

    public JsonResponse listRoleAccess(User user) {
        try {
            String roleName = user.getRoleId().getRoleName();
            List<RoleMenuAccess> result;
            if ("SUPER_ADMIN".equalsIgnoreCase(roleName)) {
                log.info("Listing role access for SUPER_ADMIN across all orgs");
                result = roleMenuAccessRepository.findAll();
            } else {
                Integer orgId = user.getOrg().getOrgId();
                log.info("Listing role access for org {}", orgId);
                result = roleMenuAccessRepository.findAllByOrgId(orgId);
            }
            return JsonResponse.of(true, result, "Role access fetched");
        } catch (Exception e) {
            log.error("Failed to list role access", e);
            return JsonResponse.of(false, null, "Failed to list role access: " + e.getMessage());
        }
    }

    @Transactional
    public JsonResponse addRoleAccessWithAuth(final RoleMenuAccess request, final AuthToken authToken) {
        try {
            Employee emp = authToken.getEmployee();
            String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
            Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
            if (orgId == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                return JsonResponse.of(false, null, "User is not assigned to any organization.");
            }
            // If not super admin, restrict to user's org
            if (!"SUPER_ADMIN".equalsIgnoreCase(roleName)) {
                if (request.getRole() == null || request.getRole().getOrg() == null ||
                        !orgId.equals(request.getRole().getOrg().getOrgId())) {
                    return JsonResponse.of(false, null,
                            "You are not allowed to add role access for other organizations");
                }
            }
            // Validate required associations
            if (request.getMenu() == null || request.getMenu().getMenuId() == null) {
                return JsonResponse.of(false, null, "Menu is required for role access");
            }
            if (request.getRole() == null || request.getRole().getRoleId() == null) {
                return JsonResponse.of(false, null, "Role is required for role access");
            }

            Integer roleId = request.getRole().getRoleId();
            Integer menuId = request.getMenu().getMenuId();
            OffsetDateTime now = OffsetDateTime.now();

            Optional<RoleMenuAccess> existingOpt = roleMenuAccessRepository
                    .findFirstByRoleRoleIdAndMenuMenuIdOrderByRoleMenuAccessIdDesc(roleId, menuId);

            if (existingOpt.isPresent()) {
                RoleMenuAccess existing = existingOpt.get();
                existing.setIsActive(request.getIsActive() != null ? request.getIsActive() : existing.getIsActive());
                existing.setUpdatedBy(emp != null ? emp.getEmployeeId() : null);
                existing.setUpdatedDt(now);
                RoleMenuAccess saved = roleMenuAccessRepository.save(existing);
                return JsonResponse.of(true, saved, "Role access updated");
            }

            request.setCreatedBy(emp != null ? emp.getEmployeeId() : request.getCreatedBy());
            request.setCreatedDt(now);
            request.setUpdatedBy(emp != null ? emp.getEmployeeId() : request.getUpdatedBy());
            request.setUpdatedDt(now);
            if (request.getIsActive() == null) {
                request.setIsActive(true);
            }
            RoleMenuAccess saved = roleMenuAccessRepository.save(request);
            return JsonResponse.of(true, saved, "Role access created");
        } catch (Exception e) {
            log.error("Failed to create role access", e);
            return JsonResponse.of(false, null, "Failed to create role access: " + e.getMessage());
        }
    }

    @Transactional
    public JsonResponse updateRoleAccessWithAuth(final Integer id, final RoleMenuAccess request,
            final AuthToken authToken) {
        try {
            Employee emp = authToken.getEmployee();
            String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
            Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
            if (orgId == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                return JsonResponse.of(false, null, "User is not assigned to any organization.");
            }
            RoleMenuAccess existing = roleMenuAccessRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role access not found: " + id));
            // If not super admin, restrict to user's org
            if (!"SUPER_ADMIN".equalsIgnoreCase(roleName)) {
                if (existing.getRole() == null || existing.getRole().getOrg() == null ||
                        !orgId.equals(existing.getRole().getOrg().getOrgId())) {
                    return JsonResponse.of(false, null,
                            "You are not allowed to update role access for other organizations");
                }
            }
            // ...existing code... map allowed fields from request to existing
            existing.setIsActive(request.getIsActive());
            existing.setMenu(request.getMenu());
            existing.setRole(request.getRole());
            existing.setUpdatedBy(emp != null ? emp.getEmployeeId() : null);
            existing.setUpdatedDt(OffsetDateTime.now());

            RoleMenuAccess saved = roleMenuAccessRepository.save(existing);
            return JsonResponse.of(true, saved, "Role access updated");
        } catch (Exception e) {
            log.error("Failed to update role access {}", id, e);
            return JsonResponse.of(false, null, "Failed to update role access: " + e.getMessage());
        }
    }

    @Transactional
    public JsonResponse addRoleModuleAccessWithAuth(final RoleModuleAccess request, final AuthToken authToken) {
        try {
            Employee emp = authToken.getEmployee();
            String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
            Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
            if (orgId == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                return JsonResponse.of(false, null, "User is not assigned to any organization.");
            }
            if (!"SUPER_ADMIN".equalsIgnoreCase(roleName)) {
                if (request.getRole() == null || request.getRole().getOrg() == null ||
                        !orgId.equals(request.getRole().getOrg().getOrgId())) {
                    return JsonResponse.of(false, null,
                            "You are not allowed to add role module access for other organizations");
                }
            }
            OffsetDateTime now = OffsetDateTime.now();
            request.setCreatedBy(emp != null ? emp.getEmployeeId() : request.getCreatedBy());
            request.setCreatedDt(now);
            request.setUpdatedBy(emp != null ? emp.getEmployeeId() : request.getUpdatedBy());
            request.setUpdatedDt(now);
            if (request.getIsActive() == null) {
                request.setIsActive(true);
            }
            RoleModuleAccess saved = roleModuleAccessRepository.save(request);
            return JsonResponse.of(true, saved, "Role module access created");
        } catch (Exception e) {
            log.error("Failed to create role module access", e);
            return JsonResponse.of(false, null, "Failed to create role module access: " + e.getMessage());
        }
    }

    @Transactional
    public JsonResponse updateRoleModuleAccessWithAuth(final Integer id, final RoleModuleAccess request,
            final AuthToken authToken) {
        try {
            Employee emp = authToken.getEmployee();
            String roleName = emp != null && emp.getRoleId() != null ? emp.getRoleId().getRoleName() : null;
            Integer orgId = (emp != null && emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
            if (orgId == null && (roleName == null || !"SUPER_ADMIN".equalsIgnoreCase(roleName))) {
                return JsonResponse.of(false, null, "User is not assigned to any organization.");
            }
            RoleModuleAccess existing = roleModuleAccessRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role module access not found: " + id));
            if (!"SUPER_ADMIN".equalsIgnoreCase(roleName)) {
                if (existing.getRole() == null || existing.getRole().getOrg() == null ||
                        !orgId.equals(existing.getRole().getOrg().getOrgId())) {
                    return JsonResponse.of(false, null,
                            "You are not allowed to update role module access for other organizations");
                }
            }
            existing.setIsActive(request.getIsActive());
            existing.setModule(request.getModule());
            existing.setMenu(request.getMenu());
            existing.setRole(request.getRole());
            existing.setUpdatedBy(emp != null ? emp.getEmployeeId() : null);
            existing.setUpdatedDt(OffsetDateTime.now());

            RoleModuleAccess saved = roleModuleAccessRepository.save(existing);
            return JsonResponse.of(true, saved, "Role module access updated");
        } catch (Exception e) {
            log.error("Failed to update role module access {}", id, e);
            return JsonResponse.of(false, null, "Failed to update role module access: " + e.getMessage());
        }
    }

    public List<RoleModuleAccess> listRoleModuleAccess(Long roleId) {
        log.info("Fetching RoleModuleAccess for roleId: {}", roleId);
        Optional<Role> roleOptional = roleRepository.findById(roleId.intValue());
        if (roleOptional.isEmpty()) {
            log.error("Role with id {} not found", roleId);
            throw new EntityNotFoundException("Role with id " + roleId + " not found");
        }
        Role role = roleOptional.get();
        // Proceed with fetching RoleModuleAccess
        return roleModuleAccessRepository.findAllByRole(role);
    }

    public java.util.Map<String, Object> getRoleAccessListByRoleId(Employee requester, Integer roleId) {
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isEmpty()) {
            throw new EntityNotFoundException("Role with id " + roleId + " not found");
        }

        Role targetRole = roleOptional.get();
        String roleName = requester != null && requester.getRoleId() != null ? requester.getRoleId().getRoleName() : null;
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(roleName);

        if (!isSuperAdmin) {
            Integer requesterOrgId = requester != null && requester.getOrg() != null ? requester.getOrg().getOrgId() : null;
            Integer targetOrgId = targetRole.getOrg() != null ? targetRole.getOrg().getOrgId() : null;
            if (requesterOrgId == null || targetOrgId == null || !requesterOrgId.equals(targetOrgId)) {
                throw new IllegalArgumentException("You are not allowed to view role access for other organizations");
            }
        }

        List<MenuTO> allMenus = menuRepository.findByIsActiveTrue();
        List<Module> allModules = moduleRepository.findByIsActiveTrue();
        List<RoleMenuAccess> roleMenuAccess = roleMenuAccessRepository.findByRoleIdAndMenuActive(roleId);
        List<RoleModuleAccess> roleModuleAccess = roleModuleAccessRepository.findByRoleIdAndModuleActive(roleId);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("roleId", roleId);
        data.put("menus", allMenus);
        data.put("modules", allModules);
        data.put("roleMenuAccess", roleMenuAccess);
        data.put("roleModuleAccess", roleModuleAccess);
        return data;
    }
}
