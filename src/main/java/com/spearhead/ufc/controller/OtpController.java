package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/authController")
@CrossOrigin(origins = "*")
public class OtpController {

	private static final Logger log = LoggerFactory.getLogger(OtpController.class);

	@Autowired
	private OtpService otpService;

	@PostMapping("/send-otp")
	public ResponseEntity<JsonResponse> sendOtp(@RequestBody Map<String, Object> payload) {
		try {
			String emailId = payload.get("emailId") != null ? payload.get("emailId").toString() : null;

			otpService.sendOtpToEmployeeEmail(emailId);

			Map<String, Object> data = new HashMap<>();
			data.put("emailId", emailId);
			return ResponseEntity.ok(JsonResponse.of(true, data, "OTP sent successfully"));
		} catch (IllegalArgumentException ex) {
			log.warn("OTP send validation failed: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (IllegalStateException ex) {
			log.warn("OTP send blocked: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error while sending OTP", ex);
			return ResponseEntity.internalServerError()
					.body(JsonResponse.of(false, null, "Unable to send OTP right now. Please try again."));
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<JsonResponse> verifyOtp(@RequestBody Map<String, Object> payload) {
		try {
			String emailId = payload.get("emailId") != null ? payload.get("emailId").toString() : null;
			String otp = payload.get("otp") != null ? payload.get("otp").toString() : null;

			Integer employeeId = otpService.verifyOtp(emailId, otp);

			Map<String, Object> data = new HashMap<>();
			data.put("employeeId", employeeId);
			data.put("emailId", emailId);
			return ResponseEntity.ok(JsonResponse.of(true, data, "OTP verified successfully"));
		} catch (IllegalArgumentException ex) {
			log.warn("OTP verify validation failed: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (IllegalStateException ex) {
			log.warn("OTP verify blocked: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error while verifying OTP", ex);
			return ResponseEntity.internalServerError()
					.body(JsonResponse.of(false, null, "Unable to verify OTP right now. Please try again."));
		}
	}

	@PostMapping("/update-password")
	public ResponseEntity<JsonResponse> updatePassword(@RequestBody Map<String, Object> payload) {
		try {
			Integer employeeId = null;
			if (payload.get("employeeId") != null) {
				employeeId = Integer.parseInt(payload.get("employeeId").toString());
			}
			String emailId = payload.get("emailId") != null ? payload.get("emailId").toString() : null;
			String password = payload.get("password") != null ? payload.get("password").toString() : null;

			Integer updatedEmployeeId = otpService.updateEmployeePassword(employeeId, emailId, password);

			Map<String, Object> data = new HashMap<>();
			data.put("employeeId", updatedEmployeeId);
			data.put("emailId", emailId);
			return ResponseEntity.ok(JsonResponse.of(true, data, "Password updated successfully"));
		} catch (IllegalArgumentException ex) {
			log.warn("Password update validation failed: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (IllegalStateException ex) {
			log.warn("Password update blocked: {}", ex.getMessage());
			return ResponseEntity.badRequest().body(JsonResponse.of(false, null, ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error while updating password", ex);
			return ResponseEntity.internalServerError()
					.body(JsonResponse.of(false, null, "Unable to update password right now. Please try again."));
		}
	}
}
