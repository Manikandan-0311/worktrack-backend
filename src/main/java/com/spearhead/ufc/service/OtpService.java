package com.spearhead.ufc.service;

import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class OtpService {

	private static final Logger log = LoggerFactory.getLogger(OtpService.class);
	private static final int OTP_EXPIRY_MINUTES = 1;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${spring.mail.username:}")
	private String fromEmail;

	public void sendOtpToEmployeeEmail(String emailId) {
		String normalizedEmail = emailId == null ? "" : emailId.trim();
		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException("Email ID is required");
		}

		Optional<Employee> employeeOpt = employeeRepository.findByEmailIdIgnoreCase(normalizedEmail);
		if (employeeOpt.isEmpty()) {
			throw new IllegalArgumentException("Staff email not found");
		}

		Employee employee = employeeOpt.get();
		if (employee.getIsActive() != null && !employee.getIsActive()) {
			throw new IllegalStateException("Employee account is inactive");
		}

		String otp = generateOtp();
		sendOtpEmail(normalizedEmail, otp);

		employee.setOtp(otp);
		employee.setOtpSend(true);
		employee.setOtpAttempts(0);
		employee.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
		employeeRepository.save(employee);
	}

	public Integer verifyOtp(String emailId, String otp) {
		String normalizedEmail = emailId == null ? "" : emailId.trim();
		String normalizedOtp = otp == null ? "" : otp.trim();

		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException("Email ID is required");
		}
		if (normalizedOtp.isEmpty()) {
			throw new IllegalArgumentException("OTP is required");
		}

		Optional<Employee> employeeOpt = employeeRepository.findByEmailIdIgnoreCase(normalizedEmail);
		if (employeeOpt.isEmpty()) {
			throw new IllegalArgumentException("Staff email not found");
		}

		Employee employee = employeeOpt.get();
		if (employee.getIsActive() != null && !employee.getIsActive()) {
			throw new IllegalStateException("Staff account is inactive");
		}

		if (employee.getOtpSend() == null || !employee.getOtpSend() || employee.getOtp() == null
				|| employee.getOtp().trim().isEmpty()) {
			throw new IllegalStateException("OTP not requested. Please request a new OTP.");
		}

		if (employee.getOtpExpiry() == null || LocalDateTime.now().isAfter(employee.getOtpExpiry())) {
			clearOtpFields(employee);
			employeeRepository.save(employee);
			throw new IllegalStateException("OTP has expired. Please request a new OTP.");
		}

		if (!normalizedOtp.equals(employee.getOtp())) {
			Integer attempts = employee.getOtpAttempts() == null ? 0 : employee.getOtpAttempts();
			employee.setOtpAttempts(attempts + 1);
			employeeRepository.save(employee);
			throw new IllegalArgumentException("Invalid OTP. Please enter the correct OTP.");
		}

		clearOtpFields(employee);
		employeeRepository.save(employee);
		return employee.getEmployeeId();
	}

	public Integer updateEmployeePassword(Integer employeeId, String emailId, String password) {
		if (employeeId == null || employeeId <= 0) {
			throw new IllegalArgumentException("Staff ID is required");
		}
		String normalizedEmail = emailId == null ? "" : emailId.trim();
		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException("Email ID is required");
		}
		String normalizedPassword = password == null ? "" : password.trim();
		if (normalizedPassword.isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (normalizedPassword.length() < 6) {
			throw new IllegalArgumentException("Password must be at least 6 characters");
		}

		Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
		if (employeeOpt.isEmpty()) {
			throw new IllegalArgumentException("Staff not found");
		}

		Employee employee = employeeOpt.get();
		if (employee.getEmailId() == null || !employee.getEmailId().equalsIgnoreCase(normalizedEmail)) {
			throw new IllegalArgumentException("Staff ID and email ID do not match");
		}
		if (employee.getIsActive() != null && !employee.getIsActive()) {
			throw new IllegalStateException("Staff account is inactive");
		}

		employee.setPasswordHash(passwordEncoder.encode(normalizedPassword));
		employee.setUpdatedDt(OffsetDateTime.now());
		clearOtpFields(employee);
		employeeRepository.save(employee);
		return employee.getEmployeeId();
	}

	private String generateOtp() {
		int otpNumber = 100000 + SECURE_RANDOM.nextInt(900000);
		return String.valueOf(otpNumber);
	}

	private void sendOtpEmail(String recipient, String otp) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			if (fromEmail != null && !fromEmail.isBlank()) {
				message.setFrom(fromEmail);
			}
			message.setTo(recipient);
			message.setSubject("UFC - Verification Code (OTP)");
			message.setText("Dear User,\n\n"
					+ "Your One-Time Password (OTP) for verification is:\n\n"
					+ otp + "\n\n"
					+ "This code is valid for 1 minute only. Please do not share this code with anyone for security reasons.\n\n"
					+ "If you did not request this OTP, please ignore this email.\n\n"
					+ "This is an auto-generated email sent for OTP verification purposes only. Please do not reply to this email.\n\n"
					+ "Best regards,\n"
					+ "UFC Support Team");
			mailSender.send(message);
		} catch (Exception ex) {
			log.error("Failed to send OTP email to {}", recipient, ex);
			throw new RuntimeException("Failed to send OTP email");
		}
	}

	private void clearOtpFields(Employee employee) {
		employee.setOtpSend(false);
		employee.setOtp(null);
		employee.setOtpExpiry(null);
		employee.setOtpAttempts(0);
	}
}
