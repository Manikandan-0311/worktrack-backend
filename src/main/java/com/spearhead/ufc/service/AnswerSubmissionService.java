package com.spearhead.ufc.service;

import com.spearhead.ufc.model.AnswerSubmissionTimeExtension;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.repository.AnswerSubmissionTimeExtensionRepository;
import com.spearhead.ufc.repository.CalendarRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.EmployeeLocationAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Returns special permissions for the employee's org if super admin, else only
 * for that org's employees based on special permission flag.
 */

@Service
public class AnswerSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AnswerSubmissionService.class);

    private final AnswerSubmissionTimeExtensionRepository repository;
    private final EmployeeLocationAccessRepository employeeLocationAccessRepository;
    private final EmployeeRepository employeeRepository;
    private final CalendarRepository calendarRepository;

    public AnswerSubmissionService(AnswerSubmissionTimeExtensionRepository repository,
            EmployeeLocationAccessRepository employeeLocationAccessRepository,
            EmployeeRepository employeeRepository,
            CalendarRepository calendarRepository) {
        this.repository = repository;
        this.employeeLocationAccessRepository = employeeLocationAccessRepository;
        this.employeeRepository = employeeRepository;
        this.calendarRepository = calendarRepository;
    }

    public JsonResponse addSpecialPermission(AnswerSubmissionTimeExtension request) {
        try {
            if (request == null || request.getEmployee() == null || request.getEmployee().getEmployeeId() == null) {
                return JsonResponse.of(false, null, "Staff is required.");
            }
            if (request.getQuestionDate() == null) {
                return JsonResponse.of(false, null, "Compliance date is required.");
            }

            if (Boolean.TRUE.equals(request.getIsHoliday())) {
                Integer employeeId = request.getEmployee().getEmployeeId();
                Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
                if (employeeOpt.isEmpty() || employeeOpt.get().getOrg() == null
                        || employeeOpt.get().getOrg().getOrgId() == null) {
                    return JsonResponse.of(false, null,
                            "Unable to validate holiday. Staff organization details are missing.");
                }

                Integer orgId = employeeOpt.get().getOrg().getOrgId();
                Optional<com.spearhead.ufc.model.Calendar> calendarOpt = calendarRepository
                        .findByOrgOrgIdAndCalendarDate(orgId, request.getQuestionDate());

                boolean isHolidayInCalendar = calendarOpt.isPresent()
                        && Boolean.TRUE.equals(calendarOpt.get().getIsActive());
                if (!isHolidayInCalendar) {
                    return JsonResponse.of(false, null,
                            "Selected date is not marked as holiday in calendar. Holiday special permission cannot be added.");
                }
            }

            log.info("Adding special permission for employeeId: {}, questionDate: {}",
                    request.getEmployee().getEmployeeId(), request.getQuestionDate());
            AnswerSubmissionTimeExtension saved = repository.save(request);
            log.info("Special permission added successfully with ID: {}", saved.getExtensionId());
            return JsonResponse.of(true, saved, "Special permission added successfully.");
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                    ? e.getMostSpecificCause().getMessage().toLowerCase()
                    : "";
            if (message.contains("uq_employee_day_extension")
                    || (message.contains("employee_id") && message.contains("question_date"))) {
                return JsonResponse.of(false, null,
                        "Special permission already exists for this staff on the selected date.");
            }
            log.error("Failed to add special permission due to data conflict", e);
            return JsonResponse.of(false, null, "Unable to add special permission due to data conflict.");
        } catch (Exception e) {
            log.error("Failed to add special permission", e);
            return JsonResponse.of(false, null, "Failed to add special permission. Please try again.");
        }
    }

    public JsonResponse updateSpecialPermission(AnswerSubmissionTimeExtension request, Employee updater) {
        try {
            if (request.getExtensionId() == null) {
                return JsonResponse.of(false, null, "extensionId is required for update");
            }

            Integer id = request.getExtensionId();
            java.util.Optional<AnswerSubmissionTimeExtension> existingOpt = repository.findById(id);
            if (existingOpt.isEmpty()) {
                return JsonResponse.of(false, null, "Special permission not found for update");
            }

            AnswerSubmissionTimeExtension existing = existingOpt.get();

            // Update allowed mutable fields
            if (request.getReason() != null) {
                existing.setReason(request.getReason());
            }
            if (request.getQuestionDate() != null) {
                existing.setQuestionDate(request.getQuestionDate());
            }
            if (request.getIsActive() != null) {
                existing.setIsActive(request.getIsActive());
            }
            if (request.getIsHoliday() != null) {
                existing.setIsHoliday(request.getIsHoliday());
            }
            if (request.getPermissionStatusId() != null) {
                existing.setPermissionStatusId(request.getPermissionStatusId());
            }

            // Set updater info from auth token
            if (updater != null && updater.getEmployeeId() != null) {
                existing.setUpdatedBy(updater.getEmployeeId());
                existing.setUpdatedDt(java.time.OffsetDateTime.now());
            }

            AnswerSubmissionTimeExtension saved = repository.save(existing);
            return JsonResponse.of(true, saved, "Special permission updated successfully");
        } catch (Exception e) {
            log.error("Failed to update special permission", e);
            return JsonResponse.of(false, null, "Failed to update special permission: " + e.getMessage());
        }
    }

    public JsonResponse getSpecialPermissionById(Integer id) {
        try {
            log.info("Fetching special permission with ID: {}", id);
            Optional<AnswerSubmissionTimeExtension> result = repository.findById(id);
            if (result.isPresent()) {
                log.info("Special permission found with ID: {}", id);
                return JsonResponse.of(true, result.get(), "Special permission fetched successfully.");
            } else {
                log.warn("Special permission not found with ID: {}", id);
                return JsonResponse.of(false, null, "Special permission not found.");
            }
        } catch (Exception e) {
            log.error("Failed to fetch special permission with ID: {}", id, e);
            return JsonResponse.of(false, null, "Failed to fetch special permission: " + e.getMessage());
        }
    }

    public JsonResponse getSpecialPermissionsForEmployee(Employee employee) {
        try {
            Role role = employee.getRoleId();
            String roleName = role != null ? role.getRoleName() : null;
            Integer roleId = role != null ? role.getRoleId() : null;

            log.debug("Employee id={} roleId={} roleName={}", employee.getEmployeeId(), roleId, roleName);

            boolean isSuperAdmin = false;
            if (roleName != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(roleName) || roleName.toUpperCase().contains("SUPER")) {
                    isSuperAdmin = true;
                }
            }

            List<AnswerSubmissionTimeExtension> result;

            if (isSuperAdmin) {
                // SUPER_ADMIN: Get all special permissions
                result = repository.findAll();
                log.info("Fetching all special permissions for SUPER_ADMIN");
            } else {
                Integer empId = employee.getEmployeeId();
                if (empId == null) {
                    return JsonResponse.of(false, null, "Employee id not found in token");
                }

                // Check if employee has special permission flag enabled
                Boolean hasSpecialPermission = employee.getSpecialPermissionFlag();

                if (hasSpecialPermission != null && hasSpecialPermission) {
                    // Employee has special permission flag: Load all permissions for their org and
                    // accessible locations
                    Integer orgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;

                    if (orgId == null) {
                        log.warn("Employee {} has special permission flag but no org assigned", empId);
                        return JsonResponse.of(false, null, "Employee organization not found");
                    }

                    // Get all locations the employee has access to
                    List<EmployeeLocationAccess> locationAccess = employeeLocationAccessRepository
                            .findByEmployeeEmployeeIdAndIsActiveTrue(empId);

                    Set<Integer> accessibleLocationIds = locationAccess.stream()
                            .map(ela -> ela.getLocation() != null ? ela.getLocation().getLocationId() : null)
                            .filter(id -> id != null)
                            .collect(Collectors.toSet());

                    log.info("Employee {} has access to {} locations", empId, accessibleLocationIds.size());

                    // Get all special permissions for the org
                    List<AnswerSubmissionTimeExtension> allOrgPermissions = repository.findAll();

                    // Filter to include only permissions for accessible locations
                    result = allOrgPermissions.stream()
                            .filter(permission -> {
                                if (permission.getEmployee() == null) {
                                    return false;
                                }
                                Employee permEmp = permission.getEmployee();

                                // Check if permission's employee's location is in accessible locations
                                if (permEmp.getLocation() != null && permEmp.getLocation().getLocationId() != null) {
                                    return accessibleLocationIds.contains(permEmp.getLocation().getLocationId());
                                }
                                return false;
                            })
                            .collect(Collectors.toList());

                    log.info("Filtered {} special permissions for employee {} with accessible locations",
                            result.size(), empId);
                } else {
                    // Employee does NOT have special permission flag: Load only their own
                    // permissions
                    result = repository.findByEmployee_EmployeeId(empId);
                    log.info("Fetching special permissions for employee {} only (flag=false)", empId);
                }
            }

            return JsonResponse.of(true, result, "Special permissions fetched successfully");
        } catch (Exception e) {
            log.error("Failed to fetch special permissions for employee", e);
            return JsonResponse.of(false, null, "Failed to fetch special permissions: " + e.getMessage());
        }
    }

    public JsonResponse getAllSpecialPermissions() {
        try {
            log.info("Fetching all special permissions (SUPER_ADMIN)");
            List<AnswerSubmissionTimeExtension> all = repository.findAll();
            return JsonResponse.of(true, all, "All special permissions fetched successfully");
        } catch (Exception e) {
            log.error("Failed to fetch all special permissions", e);
            return JsonResponse.of(false, null, "Failed to fetch special permissions: " + e.getMessage());
        }
    }

    public JsonResponse deleteSpecialPermission(Integer extensionId) {
        try {
            if (extensionId == null) {
                return JsonResponse.of(false, null, "extensionId is required for delete");
            }

            Optional<AnswerSubmissionTimeExtension> existingOpt = repository.findById(extensionId);
            if (existingOpt.isEmpty()) {
                return JsonResponse.of(false, null, "Special permission not found for delete");
            }

            repository.deleteById(extensionId);
            return JsonResponse.of(true, null, "Special permission deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete special permission with id={}", extensionId, e);
            return JsonResponse.of(false, null, "Failed to delete special permission: " + e.getMessage());
        }
    }
}
