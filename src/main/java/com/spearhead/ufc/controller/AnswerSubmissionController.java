package com.spearhead.ufc.controller;

import com.spearhead.ufc.config.JwtUtil;
import com.spearhead.ufc.model.AnswerSubmissionTimeExtension;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.repository.SubmissionRepository;
import com.spearhead.ufc.service.AnswerSubmissionService;
import com.spearhead.ufc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.spearhead.ufc.utils.AuthUtil;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/answer-submission")
public class AnswerSubmissionController {

    private static final Logger log = LoggerFactory.getLogger(AnswerSubmissionController.class);

    private final AnswerSubmissionService service;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuthUtil authUtil;
    private final SubmissionRepository submissionRepository;

    @Autowired
    public AnswerSubmissionController(AnswerSubmissionService service, JwtUtil jwtUtil, UserService userService,
            AuthUtil authUtil, SubmissionRepository submissionRepository) {
        this.authUtil = authUtil;
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/special-permissions")
    public JsonResponse getSpecialPermissions(
            @RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Received request to fetch special permissions list");

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return JsonResponse.of(false, null, "Employee not found");
            }

            // If the logged-in user is SUPER_ADMIN, load all special permissions across
            // orgs
            String roleName = null;
            if (employee.getRoleId() != null) {
                roleName = employee.getRoleId().getRoleName();
            }
            boolean isSuperAdmin = roleName != null && ("SUPER_ADMIN".equalsIgnoreCase(roleName)
                    || roleName.toUpperCase().contains("SUPER"));

            // Non-super-admin users must belong to an active location
            if (!isSuperAdmin && (employee.getLocation() == null || employee.getLocation().getIsActive() == null
                    || !employee.getLocation().getIsActive())) {
                log.warn("Employee {} attempted to access special permissions from inactive location",
                        employee.getEmployeeId());
                return JsonResponse.of(false, null,
                        "Your location is inactive. You cannot access special permissions.");
            }

            if (isSuperAdmin) {
                JsonResponse allPermissions = service.getAllSpecialPermissions();

                if (allPermissions != null && allPermissions.isSuccess() && allPermissions.getResponseData() != null) {
                    List<?> permissionsList = (List<?>) allPermissions.getResponseData();
                    List<AnswerSubmissionTimeExtension> allSortedPermissions = permissionsList.stream()
                            .filter(AnswerSubmissionTimeExtension.class::isInstance)
                            .map(AnswerSubmissionTimeExtension.class::cast)
                            .toList();

                    allSortedPermissions = sortSpecialPermissionsLatestFirst(allSortedPermissions);
                    return JsonResponse.of(true, allSortedPermissions, "Special permissions fetched successfully");
                }

                return allPermissions;
            }

            // Non-super-admin: load special permissions scoped to the employee's org
            JsonResponse response = service.getSpecialPermissionsForEmployee(employee);
            if (response != null && response.isSuccess() && response.getResponseData() != null
                    && response.getResponseData() instanceof List<?>) {
                List<?> raw = (List<?>) response.getResponseData();
                List<AnswerSubmissionTimeExtension> sorted = raw.stream()
                        .filter(AnswerSubmissionTimeExtension.class::isInstance)
                        .map(AnswerSubmissionTimeExtension.class::cast)
                        .toList();
                sorted = sortSpecialPermissionsLatestFirst(sorted);
                return JsonResponse.of(true, sorted, response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Error while fetching special permissions list: {}", e.getMessage(), e);
            return JsonResponse.of(false, null, "An error occurred while processing the request");
        }
    }

    private List<AnswerSubmissionTimeExtension> sortSpecialPermissionsLatestFirst(
            List<AnswerSubmissionTimeExtension> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        return list.stream()
                .sorted((a, b) -> {
                    OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt() : a.getCreatedDt();
                    OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt() : b.getCreatedDt();
                    if (aTime != null && bTime != null) {
                        int cmp = bTime.compareTo(aTime);
                        if (cmp != 0) {
                            return cmp;
                        }
                    } else if (aTime == null && bTime != null) {
                        return 1;
                    } else if (aTime != null) {
                        return -1;
                    }
                    Integer aId = a.getExtensionId();
                    Integer bId = b.getExtensionId();
                    if (aId == null && bId == null) {
                        return 0;
                    }
                    if (aId == null) {
                        return 1;
                    }
                    if (bId == null) {
                        return -1;
                    }
                    return bId.compareTo(aId);
                })
                .toList();
    }

    @PostMapping("/add-special-permission")
    public JsonResponse addSpecialPermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AnswerSubmissionTimeExtension request) {
        try {
            log.info("Received request to add special permission");

            Employee employee = authUtil.getEmployeeFromToken(authHeader);

            if (employee == null) {
                return JsonResponse.of(false, null, "Employee not found");
            }

            // Set createdBy field
            request.setGrantedBy(employee);

            return service.addSpecialPermission(request);
        } catch (Exception e) {
            log.error("Error while adding special permission: {}", e.getMessage(), e);
            return JsonResponse.of(false, null, "An error occurred while processing the request");
        }
    }

