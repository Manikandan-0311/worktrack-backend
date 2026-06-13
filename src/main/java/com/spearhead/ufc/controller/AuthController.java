package com.spearhead.ufc.controller;

import com.spearhead.ufc.config.JwtUtil;
import com.spearhead.ufc.dto.LoginDTO;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.service.AuthTokenService;
import com.spearhead.ufc.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/authController")
@CrossOrigin(origins = "*")
public class AuthController {
	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserService userService;

	@Autowired
	private AuthTokenService authTokenService;

	public AuthController() {
	}

	public static class RefreshRequest {
		public String refreshToken;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginDTO request) {
		log.info("Enter into Login method - AuthController");
		try {
			Employee employee = userService.authenticateEmployee(request.username, request.password);
			if (employee == null) {
				return ResponseEntity.ok(JsonResponse.of(false, null, "Invalid username or password"));
			}
			
			// Check if organization is active (skip check for Super_Admin role)
			boolean isSuperAdmin = employee.getRoleId() != null && "Super_Admin".equalsIgnoreCase(employee.getRoleId().getRoleName());
			if (!isSuperAdmin && employee.getOrg() != null && (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive())) {
				return ResponseEntity.ok(JsonResponse.of(false, null, "Your organization is currently inactive. Please contact your administrator for assistance."));
			}
			
			String accessToken = jwtUtil.generateToken(employee.getEmployeeId());
			String refreshToken = jwtUtil.generateRefreshToken(employee.getEmployeeId());
			authTokenService.saveRefreshToken(employee, refreshToken);

			// Build response data
			java.util.Map<String, Object> responseData = new java.util.HashMap<>();
			responseData.put("userName", employee.getUsername());
			responseData.put("userId", employee.getEmployeeId());
			responseData.put("specialPermissionFlag", employee.getSpecialPermissionFlag());
			if (employee.getOrg() != null) {
				responseData.put("orgId", employee.getOrg().getOrgId());
			}
			try {
				if (employee.getDepartment() != null) {
					responseData.put("departmentId", employee.getDepartment().getDepartmentId());
				}
				if (employee.getLocation() != null) {
					responseData.put("locationId", employee.getLocation().getLocationId());
				}
				if (employee.getRoleId() != null) {
					responseData.put("roleId", employee.getRoleId().getRoleId());
					// Provide a nested userRole object for frontend convenience
					java.util.Map<String, Object> userRole = new java.util.HashMap<>();
					userRole.put("roleId", employee.getRoleId().getRoleId());
					userRole.put("roleName", employee.getRoleId().getRoleName());
					responseData.put("userRole", userRole);
					responseData.put("userRoleName", employee.getRoleId().getRoleName());
				}
			} catch (Exception ex) {
				log.warn("Employee details could not be loaded: {}", ex.getMessage());
			}
			responseData.put("accessToken", accessToken);
			responseData.put("refreshToken", refreshToken);
			return ResponseEntity.ok(JsonResponse.of(true, responseData, "Login successful"));
		} catch (Exception e) {
			log.error("Error during login process: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(JsonResponse.of(false, null, "An error occurred during login. Please try again later."));
		} finally {
			log.info("Exited from Login method - AuthController");
		}
	}

}
