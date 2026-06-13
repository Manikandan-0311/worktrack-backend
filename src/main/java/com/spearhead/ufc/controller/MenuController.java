package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.MenuTO;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.service.MenuService;
import com.spearhead.ufc.service.ModuleService;
import com.spearhead.ufc.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/menuController")
@CrossOrigin(origins = "*")
public class MenuController {
    private static final Logger log = LoggerFactory.getLogger(MenuController.class);

    @Autowired
    private MenuService menuService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/user-access")
    public ResponseEntity<?> getUserAccess(@RequestHeader("Authorization") String authHeader) {
        log.info("Request: getUserAccess with Authorization header: {}", authHeader);
        try {
            Employee user = authUtil.getEmployeeFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "User not found or token invalid. Please login again."));
            }
            Integer roleId = (user.getRoleId() != null) ? user.getRoleId().getRoleId() : null;
            Integer orgId = (user.getOrg() != null) ? user.getOrg().getOrgId() : null;
            if (roleId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "User role not found. Contact admin."));
            }
            List<MenuTO> menus;
            List<com.spearhead.ufc.model.Module> modules;
            try {
                if (user.getRoleId().getRoleName().equalsIgnoreCase("SUPER_ADMIN")) {
                    menus = menuService.getAllMenus();
                    modules = moduleService.getAllModules();
                } else {
                    List<com.spearhead.ufc.model.RoleMenuAccess> roleMenuAccessList = menuService
                            .getRoleMenuAccessForRoleAndOrg(roleId, orgId);
                    menus = roleMenuAccessList.stream()
                            .map(RoleMenuAccess -> RoleMenuAccess.getMenu()) // assuming getMenu() returns MenuTO
                            .toList();
                    List<com.spearhead.ufc.model.RoleModuleAccess> roleModuleAccessList = menuService
                            .getRoleModuleAccessForRoleAndOrg(roleId, orgId);
                    modules = roleModuleAccessList.stream()
                            .map(RoleModuleAccess -> RoleModuleAccess.getModule())
                            .toList();
                }
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(JsonResponse.of(false, null, "Failed to fetch menus/modules: " + ex.getMessage()));
            }
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("menus", menus);
            responseData.put("modules", modules);
            return ResponseEntity.ok(JsonResponse.of(true, responseData, "Access loaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load access: " + e.getMessage()));
        }
    }
}