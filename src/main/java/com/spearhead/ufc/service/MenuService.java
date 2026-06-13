
package com.spearhead.ufc.service;

import com.spearhead.ufc.model.MenuTO;
import com.spearhead.ufc.model.Module;
import com.spearhead.ufc.model.RoleMenuAccess;
import com.spearhead.ufc.model.RoleModuleAccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.spearhead.ufc.repository.MenuRepository;
import com.spearhead.ufc.repository.ModuleRepository;
import com.spearhead.ufc.repository.RoleMenuAccessRepository;
import com.spearhead.ufc.repository.RoleModuleAccessRepository;

@Service
public class MenuService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MenuService.class);

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private RoleMenuAccessRepository roleMenuAccessRepository;

    @Autowired
    private RoleModuleAccessRepository roleModuleAccessRepository;

    public List<MenuTO> getAllMenus() {
        return menuRepository.findByIsActiveTrue();
    }

    public List<MenuTO> getMenusForRole(Integer roleId) {
        // TODO: Implement roleId-based menu fetch logic using mapping table (e.g.,
        // RoleMenuAccess)
        // For now, return all active menus as a stub
        return menuRepository.findByIsActiveTrue();
    }

    public List<Module> getAllModules() {
        return moduleRepository.findByIsActiveTrue();
    }

    public List<Module> getModulesForRole(Integer roleId) {
        return moduleRepository.findByIsActiveTrue();
    }

    public List<RoleMenuAccess> getRoleMenuAccessForRoleAndOrg(Integer roleId, Integer orgId) {
        log.info("Fetching RoleMenuAccess entities for roleId: {} and orgId: {}", roleId, orgId);
        try {
            return roleMenuAccessRepository.findActiveByRoleIdAndMenuActive(roleId);
        } catch (Exception ex) {
            log.error("Error fetching RoleMenuAccess for roleId {} and orgId {}: {}", roleId, orgId, ex.getMessage(),
                    ex);
            throw new RuntimeException("Failed to fetch RoleMenuAccess for role and org", ex);
        }
    }

    public List<RoleModuleAccess> getRoleModuleAccessForRoleAndOrg(Integer roleId, Integer orgId) {
        log.info("Fetching RoleModuleAccess entities for roleId: {} and orgId: {}", roleId, orgId);
        try {
            return roleModuleAccessRepository.findActiveByRoleIdAndModuleActive(roleId);
        } catch (Exception ex) {
            log.error("Error fetching RoleModuleAccess for roleId {} and orgId {}: {}", roleId, orgId, ex.getMessage(),
                    ex);
            throw new RuntimeException("Failed to fetch RoleModuleAccess for role and org", ex);
        }
    }

}