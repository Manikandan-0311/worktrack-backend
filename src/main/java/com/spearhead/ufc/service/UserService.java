package com.spearhead.ufc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spearhead.ufc.model.User;
import com.spearhead.ufc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.EmployeeRepository;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	@Autowired
	private final UserRepository userRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Authenticate by username or email. Returns the User on success (and updates
	 * lastLoginDt), or null on failure.
	 */

	
	@Transactional
	public User authenticate(String login, String rawPassword) {
		log.info("Enter into authenticate method - UserService ");
		try {
			Optional<Employee> eopt = employeeRepository.findByUsername(login);

			if (eopt.isEmpty()) {
				// Try email as fallback
				Optional<Employee> empByEmailOpt = employeeRepository.findByEmailId(login);
				if (empByEmailOpt.isPresent()) {
					eopt = empByEmailOpt;
				}
			}
			if (eopt.isEmpty())
				return null;

			Employee emp = eopt.get();
			if (Boolean.FALSE.equals(emp.getIsActive()))
				return null;

			String storedHash = emp.getPasswordHash();
			if (storedHash == null || storedHash.isEmpty())
				return null;
			boolean matches;
			if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
				matches = passwordEncoder.matches(rawPassword, storedHash);
			} else {
				matches = storedHash.equals(rawPassword);
			}
			if (!matches) {
				return null;
			}
			// Resolve linked User for JWT/context and update last login
			Optional<User> uopt = userRepository.findFirstByEmployee_EmployeeId(emp.getEmployeeId());
			if (uopt.isEmpty()) return null;
			User user = uopt.get();
			if (Boolean.FALSE.equals(user.getIsActive())) return null;
			user.setLastLoginDt(OffsetDateTime.now());
			userRepository.save(user);
			return user;
		} catch (Exception e) {
			log.error("Error during login process: {}", e.getMessage(), e);
			return null;
		} finally {
			log.info("Exited from authenticate method - UserService ");
		}

	}

	public User findById(int userId) {
		try {
			Optional<User> opt = userRepository.findByUserId(userId);
			return opt.orElse(null);
		} catch (Exception e) {
			log.error("Error finding user by id: {}", userId, e);
			return null;
		}
	}

	public List<User> getAllUsers() {
		try {
			return userRepository.findAll();
		} catch (Exception e) {
			log.error("Error retrieving all users", e);
			return List.of();
		}
	}

	public List<User> getUsersByOrgId(Integer orgId) {
		try {
			return userRepository.findAll().stream()
					.filter(u -> u.getOrg() != null && orgId.equals(u.getOrg().getOrgId()))
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error retrieving users by orgId {}", orgId, e);
			return List.of();
		}
	}

	public User addUser(User request, User actingUser) {
		try {
			// Hash the password if provided
			if (request.getPassword() != null && !request.getPassword().isEmpty()) {
				request.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			}
			if (request.getPasswordHash() == null || request.getPasswordHash().isEmpty()) {
				throw new IllegalArgumentException("Password must be provided.");
			}
			return userRepository.save(request);
		} catch (Exception e) {
			log.error("Error adding user", e);
			throw e;
		}
	}

	public User updateUser(User request, User actingUser) {
		try {
			Optional<User> existingOpt = userRepository.findById(request.getUserId());
			if (existingOpt.isEmpty()) {
				throw new IllegalArgumentException("User not found: " + request.getUserId());
			}
			User existing = existingOpt.get();
			existing.setIsActive(request.getIsActive());
			existing.setOrg(request.getOrg());
			existing.setRoleId(request.getRoleId());
			existing.setUpdatedBy(request.getUpdatedBy());
			existing.setUpdatedDt(request.getUpdatedDt());

			// Hash and update password if a new plain password is provided
			if (request.getPassword() != null && !request.getPassword().isEmpty()) {
				existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			} else if (request.getPasswordHash() != null && !request.getPasswordHash().isEmpty()) {
				// Or update hash directly if provided
				existing.setPasswordHash(request.getPasswordHash());
			}
			return userRepository.save(existing);
		} catch (Exception e) {
			log.error("Error updating user {}", request.getUserId(), e);
			throw e;
		}
	}

	/**
	 * Authenticate by username or email against the Employee table.
	 * Returns the Employee on success, or null on failure.
	 */
	@Transactional(readOnly = true)
	public Employee authenticateEmployee(String login, String rawPassword) {
		log.info("Enter into authenticateEmployee method - UserService ");
		try {
			Optional<Employee> eopt = employeeRepository.findByUsername(login);
			if (eopt.isEmpty()) {
				Optional<Employee> empByEmailOpt = employeeRepository.findByEmailId(login);
				if (empByEmailOpt.isPresent()) {
					eopt = empByEmailOpt;
				}
			}
			if (eopt.isEmpty()) return null;

			Employee emp = eopt.get();
			if (Boolean.FALSE.equals(emp.getIsActive())) return null;

			String storedHash = emp.getPasswordHash();
			if (storedHash == null || storedHash.isEmpty()) return null;
			boolean matches;
			if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
				matches = passwordEncoder.matches(rawPassword, storedHash);
			} else {
				matches = storedHash.equals(rawPassword);
			}
			if (!matches) return null;

			return emp;
		} catch (Exception e) {
			log.error("Error during employee login process: {}", e.getMessage(), e);
			return null;
		} finally {
			log.info("Exited from authenticateEmployee method - UserService ");
		}
	}
}
