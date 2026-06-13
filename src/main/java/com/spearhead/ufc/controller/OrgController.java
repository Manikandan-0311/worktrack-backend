package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.service.EmployeeService;
import com.spearhead.ufc.service.OrgService;
import com.spearhead.ufc.utils.AuthUtil;
import com.spearhead.ufc.dto.CalendarDTO;
import com.spearhead.ufc.model.Calendar;
import com.spearhead.ufc.repository.CalendarRepository;
import com.spearhead.ufc.repository.OrgRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/orgController")
@CrossOrigin(origins = "*")
public class OrgController {
    private static final Logger log = LoggerFactory.getLogger(OrgController.class);

    @Autowired
    private OrgService orgService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/organizationList")
    public ResponseEntity<?> getOrganizationList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Request: getOrganizationList received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in getOrganizationList");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in getOrganizationList");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }
            log.debug("Employee {} authenticated for getOrganizationList", employee.getEmployeeId());

            // Check if employee is SUPER_ADMIN
            Role role = employee.getRoleId();
            String roleName = role != null ? role.getRoleName() : null;
            boolean isSuperAdmin = false;
            if (roleName != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(roleName) || roleName.toUpperCase().contains("SUPER")) {
                    isSuperAdmin = true;
                }
            }

            List<Org> orgs;
            if (isSuperAdmin) {
                orgs = orgService.getAllOrganizations();
                if (orgs != null) {
                    for (Org e : orgs) {
                        if (e.getCreatedBy() != null) {
                            e.setCreatedEmployeeName(employeeService.getEmployeeFullName(e.getCreatedBy()));
                        }
                        if (e.getUpdatedBy() != null) {
                            e.setUpdatedEmployeeName(employeeService.getEmployeeFullName(e.getUpdatedBy()));
                        }
                    }
                }
                log.info("SUPER_ADMIN {} retrieved all {} organizations", employee.getEmployeeId(), orgs.size());
            } else {
                Org userOrg = employee.getOrg();
                if (userOrg == null) {
                    log.warn("Employee {} has no organization assigned", employee.getEmployeeId());
                    orgs = new java.util.ArrayList<>();
                } else {
                    orgs = java.util.Arrays.asList(userOrg);
                    for (Org e : orgs) {
                        if (e.getCreatedBy() != null) {
                            e.setCreatedEmployeeName(employeeService.getEmployeeFullName(e.getCreatedBy()));
                        }
                        if (e.getUpdatedBy() != null) {
                            e.setUpdatedEmployeeName(employeeService.getEmployeeFullName(e.getUpdatedBy()));
                        }
                    }
                    log.info("Employee {} retrieved their organization: {}", employee.getEmployeeId(),
                            userOrg.getOrgId());
                }
            }

            return ResponseEntity.ok(JsonResponse.of(true, orgs, "Organization list loaded successfully"));
        } catch (Exception e) {
            log.error("Unexpected error in getOrganizationList: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load organization list: " + e.getMessage()));
        }
    }

    @GetMapping("/organizationListWithInactive")
    public ResponseEntity<?> getOrganizationListWithInactive(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Request: getOrganizationListWithInactive received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }

            Role role = employee.getRoleId();
            String roleName = role != null ? role.getRoleName() : null;
            boolean isSuperAdmin = false;
            if (roleName != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(roleName) || roleName.toUpperCase().contains("SUPER")) {
                    isSuperAdmin = true;
                }
            }

            List<Org> orgs;
            if (isSuperAdmin) {
                orgs = orgService.getAllOrganizations(); // includes active + inactive
                if (orgs != null) {
                    for (Org e : orgs) {
                        if (e.getCreatedBy() != null) {
                            e.setCreatedEmployeeName(employeeService.getEmployeeFullName(e.getCreatedBy()));
                        }
                        if (e.getUpdatedBy() != null) {
                            e.setUpdatedEmployeeName(employeeService.getEmployeeFullName(e.getUpdatedBy()));
                        }
                    }
                }
            } else {
                Org userOrg = employee.getOrg();
                if (userOrg == null) {
                    orgs = new java.util.ArrayList<>();
                } else {
                    orgs = java.util.Arrays.asList(userOrg);
                    for (Org e : orgs) {
                        if (e.getCreatedBy() != null) {
                            e.setCreatedEmployeeName(employeeService.getEmployeeFullName(e.getCreatedBy()));
                        }
                        if (e.getUpdatedBy() != null) {
                            e.setUpdatedEmployeeName(employeeService.getEmployeeFullName(e.getUpdatedBy()));
                        }
                    }
                }
            }

            if (orgs != null && !orgs.isEmpty()) {
                orgs = orgs.stream()
                        .sorted((a, b) -> {
                            OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt() : a.getCreatedDt();
                            OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt() : b.getCreatedDt();
                            if (aTime == null && bTime == null) {
                                return 0;
                            }
                            if (aTime == null) {
                                return 1;
                            }
                            if (bTime == null) {
                                return -1;
                            }
                            return bTime.compareTo(aTime);
                        })
                        .toList();
            }

            return ResponseEntity.ok(JsonResponse.of(true, orgs, "Organization list loaded successfully"));
        } catch (Exception e) {
            log.error("Unexpected error in getOrganizationListWithInactive: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to load organization list: " + e.getMessage()));
        }
    }

    @PostMapping("/calendar")
    public ResponseEntity<?> upsertCalendarEntries(
            @RequestParam Integer orgId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody List<CalendarDTO> payload) {
        log.info("Request: upsertCalendarEntries received for orgId={} with {} entries", orgId,
                payload == null ? 0 : payload.size());
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Authorization header is required"));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token"));
            }

            if (orgId == null || !orgRepository.existsById(orgId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Invalid orgId"));
            }

            List<Calendar> saved = new java.util.ArrayList<>();
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

            if (payload != null) {
                for (CalendarDTO item : payload) {
                    if (item.getDate() == null)
                        continue;
                    java.util.Optional<Calendar> existingOpt = calendarRepository.findByOrgOrgIdAndCalendarDate(orgId,
                            item.getDate());
                    if (existingOpt.isPresent()) {
                        Calendar c = existingOpt.get();
                        c.setIsActive(item.getIsActive() != null ? item.getIsActive() : true);
                        c.setRemarks(item.getRemarks());
                        c.setUpdatedBy(employee.getEmployeeId());
                        c.setUpdatedDt(now);
                        saved.add(calendarRepository.save(c));
                    } else {
                        Calendar c = new Calendar();
                        Org o = orgRepository.getReferenceById(orgId);
                        c.setOrg(o);
                        c.setCalendarDate(item.getDate());
                        c.setIsActive(item.getIsActive() != null ? item.getIsActive() : true);
                        c.setRemarks(item.getRemarks());
                        c.setCreatedBy(employee.getEmployeeId());
                        c.setCreatedDt(now);
                        // ensure updated fields are set too (DB expects not-null)
                        c.setUpdatedBy(employee.getEmployeeId());
                        c.setUpdatedDt(now);
                        saved.add(calendarRepository.save(c));
                    }
                }
            }

            return ResponseEntity.ok(JsonResponse.of(true, saved, "Calendar entries saved"));
        } catch (Exception e) {
            log.error("Error in upsertCalendarEntries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to save calendar entries: " + e.getMessage()));
        }
    }

    @GetMapping("/calendarByMonth")
    public ResponseEntity<?> getCalendarByMonth(
            @RequestParam Integer orgId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        log.info("Request: getCalendarByMonth received for orgId={}, year={}, month={}", orgId, year, month);
        try {
            if (orgId == null || year == null || month == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "orgId, year and month are required"));
            }
            if (!orgRepository.existsById(orgId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(JsonResponse.of(false, null, "Invalid orgId"));
            }

            List<Calendar> all = calendarRepository.findAllByOrgOrgId(orgId);
            List<Calendar> filtered = new java.util.ArrayList<>();
            for (Calendar c : all) {
                if (c.getCalendarDate() == null)
                    continue;
                if (c.getCalendarDate().getYear() == year && c.getCalendarDate().getMonthValue() == month) {
                    filtered.add(c);
                }
            }

            return ResponseEntity.ok(JsonResponse.of(true, filtered, "Calendar entries fetched"));
        } catch (Exception e) {
            log.error("Error in getCalendarByMonth: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to fetch calendar entries: " + e.getMessage()));
        }
    }

    @PostMapping("/addOrganization")
    public ResponseEntity<?> addOrUpdateOrganization(
            @RequestHeader(value = "Authorization", required = false) String authHeader, @RequestBody Org org) {
        log.info("Request: addOrUpdateOrganization received with Authorization header: {}", authHeader);
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in addOrUpdateOrganization");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null,
                                "Authorization header is required. Please provide a valid token."));
            }
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                log.warn("Unauthorized access attempt: Invalid or expired token in addOrUpdateOrganization");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or expired token. Please login again."));
            }
            log.debug("Employee {} authenticated for addOrUpdateOrganization", employee.getEmployeeId());

            Org result;
            if (org.getOrgId() == null || org.getOrgId() == 0) {
                log.info("Creating new organization: {}", org.getOrgName());
                org.setCreatedBy(employee.getEmployeeId());
                org.setCreatedDt(OffsetDateTime.now());
                result = orgService.addOrganization(org);
                log.info("Organization created successfully with ID: {}", result.getOrgId());
                return ResponseEntity.ok(JsonResponse.of(true, result, "Organization added successfully"));
            } else {
                log.info("Updating organization with ID: {}", org.getOrgId());
                org.setUpdatedBy(employee.getEmployeeId());
                org.setUpdatedDt(OffsetDateTime.now());
                result = orgService.updateOrganization(org);
                log.info("Organization updated successfully with ID: {}", result.getOrgId());
                return ResponseEntity.ok(JsonResponse.of(true, result, "Organization updated successfully"));
            }
        } catch (IllegalArgumentException e) {
            log.error("Validation error in addOrUpdateOrganization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsonResponse.of(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in addOrUpdateOrganization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, null, "Failed to add/update organization: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<JsonResponse> uploadOrganizationLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("orgId") Integer orgId,
            @RequestHeader("Authorization") String authToken) {
        log.info("Entered into uploadOrganizationLogo - OrgController");
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(JsonResponse.of(false, null, "File is empty"));
            }

            Employee user = authUtil.getEmployeeFromToken(authToken);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or missing auth token"));
            }

            if (orgId == null) {
                return ResponseEntity.badRequest().body(JsonResponse.of(false, null, "orgId is required"));
            }

            Org org = orgRepository.findById(orgId).orElse(null);
            if (org == null) {
                return ResponseEntity.badRequest().body(JsonResponse.of(false, null, "Organization not found"));
            }

            log.info("uploadOrganizationLogo orgId={}, fileName={}, size={}",
                    orgId, file.getOriginalFilename(), file.getSize());

            String directoryValue;
            if (new File("E:/").exists()) {
                directoryValue = "E:/UFC/" + orgId + "/ORG_DETAILS/";
            } else if (new File("D:/").exists()) {
                directoryValue = "D:/UFC/" + orgId + "/ORG_DETAILS/";
            } else {
                directoryValue = "C:/UFC/" + orgId + "/ORG_DETAILS/";
            }

            File writeFileDir = new File(directoryValue);
            if (!writeFileDir.exists()) {
                writeFileDir.mkdirs();
            }

            String fileName = file.getOriginalFilename();
            File writeFile = new File(writeFileDir, fileName);
            log.info("uploadOrganizationLogo writeFile={}", writeFile.getAbsolutePath());
            file.transferTo(writeFile);
            log.info("uploadOrganizationLogo saved exists={}, length={}",
                    writeFile.exists(), writeFile.length());

            org.setLogo(fileName);
            orgRepository.save(org);

            Map<String, String> responseData = new HashMap<>();
            responseData.put("logoUrl", fileName);
            return ResponseEntity.ok(JsonResponse.of(true, responseData, "Logo uploaded successfully"));
        } catch (Exception e) {
            log.error("Error in uploadOrganizationLogo - OrgController", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", null);
            errorResponse.put("message", "Server did not accept the logo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, errorResponse, "Upload failed"));
        } finally {
            log.info("Exited from uploadOrganizationLogo - OrgController");
        }
    }
}
