package com.spearhead.ufc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.service.EmployeeService;
import jakarta.persistence.EntityManager;
import com.spearhead.ufc.dto.EmployeeLocationAccessDTO;
import com.spearhead.ufc.dto.EmployeeDTO;
import com.spearhead.ufc.dto.EmployeeGetByIdResponseDTO;
import com.spearhead.ufc.repository.LocationRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.config.JwtUtil;
// import com.spearhead.ufc.service.AuthUtil; // Commented out as AuthUtil is unresolved
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.Department;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/employeeController")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Autowired
    private EntityManager entityManager;

    @GetMapping("/employeelist")
    public ResponseEntity<Map<String, Object>> getEmployees(
            @RequestParam(required = false) Integer orgId,
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        log.info("Entered into getEmployees method - EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            List<Employee> employees = employeeService.getEmployees(orgId, locationId, departmentId, employeeId);

            // Filter employees: Only include employees from active organizations AND active locations
            if (employees != null && !employees.isEmpty()) {
                employees = employees.stream()
                        .filter(emp -> {
                            // Check if organization is active
                            boolean orgActive = emp.getOrg() != null && emp.getOrg().getIsActive() != null && emp.getOrg().getIsActive();
                            // Check if location is active
                            boolean locationActive = emp.getLocation() != null && emp.getLocation().getIsActive() != null && emp.getLocation().getIsActive();
                            return orgActive && locationActive;
                        })
                        .toList();
                log.info("Filtered employees from active organizations and locations: {}", employees.size());
            }

            // Populate created/updated employee names for each result
            if (employees != null) {
                for (Employee e : employees) {
                    if (e.getCreatedBy() != null) {
                        e.setCreatedEmployeeName(employeeService.getEmployeeFullName(e.getCreatedBy()));
                    }
                    if (e.getUpdatedBy() != null) {
                        e.setUpdatedEmployeeName(employeeService.getEmployeeFullName(e.getUpdatedBy()));
                    }
                }
            }

            // Latest updated record first (fallback to created date)
            if (employees != null && !employees.isEmpty()) {
                employees = employees.stream()
                        .sorted((a, b) -> {
                            java.time.OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt() : a.getCreatedDt();
                            java.time.OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt() : b.getCreatedDt();
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

            // If pagination params not provided, return full list
            if (page == null && pageSize == null) {
                response.put("success", true);
                response.put("message", "Employees fetched successfully");
                response.put("data", employees);
                return ResponseEntity.ok(response);
            }

            // Validate pagination params
            int p = (page != null && page >= 0) ? page : 0;
            int ps = (pageSize != null && pageSize > 0) ? pageSize : 10;

            int totalRecords = employees != null ? employees.size() : 0;
            int fromIndex = p * ps;
            List<Employee> paged;
            if (employees == null || fromIndex >= totalRecords) {
                paged = List.of();
            } else {
                int toIndex = Math.min(fromIndex + ps, totalRecords);
                paged = employees.subList(fromIndex, toIndex);
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("page", p);
            meta.put("pageSize", ps);
            meta.put("totalRecords", totalRecords);
            meta.put("totalPages", (int) Math.ceil(totalRecords / (double) ps));

            response.put("success", true);
            response.put("message", "Employees fetched successfully");
            response.put("data", paged);
            response.put("meta", meta);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error occurred in getEmployees method - EmployeeController: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to fetch employees: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            log.info("Exited from getEmployees method - EmployeeController");
        }
    }

    @PostMapping("/add")
    public JsonResponse addEmployee(@RequestBody Employee employee,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Entered into addEmployee method - EmployeeController");
        try {
            StringBuilder validationErrors = new StringBuilder();
            if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
                validationErrors.append("Organization is required. ");
            }
            if (employee.getLocation() == null || employee.getLocation().getLocationId() == null) {
                validationErrors.append("Location is required. ");
            }
            if (employee.getRoleId() == null || employee.getRoleId().getRoleId() == null) {
                validationErrors.append("Role is required. ");
            }
            if (employee.getEmployeeCode() == null || employee.getEmployeeCode().trim().isEmpty()) {
                validationErrors.append("Employee Code is required. ");
            }
            if (employee.getUsername() == null || employee.getUsername().trim().isEmpty()) {
                validationErrors.append("Username is required. ");
            }
            if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
                validationErrors.append("First Name is required. ");
            }
            if (employee.getPasswordHash() == null || employee.getPasswordHash().trim().isEmpty()) {
                validationErrors.append("Password is required. ");
            }
            if (validationErrors.length() > 0) {
                return JsonResponse.of(false, null, "Validation Error: " + validationErrors.toString().trim());
            }

            if (employee.getOrg() != null && employee.getOrg().getOrgId() != null &&
                    employee.getLocation() != null && employee.getLocation().getLocationId() != null) {
                if (employeeRepository.existsEmployeeCodeInOrgAndLocation(
                        employee.getOrg().getOrgId(),
                        employee.getLocation().getLocationId(),
                        employee.getEmployeeCode())) {
                    return JsonResponse.of(false, null, "Staff Code '" + employee.getEmployeeCode()
                            + "' already exists for this organization and Branch");
                }
            }

            // Extract location access list if provided
            List<EmployeeLocationAccess> locationAccessList = null;
            if (employee.getEmployeeLocationAccess() != null && !employee.getEmployeeLocationAccess().isEmpty()) {
                locationAccessList = employee.getEmployeeLocationAccess();
                for (EmployeeLocationAccess ela : locationAccessList) {
                    if (ela.getLocation() == null || ela.getLocation().getLocationId() == null) {
                        return JsonResponse.of(false, null,
                                "Validation Error: Employee location access must have a valid location ID");
                    }
                    if (!locationRepository.existsById(ela.getLocation().getLocationId())) {
                        return JsonResponse.of(false, null,
                                "Location ID " + ela.getLocation().getLocationId() + " does not exist");
                    }
                }
            }

            // Determine acting employee from auth token
            Employee actingUser = authUtil.getEmployeeFromToken(authHeader);
            Integer actingEmployeeId = (actingUser != null) ? actingUser.getEmployeeId() : null;

            // Save employee with location access
            Employee saved = employeeService.addEmployee(employee, locationAccessList, actingEmployeeId);
            if (saved != null && saved.getEmployeeId() > 0) {
                // Map to DTO for response
                EmployeeDTO dto = new EmployeeDTO();
                dto.setEmployeeId(saved.getEmployeeId());
                dto.setOrgId(saved.getOrg() != null ? saved.getOrg().getOrgId() : null);
                dto.setLocationId(saved.getLocation() != null ? saved.getLocation().getLocationId() : null);
                dto.setDepartmentId(saved.getDepartment() != null ? saved.getDepartment().getDepartmentId() : null);
                dto.setRoleId(saved.getRoleId() != null ? saved.getRoleId().getRoleId() : null);
                dto.setEmployeeCode(saved.getEmployeeCode());
                dto.setFirstName(saved.getFirstName());
                dto.setLastName(saved.getLastName());
                dto.setEmailId(saved.getEmailId());
                dto.setPhoneNumber(saved.getPhoneNumber());
                dto.setIsActive(saved.getIsActive());
                dto.setJoinDate(saved.getJoinDate());
                dto.setRelieveDate(saved.getRelieveDate());
                dto.setRemarks(saved.getRemarks());
                dto.setLocationName(saved.getLocation() != null ? saved.getLocation().getLocationName() : null);
                dto.setDepartmentName(saved.getDepartment() != null ? saved.getDepartment().getDepartmentName() : null);
                dto.setRoleName(saved.getRoleId() != null ? saved.getRoleId().getRoleName() : null);
                dto.setSpecialPermissionFlag(saved.getSpecialPermissionFlag());
                return JsonResponse.of(true, dto, "Employee added successfully");
            } else {
                return JsonResponse.of(false, null, "Unable to add employee");
            }
        } catch (IllegalArgumentException e) {
            log.error("Validation error in addEmployee: {}", e.getMessage());
            return JsonResponse.of(false, null, e.getMessage());
        } catch (Exception e) {
            log.error("Error occurred in addEmployee method - EmployeeController: {}", e.getMessage(), e);
            String userMessage = "Unable to add employee";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("role_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Role is required and must be a valid role";
                } else if (e.getMessage().contains("org_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Organization is required and must be a valid organization";
                } else if (e.getMessage().contains("location_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Location is required and must be a valid location";
                } else if (e.getMessage().contains("employee_code") && e.getMessage().contains("unique")) {
                    userMessage = "Employee Code already exists in this organization";
                } else if (e.getMessage().contains("email_id") && e.getMessage().contains("unique")) {
                    userMessage = "Email already exists in the system";
                } else if (e.getMessage().contains("username") && e.getMessage().contains("unique")) {
                    userMessage = "Username already exists in the system";
                } else {
                    userMessage = "Unable to add employee: " + e.getMessage();
                }
            }
            return JsonResponse.of(false, null, userMessage);
        } finally {
            log.info("Exited from addEmployee method - EmployeeController");
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateEmployee(@RequestBody Employee employee,
                                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Entered into updateEmployee method - EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate required fields
            StringBuilder validationErrors = new StringBuilder();

            // if (employee.getEmployeeId() == null || employee.getEmployeeId() <= 0) {
            //     validationErrors.append("Employee ID is required. ");
            // }
            // if (employee.getEmployeeCode() == null || employee.getEmployeeCode().trim().isEmpty()) {
            //     validationErrors.append("Employee Code is required. ");
            // }
            // if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
            //     validationErrors.append("First Name is required. ");
            // }

            if (validationErrors.length() > 0) {
                response.put("success", false);
                response.put("message", "Validation Error: " + validationErrors.toString().trim());
                response.put("data", null);
                return ResponseEntity.badRequest().body(response);
            }

            // (excluding current employee)
            if (employee.getOrg() != null && employee.getOrg().getOrgId() != null &&
                    employee.getLocation() != null && employee.getLocation().getLocationId() != null) {
                if (employeeRepository.existsEmployeeCodeInOrgAndLocationExcludingId(
                        employee.getOrg().getOrgId(),
                        employee.getLocation().getLocationId(),
                        employee.getEmployeeCode(),
                        employee.getEmployeeId())) {
                    response.put("success", false);
                    response.put("message", "Staff Code '" + employee.getEmployeeCode()
                            + "' already exists for this organization and Branch");
                    response.put("data", null);
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Extract location access list if provided
            List<EmployeeLocationAccess> locationAccessList = null;
            if (employee.getEmployeeLocationAccess() != null && !employee.getEmployeeLocationAccess().isEmpty()) {
                locationAccessList = employee.getEmployeeLocationAccess();

                // Validate that all locations in employeeLocationAccess exist
                for (EmployeeLocationAccess ela : locationAccessList) {
                    if (ela.getLocation() == null || ela.getLocation().getLocationId() == null) {
                        response.put("success", false);
                        response.put("message",
                                "Validation Error: Employee location access must have a valid location ID");
                        response.put("data", null);
                        return ResponseEntity.badRequest().body(response);
                    }
                    // Check if location exists in database
                    if (!locationRepository.existsById(ela.getLocation().getLocationId())) {
                        response.put("success", false);
                        response.put("message", "Location ID " + ela.getLocation().getLocationId() + " does not exist");
                        response.put("data", null);
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            }

            // Determine acting employee from auth token
            Employee actingUser = authUtil.getEmployeeFromToken(authHeader);
            Integer actingEmployeeId = (actingUser != null) ? actingUser.getEmployeeId() : null;

            // Update employee with location access
            Employee updated = employeeService.updateEmployee(employee, locationAccessList, actingEmployeeId);
            if (updated != null && updated.getEmployeeId() > 0) {
                response.put("success", true);
                response.put("message", "Employee updated successfully");
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "Unable to update employee");
                response.put("data", null);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error in updateEmployee: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error occurred in updateEmployee method - EmployeeController", e.getMessage(), e);
            String userMessage = "Unable to update employee";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("role_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Role is required and must be a valid role";
                } else if (e.getMessage().contains("org_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Organization is required and must be a valid organization";
                } else if (e.getMessage().contains("location_id") && e.getMessage().contains("not-null")) {
                    userMessage = "Branch is required and must be a valid Branch";
                } else if (e.getMessage().contains("employee_code") && e.getMessage().contains("unique")) {
                    userMessage = "Staff Code already exists in this organization";
                } else if (e.getMessage().contains("email_id") && e.getMessage().contains("unique")) {
                    userMessage = "Email already exists in the system";
                } else {
                    userMessage = "Unable to update Staff: " + e.getMessage();
                }
            }
            response.put("success", false);
            response.put("message", userMessage);
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            log.info("Exited from updateEmployee method - EmployeeController");
        }
    }

    // DTO-based update endpoint to accept flat IDs and return JsonResponse
    @PostMapping("/update-dto")
    public JsonResponse updateEmployeeDto(@RequestBody EmployeeDTO dto,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Entered into updateEmployeeDto - EmployeeController");
        try {
            // Basic validation
            if (dto.getEmployeeId() == null || dto.getEmployeeId() <= 0) {
                return JsonResponse.of(false, null, "Validation Error: Employee ID is required");
            }
            if (dto.getEmployeeCode() == null || dto.getEmployeeCode().trim().isEmpty()) {
                return JsonResponse.of(false, null, "Validation Error: Employee Code is required");
            }
            if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
                return JsonResponse.of(false, null, "Validation Error: First Name is required");
            }

            // Map DTO -> Entity (id-only references for relations)
            Employee emp = new Employee();
            emp.setEmployeeId(dto.getEmployeeId());
            emp.setEmployeeCode(dto.getEmployeeCode());
            emp.setFirstName(dto.getFirstName());
            emp.setLastName(dto.getLastName());
            emp.setEmailId(dto.getEmailId());
            emp.setPhoneNumber(dto.getPhoneNumber());
            emp.setIsActive(dto.getIsActive());
            emp.setJoinDate(dto.getJoinDate());
            emp.setRelieveDate(dto.getRelieveDate());
            emp.setRemarks(dto.getRemarks());

            if (dto.getOrgId() != null) {
                Org o = new Org();
                o.setOrgId(dto.getOrgId());
                emp.setOrg(o);
            }
            if (dto.getLocationId() != null) {
                Location l = new Location();
                l.setLocationId(dto.getLocationId());
                emp.setLocation(l);
            }
            if (dto.getDepartmentId() != null) {
                Department d = new Department();
                d.setDepartmentId(dto.getDepartmentId());
                emp.setDepartment(d);
            }
            if (dto.getRoleId() != null) {
                Role r = new Role();
                r.setRoleId(dto.getRoleId());
                emp.setRoleId(r);
            }

            // Determine acting employee from auth token
            Employee actingUser = authUtil.getEmployeeFromToken(authHeader);
            Integer actingEmployeeId = (actingUser != null) ? actingUser.getEmployeeId() : null;

            Employee updated = employeeService.updateEmployee(emp, null, actingEmployeeId);
            if (updated != null && updated.getEmployeeId() != null) {
                return JsonResponse.of(true, updated, "Employee updated successfully");
            }
            return JsonResponse.of(false, null, "Unable to update employee");
        } catch (IllegalArgumentException ex) {
            log.error("Validation error in updateEmployeeDto: {}", ex.getMessage());
            return JsonResponse.of(false, null, ex.getMessage());
        } catch (Exception e) {
            log.error("Error occurred in updateEmployeeDto - EmployeeController", e);
            return JsonResponse.of(false, null, "Unable to update employee: " + e.getMessage());
        } finally {
            log.info("Exited from updateEmployeeDto - EmployeeController");
        }
    }

    @PostMapping("/employeeDashboard")
    public ResponseEntity<Map<String, Object>> employeeDashboard(@RequestBody Submission submission) {
        log.info("Enter into employeeDashboard method -EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            List<Submission> data = employeeService.employeeDashboard(submission);
            if (submission != null) {
                response.put("message", "Employee Summary data fetched Successfully");
                response.put("success", true);
                response.put("data", data);
            } else {
                response.put("message", "Employee Summary data fetched Error");
                response.put("success", false);
                response.put("data", null);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error Occured into the employeeDashboard method -EmployeeController", e);
            response.put("message", "Employee Summary data fetched Error");
            response.put("success", false);
            response.put("data", null);
            return ResponseEntity.ok(response);
        } finally {
            log.info("Exited from employeeDashboard method - EmployeeController");
        }
    }

    @PostMapping("/{employeeId}/location-access")
    public ResponseEntity<Map<String, Object>> saveEmployeeLocationAccess(
            @PathVariable Integer employeeId,
            @RequestBody EmployeeLocationAccessDTO dto) {
        log.info("Entered into saveEmployeeLocationAccess - EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            employeeService.saveEmployeeLocationAccess(
                    employeeId,
                    dto.getOrgId(),
                    dto.getDefaultLocationId(),
                    dto.getEnabledLocationIds(),
                    null);
            response.put("success", true);
            response.put("message", "Employee location access saved successfully");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error occurred in saveEmployeeLocationAccess - EmployeeController", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            log.info("Exited from saveEmployeeLocationAccess - EmployeeController");
        }
    }

    @GetMapping("/getById")
    public ResponseEntity<Map<String, Object>> getEmployeeById(@RequestParam Integer employeeId) {
        log.info("Entered into getEmployeeById - EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            EmployeeGetByIdResponseDTO data = employeeService.getEmployeeByIdWithAccess(employeeId);
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getEmployeeById - EmployeeController", e);
            response.put("success", false);
            response.put("data", null);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            log.info("Exited from getEmployeeById - EmployeeController");
        }
    }

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getEmployeeProfile(@RequestHeader("Authorization") String authHeader) {
        log.info("Entered into getEmployeeProfile - EmployeeController");
        Map<String, Object> response = new HashMap<>();
        try {
            Employee user = authUtil.getEmployeeFromToken(authHeader);
            
            // Populate created/updated employee names
            if (user != null) {
                if (user.getCreatedBy() != null) {
                    user.setCreatedEmployeeName(employeeService.getEmployeeFullName(user.getCreatedBy()));
                }
                if (user.getUpdatedBy() != null) {
                    user.setUpdatedEmployeeName(employeeService.getEmployeeFullName(user.getUpdatedBy()));
                }
            }
            
            response.put("success", true);
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getEmployeeProfile - EmployeeController", e);
            response.put("success", false);
            response.put("data", null);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            log.info("Exited from getEmployeeProfile - EmployeeController");
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<JsonResponse> uploadEmployeeImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authToken) {
        log.info("Entered into uploadEmployeeImage - EmployeeController");
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(JsonResponse.of(false, null, "File is empty"));
            }

            // Extract employeeId and orgId from authToken
            Employee user = authUtil.getEmployeeFromToken(authToken);
            Integer employeeId = (user != null) ? user.getEmployeeId() : null;
            Integer orgId = (user != null && user.getOrg() != null) ? user.getOrg().getOrgId() : null;

            if (employeeId == null || orgId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsonResponse.of(false, null, "Invalid or missing auth token"));
            }

            // Validate employee existence
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body(JsonResponse.of(false, null, "Employee not found"));
            }

            // Determine directory based on available drives
            String directoryValue;
            if (new File("E:/").exists()) {
                directoryValue = "E:/UFC/" + orgId + "/EMPLOYEE_DETAILS/" + employeeId + "/";
            } else if (new File("D:/").exists()) {
                directoryValue = "D:/UFC/" + orgId + "/EMPLOYEE_DETAILS/" + employeeId + "/";
            } else {
                directoryValue = "C:/UFC/" + orgId + "/EMPLOYEE_DETAILS/" + employeeId + "/";
            }

            // Create directory if it doesn't exist
            File writeFileDir = new File(directoryValue);
            if (!writeFileDir.exists()) {
                writeFileDir.mkdirs();
            }

            // Save the file
            String fileName = file.getOriginalFilename();
            File writeFile = new File(writeFileDir, fileName);
            file.transferTo(writeFile);

            // Update employee profile image path with only the file name
            employee.setProfileImageFilePath(fileName);
            employeeRepository.save(employee);

            // Return success response
            Map<String, String> responseData = new HashMap<>();
            responseData.put("photoUrl", fileName);
            return ResponseEntity.ok(JsonResponse.of(true, responseData, "Image uploaded successfully"));
        } catch (Exception e) {
            log.error("Error in uploadEmployeeImage - EmployeeController", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", null);
            errorResponse.put("message", "Server did not accept the image");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsonResponse.of(false, errorResponse, "Upload failed"));
        } finally {
            log.info("Exited from uploadEmployeeImage - EmployeeController");
        }
    }

}