    @PostMapping("/update-special-permission")
    public JsonResponse updateSpecialPermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AnswerSubmissionTimeExtension request) {
        try {
            log.info("Received request to update special permission id={}", request.getExtensionId());

            Employee employee = authUtil.getEmployeeFromToken(authHeader);

            if (employee == null) {
                return JsonResponse.of(false, null, "Employee not found");
            }

            if (request.getIsHoliday() == true && request.getIsActive() == true) {
                approveHolidayWork(request, employee.getEmployeeId());
            }

            return service.updateSpecialPermission(request, employee);
        } catch (Exception e) {
            log.error("Error while updating special permission: {}", e.getMessage(), e);
            return JsonResponse.of(false, null, "An error occurred while processing the request");
        }
    }

    @GetMapping("/get-special-permission/{id}")
    public JsonResponse getSpecialPermissionById(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @PathVariable Integer id) {
        try {
            log.info("Received request to fetch special permission with ID: {}", id);

            authUtil.getEmployeeFromToken(authHeader);

            return service.getSpecialPermissionById(id);
        } catch (Exception e) {
            log.error("Error while fetching special permission: {}", e.getMessage(), e);
            return JsonResponse.of(false, null, "An error occurred while processing the request");
        }
    }

    @PostMapping("/delete-special-permission/{extensionId}")
    public JsonResponse deleteSpecialPermission(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer extensionId) {
        try {
            log.info("Received request to delete special permission with ID: {}", extensionId);

            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return JsonResponse.of(false, null, "Employee not found");
            }

            return service.deleteSpecialPermission(extensionId);
        } catch (Exception e) {
            log.error("Error while deleting special permission: {}", e.getMessage(), e);
            return JsonResponse.of(false, null, "An error occurred while processing the request");
        }
    }

    /**
     * Approve holiday work by marking all submissions for that employee on that
     * date as active
     */
    private void approveHolidayWork(AnswerSubmissionTimeExtension request, Integer grantedByEmployeeId) {
        try {

            Integer employeeId = request.getEmployee().getEmployeeId();

            // Find all submissions for this employee on this date
            List<Submission> submissions = submissionRepository.findByEmployeeIdAndQuestionDate(
                    employeeId, request.getQuestionDate());

            if (submissions == null || submissions.isEmpty()) {
                log.info("No submissions found for employee {} on date {}",
                        employeeId, request.getQuestionDate());
                return;
            }

            // Mark all submissions as active
            OffsetDateTime now = OffsetDateTime.now();
            for (Submission submission : submissions) {
                submission.setActive(true);
                submission.setUpdatedDt(now);
                submission.setUpdatedBy(grantedByEmployeeId);
                submissionRepository.save(submission);
                log.info("Marked submission {} as active for holiday work approval", submission.getSubmissionId());
            }

            log.info("Successfully approved holiday work for employee {} on date {} - {} submissions updated",
                    employeeId, request.getQuestionDate(), submissions.size());
        } catch (Exception e) {
            log.error("Error approving holiday work: {}", e.getMessage(), e);
        }
    }
}
