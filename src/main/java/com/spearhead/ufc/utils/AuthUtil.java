package com.spearhead.ufc.utils;

import com.spearhead.ufc.config.JwtUtil;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.User;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {
    private static final Logger log = LoggerFactory.getLogger(AuthUtil.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Extracts user details from JWT token in Authorization header.
     * Returns null if token is invalid or user not found.
     */
    public User getUserFromToken(String authHeader) {
        log.debug("Attempting to extract user from Authorization header");
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("Authorization header is null or empty");
            return null;
        }
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with 'Bearer ': {}",
                    authHeader.substring(0, Math.min(20, authHeader.length())));
            return null;
        }

        String token = authHeader.substring(7);
        log.debug("Extracted token from header");

        String userIdStr;
        try {
            userIdStr = jwtUtil.getUsername(token);
            log.debug("Successfully extracted username from token: {}", userIdStr);
        } catch (Exception ex) {
            log.error("Error extracting username from JWT token: {}", ex.getMessage(), ex);
            return null;
        }

        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            log.warn("Username extracted from token is null or empty");
            return null;
        }

        try {
            int userId = Integer.parseInt(userIdStr);
            User user = userService.findById(userId);
            if (user == null) {
                log.warn("User not found in database with ID: {}", userId);
                return null;
            }
            log.debug("Successfully retrieved user from database: {}", userId);
            return user;
        } catch (NumberFormatException e) {
            log.error("Error parsing user ID from token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving user from database: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * New method: Extracts employee details from JWT token in Authorization header.
     * Works when the JWT subject is the employeeId.
     */
    public Employee getEmployeeFromToken(String authHeader) {
        log.debug("Attempting to extract employee from Authorization header");
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("Authorization header is null or empty");
            return null;
        }
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with 'Bearer ': {}",
                    authHeader.substring(0, Math.min(20, authHeader.length())));
            return null;
        }

        String token = authHeader.substring(7);
        log.debug("Extracted token from header");

        String employeeIdStr;
        try {
            employeeIdStr = jwtUtil.getUsername(token);
            log.debug("Successfully extracted subject from token: {}", employeeIdStr);
        } catch (Exception ex) {
            log.error("Error extracting subject from JWT token: {}", ex.getMessage(), ex);
            return null;
        }

        if (employeeIdStr == null || employeeIdStr.trim().isEmpty()) {
            log.warn("Subject extracted from token is null or empty");
            return null;
        }

        try {
            int employeeId = Integer.parseInt(employeeIdStr);
            return employeeRepository.findById(employeeId).orElse(null);
        } catch (NumberFormatException e) {
            log.error("Error parsing employee ID from token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving employee from database: {}", e.getMessage(), e);
            return null;
        }
    }
}
