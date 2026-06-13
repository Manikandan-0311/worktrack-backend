package com.spearhead.ufc.controller;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.spearhead.ufc.config.JwtUtil;
import com.spearhead.ufc.controller.AuthController.RefreshRequest;
import com.spearhead.ufc.model.AuthToken;
import com.spearhead.ufc.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/authTokenController")
@CrossOrigin(origins = "*")
public class AuthTokenController {

	private static final Logger log = LoggerFactory.getLogger(AuthTokenController.class);

	@Autowired
	private final JwtUtil jwtUtil;

	@Autowired
	private final AuthTokenService authTokenService;

	// No longer need UserRepository; tokens are issued with employeeId

	public AuthTokenController(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
		this.authTokenService = new AuthTokenService();
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
		log.info("Enter into refresh method - AuthTokenController");
		Optional<AuthToken> tokenOpt = authTokenService.findByToken(request.refreshToken);
		try {
			if (tokenOpt.isEmpty()) {
				return ResponseEntity.status(401).body("Invalid refresh token");
			}

			AuthToken refreshToken = tokenOpt.get();

			if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
				return ResponseEntity.status(401).body("Refresh token expired");
			}

			// Issue a new access token using employeeId as the subject
			Integer employeeId = refreshToken.getEmployee() != null ? refreshToken.getEmployee().getEmployeeId() : null;
			if (employeeId == null) {
				return ResponseEntity.status(401).body("Invalid token payload");
			}
			String newAccessToken = jwtUtil.generateToken(employeeId);

			Map<String, Object> resp = new HashMap<>();
			resp.put("accessToken", newAccessToken);

			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error Occured into refresh method -AuthTokenController", e.getMessage(), e);
			return null;
		} finally {
			log.info("Exited into  refresh method- AuthTokenController");
		}
	}

}
