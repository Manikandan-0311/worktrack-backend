/**
 * Author: Somaskandhan
 * Location Controller for managing branch/location operations
 */
package com.spearhead.ufc.controller;

import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.service.LocationService;
import com.spearhead.ufc.utils.AuthUtil;
import com.spearhead.ufc.repository.EmployeeLocationAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/locationController")
@CrossOrigin(origins = "*")
public class LocationController {
        private static final Logger log = LoggerFactory.getLogger(LocationController.class);

        @Autowired
        private LocationService locationService;

        @Autowired
        private AuthUtil authUtil;

        @Autowired
        private EmployeeLocationAccessRepository employeeLocationAccessRepository;

        /**
         * Get all active and inacitve locaitons          * This endpoint is intended for SuperAdmin users to view all locations regardless of organization
          * It will return all locations, but the frontend should filter based on active status and organization if needed
         */


        // @GetMapping("/locationList")
        // public ResponseEntity<?> getLocationsList1(
        //                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        //         log.info("Request: getLocationsList received with Authorization header: {}", authHeader);
        //         try {
        //                 if (authHeader == null || authHeader.trim().isEmpty()) {
        //                         log.warn("Missing Authorization header in getLocationsList");
        //                         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        //                                         .body(JsonResponse.of(false, null,
        //                                                         "Authorization header is required. Please provide a valid token."));
        //                 }

        //                 Employee employee = authUtil.getEmployeeFromToken(authHeader);
        //                 if (employee == null) {
        //                         log.warn("Unauthorized access attempt: Invalid or expired token in getLocationsList");
        //                         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        //                                         .body(JsonResponse.of(false, null,
        //                                                         "Invalid or expired token. Please login again."));
        //                 }

        //                 log.debug("Employee {} authenticated for getLocationsList", employee.getEmployeeId());
        //                 log.debug("Employee role: {}, Organization ID: {}",
        //                                 employee.getRoleId() != null ? employee.getRoleId().getRoleName() : "N/A",
        //                                 employee.getOrg() != null ? employee.getOrg().getOrgId() : "N/A");

        //                 // Check if user is SuperAdmin
        //                 boolean isSuperAdmin = employee.getRoleId() != null &&
        //                                 "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

        //                 List<Location> locations;
        //                 if (isSuperAdmin) {
        //                         log.info("SuperAdmin employee {} requesting all locations", employee.getEmployeeId());
        //                         locations = locationService.getAllLocations();

        //                 } else {
        //                         if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
        //                                 log.warn("Non-admin employee {} has no organization assigned",
        //                                                 employee.getEmployeeId());
        //                                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
        //                                                 .body(JsonResponse.of(false, null,
        //                                                                 "User is not assigned to any organization."));
        //                         }

        //                         // Check if user's organization is active
        //                         if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
        //                                 log.warn("Non-admin employee {} organization is inactive: {}",
        //                                                 employee.getEmployeeId(), employee.getOrg().getOrgId());
        //                                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
        //                                                 .body(JsonResponse.of(false, null,
        //                                                                 "Your organization is inactive. You cannot access location data."));
        //                         }
        //                         locations = locationService.getLocationsByOrgId(employee.getOrg().getOrgId());
        //                 }

        //                 log.info("Successfully retrieved {} locations from active organizations", locations.size());
        //                 return ResponseEntity.ok(JsonResponse.of(true, locations, "Location list loaded successfully"));

        //         } catch (IllegalArgumentException e) {
        //                 log.error("Validation error in getLocationsList: {}", e.getMessage());
        //                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        //                                 .body(JsonResponse.of(false, null, e.getMessage()));
        //         } catch (Exception e) {
        //                 log.error("Unexpected error in getLocationsList: {}", e.getMessage(), e);
        //                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        //                                 .body(JsonResponse.of(false, null,
        //                                                 "Failed to load location list: " + e.getMessage()));
        //         }
        // }

        /**
         * Get locations list based on user role
         * - SuperAdmin: All locations or filtered by orgId(s)
         * - Other roles: Only locations of user's organization
         * @param orgId - Optional org ID(s) as comma-separated values (e.g., 1 or 1,2,3)
         */
        @GetMapping("/locationsList")
        public ResponseEntity<?> getLocationsList(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestParam(value = "orgId", required = false) String orgId) {
                log.info("Request: getLocationsList received with Authorization header: {}, orgId: {}", authHeader, orgId);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in getLocationsList");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in getLocationsList");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }

                        log.debug("Employee {} authenticated for getLocationsList", employee.getEmployeeId());
                        log.debug("Employee role: {}, Organization ID: {}",
                                        employee.getRoleId() != null ? employee.getRoleId().getRoleName() : "N/A",
                                        employee.getOrg() != null ? employee.getOrg().getOrgId() : "N/A");

                        // Check if user is SuperAdmin
                        boolean isSuperAdmin = employee.getRoleId() != null &&
                                        "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

                        // Parse orgId parameter (can be single value or comma-separated)
                        Set<Integer> orgIdSet = parseOrgIds(orgId);

                        List<Location> locations;
                        if (isSuperAdmin) {
                                log.info("SuperAdmin employee {} requesting locations", employee.getEmployeeId());
                                
                                if (orgIdSet.isEmpty()) {
                                        // No orgId filter - get all locations
                                        locations = locationService.getAllLocations();
                                        log.info("SuperAdmin fetching all locations");
                                } else {
                                        // Filter by provided orgId(s)
                                        locations = locationService.getAllLocations();
                                        locations = locations.stream()
                                                        .filter(loc -> loc.getOrg() != null 
                                                                        && loc.getOrg().getOrgId() != null
                                                                        && orgIdSet.contains(loc.getOrg().getOrgId()))
                                                        .toList();
                                        log.info("SuperAdmin filtered locations by orgIds {}: {} results", orgIdSet, locations.size());
                                }
                        } else {
                                if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
                                        log.warn("Non-admin employee {} has no organization assigned",
                                                        employee.getEmployeeId());
                                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                        .body(JsonResponse.of(false, null,
                                                                        "User is not assigned to any organization."));
                                }

                                // Check if user's organization is active
                                if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
                                        log.warn("Non-admin employee {} organization is inactive: {}",
                                                        employee.getEmployeeId(), employee.getOrg().getOrgId());
                                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                        .body(JsonResponse.of(false, null,
                                                                        "Your organization is inactive. You cannot access location data."));
                                }

                                // Non-admin users always use their own org ID from token (ignore orgId param)
                                log.info("Regular employee {} requesting locations for active organization: {}",
                                                employee.getEmployeeId(), employee.getOrg().getOrgId());
                                locations = locationService.getLocationsByOrgId(employee.getOrg().getOrgId());
                        }

                        log.info("Successfully retrieved {} locations from active organizations", locations.size());
                        return ResponseEntity.ok(JsonResponse.of(true, locations, "Location list loaded successfully"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error in getLocationsList: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(JsonResponse.of(false, null, e.getMessage()));
                } catch (Exception e) {
                        log.error("Unexpected error in getLocationsList: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to load location list: " + e.getMessage()));
                }
        }

        /**
         * Get locations list (including active and inactive) based on user role
         * - SuperAdmin: All locations or filtered by orgId(s)
         * - Other roles: Only locations of user's organization
         * - Returns latest updated records first (fallback: created date)
         */
        @GetMapping("/locationsListWithInactive")
        public ResponseEntity<?> getLocationsListWithInactive(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestParam(value = "orgId", required = false) String orgId) {
                log.info("Request: getLocationsListWithInactive received with Authorization header: {}, orgId: {}",
                                authHeader, orgId);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }

                        boolean isSuperAdmin = employee.getRoleId() != null &&
                                        "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());
                        Set<Integer> orgIdSet = parseOrgIds(orgId);

                        List<Location> locations;
                        if (isSuperAdmin) {
                                if (orgIdSet.isEmpty()) {
                                        locations = locationService.getAllLocationsIncludingInactive();
                                } else {
                                        locations = locationService.getAllLocationsIncludingInactive();
                                        locations = locations.stream()
                                                        .filter(loc -> loc.getOrg() != null
                                                                        && loc.getOrg().getOrgId() != null
                                                                        && orgIdSet.contains(loc.getOrg().getOrgId()))
                                                        .toList();
                                }
                        } else {
                                if (employee.getOrg() == null || employee.getOrg().getOrgId() == null) {
                                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                        .body(JsonResponse.of(false, null,
                                                                        "User is not assigned to any organization."));
                                }
                                if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
                                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                        .body(JsonResponse.of(false, null,
                                                                        "Your organization is inactive. You cannot access location data."));
                                }
                                locations = locationService
                                                .getLocationsByOrgIdIncludingInactive(employee.getOrg().getOrgId());
                        }

                        locations = locations.stream()
                                        .sorted((a, b) -> {
                                                OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt()
                                                                : a.getCreatedDt();
                                                OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt()
                                                                : b.getCreatedDt();
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

                        return ResponseEntity.ok(JsonResponse.of(true, locations, "Location list loaded successfully"));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(JsonResponse.of(false, null, e.getMessage()));
                } catch (Exception e) {
                        log.error("Unexpected error in getLocationsListWithInactive: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to load location list: " + e.getMessage()));
                }
        }

        /**
         * Get branches by organization ID
         * Simple endpoint that returns all branches for a specific org
         */
        @GetMapping("/branchesByOrg")
        public ResponseEntity<?> getBranchesByOrg(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestParam(value = "orgId", required = true) Integer orgId) {
                log.info("Request: getBranchesByOrg received with orgId: {}", orgId);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in getBranchesByOrg");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in getBranchesByOrg");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }

                        if (orgId == null || orgId <= 0) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(JsonResponse.of(false, null, "Valid orgId is required"));
                        }

                        List<Location> locations = locationService.getLocationsByOrgId(orgId);
                        log.info("Successfully retrieved {} branches for orgId: {}", locations.size(), orgId);
                        return ResponseEntity.ok(JsonResponse.of(true, locations, "Branches loaded successfully"));

                } catch (Exception e) {
                        log.error("Unexpected error in getBranchesByOrg: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to load branches: " + e.getMessage()));
                }
        }

        /**
         * Get a specific location by ID
         */
        @GetMapping("/location/{locationId}")
        public ResponseEntity<?> getLocationById(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @PathVariable Integer locationId) {
                log.info("Request: getLocationById received for locationId: {} with Authorization header: {}",
                                locationId, authHeader);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in getLocationById");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in getLocationById");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }
                        log.debug("Employee {} authenticated for getLocationById", employee.getEmployeeId());

                        Location location = locationService.getLocationById(locationId);
                        if (location == null) {
                                log.warn("Location not found with ID: {}", locationId);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(JsonResponse.of(false, null,
                                                                "Location not found with ID: " + locationId));
                        }

                        // Check if user is SuperAdmin or owns this location's organization
                        boolean isSuperAdmin = employee.getRoleId() != null &&
                                        "superadmin".equalsIgnoreCase(employee.getRoleId().getRoleName());

                        if (!isSuperAdmin && (employee.getOrg() == null ||
                                        !employee.getOrg().getOrgId().equals(location.getOrg().getOrgId()))) {
                                log.warn("Employee {} attempted to access location {} from different organization",
                                                employee.getEmployeeId(), locationId);
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(JsonResponse.of(false, null,
                                                                "You don't have access to this location."));
                        }

                        log.info("Successfully retrieved location with ID: {}", locationId);
                        return ResponseEntity
                                        .ok(JsonResponse.of(true, location, "Location details loaded successfully"));

                } catch (Exception e) {
                        log.error("Unexpected error in getLocationById: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to load location: " + e.getMessage()));
                }
        }

        /**
         * Add or update a location
         * Sets createdBy/createdDt for new locations
         * Sets updatedBy/updatedDt for updates
         */
        @PostMapping("/addLocation")
        public ResponseEntity<?> addOrUpdateLocation(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestBody Location location) {
                log.info("Request: addOrUpdateLocation received with Authorization header: {}", authHeader);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in addOrUpdateLocation");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in addOrUpdateLocation");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }
                        log.debug("Employee {} authenticated for addOrUpdateLocation", employee.getEmployeeId());

                        // Check if user is SuperAdmin or owns this location's organization
                        boolean isSuperAdmin = employee.getRoleId() != null &&
                                        "SUPER_ADMIN".equalsIgnoreCase(employee.getRoleId().getRoleName());

                        if (!isSuperAdmin && (employee.getOrg() == null )) {
                                log.warn("Employee {} attempted to modify location in different organization",
                                                employee.getEmployeeId());
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(JsonResponse.of(false, null,
                                                                "You don't have permission to modify locations in this organization."));
                        }

                        Location result;
                        if (location.getLocationId() == null || location.getLocationId() == 0) {
                                log.info("Creating new location: {} in organization: {}",
                                                location.getLocationName(), location.getOrg().getOrgId());
                                location.setCreatedBy(employee.getEmployeeId());
                                location.setCreatedDt(OffsetDateTime.now());
                                result = locationService.addLocation(location);
                                log.info("Location created successfully with ID: {}", result.getLocationId());
                                return ResponseEntity.ok(JsonResponse.of(true, result, "Location added successfully"));
                        } else {
                                log.info("Updating location with ID: {}", location.getLocationId());
                                location.setUpdatedBy(employee.getEmployeeId());
                                location.setUpdatedDt(OffsetDateTime.now());
                                result = locationService.updateLocation(location);
                                log.info("Location updated successfully with ID: {}", result.getLocationId());
                                return ResponseEntity
                                                .ok(JsonResponse.of(true, result, "Location updated successfully"));
                        }

                } catch (IllegalArgumentException e) {
                        log.error("Validation error in addOrUpdateLocation: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(JsonResponse.of(false, null, e.getMessage()));
                } catch (Exception e) {
                        log.error("Unexpected error in addOrUpdateLocation: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to add/update location: " + e.getMessage()));
                }
        }

        /**
         * Delete a location (soft delete by setting isActive to false)
         */
        @DeleteMapping("/location/{locationId}")
        public ResponseEntity<?> deleteLocation(
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        @PathVariable Integer locationId) {
                log.info("Request: deleteLocation received for locationId: {} with Authorization header: {}",
                                locationId, authHeader);
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in deleteLocation");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in deleteLocation");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }
                        log.debug("Employee {} authenticated for deleteLocation", employee.getEmployeeId());

                        Location location = locationService.getLocationById(locationId);
                        if (location == null) {
                                log.warn("Location not found with ID: {}", locationId);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(JsonResponse.of(false, null,
                                                                "Location not found with ID: " + locationId));
                        }

                        // Check if user is SuperAdmin or owns this location's organization
                        boolean isSuperAdmin = employee.getRoleId() != null &&
                                        "superadmin".equalsIgnoreCase(employee.getRoleId().getRoleName());

                        if (!isSuperAdmin && (employee.getOrg() == null ||
                                        !employee.getOrg().getOrgId().equals(location.getOrg().getOrgId()))) {
                                log.warn("Employee {} attempted to delete location {} from different organization",
                                                employee.getEmployeeId(), locationId);
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(JsonResponse.of(false, null,
                                                                "You don't have permission to delete this location."));
                        }

                        log.info("Deleting location with ID: {}", locationId);
                        location.setIsActive(false);
                        location.setUpdatedBy(employee.getEmployeeId());
                        location.setUpdatedDt(OffsetDateTime.now());
                        locationService.updateLocation(location);

                        log.info("Location deleted successfully with ID: {}", locationId);
                        return ResponseEntity.ok(JsonResponse.of(true, null, "Location deleted successfully"));

                } catch (Exception e) {
                        log.error("Unexpected error in deleteLocation: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to delete location: " + e.getMessage()));
                }
        }

        /**
         * Get employee's default branch and accessible branches
         * Takes auth token, retrieves employee details, and returns:
         * - Default branch (from employee.location)
         * - List of all accessible branches for the employee
         */
        @GetMapping("/employee-branch-access")
        public ResponseEntity<?> getEmployeeBranchAccess(
                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
                log.info("Request: getEmployeeBranchAccess received");
                try {
                        if (authHeader == null || authHeader.trim().isEmpty()) {
                                log.warn("Missing Authorization header in getEmployeeBranchAccess");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Authorization header is required. Please provide a valid token."));
                        }

                        Employee employee = authUtil.getEmployeeFromToken(authHeader);
                        if (employee == null) {
                                log.warn("Unauthorized access attempt: Invalid or expired token in getEmployeeBranchAccess");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(JsonResponse.of(false, null,
                                                                "Invalid or expired token. Please login again."));
                        }

                        log.debug("Employee {} authenticated for getEmployeeBranchAccess", employee.getEmployeeId());

                        // Prepare response map
                        Map<String, Object> responseData = new HashMap<>();

                        // Get default branch (location)
                        Location defaultBranch = employee.getLocation();
                        if (defaultBranch != null) {
                                Map<String, Object> defaultBranchData = new HashMap<>();
                                defaultBranchData.put("locationId", defaultBranch.getLocationId());
                                defaultBranchData.put("locationCode", defaultBranch.getLocationCode());
                                defaultBranchData.put("locationName", defaultBranch.getLocationName());
                                defaultBranchData.put("address", defaultBranch.getAddress());
                                defaultBranchData.put("city", defaultBranch.getCity());
                                defaultBranchData.put("state", defaultBranch.getState());
                                responseData.put("defaultBranch", defaultBranchData);
                                log.debug("Default branch set for employee {}: {}", employee.getEmployeeId(),
                                                defaultBranch.getLocationId());
                        } else {
                                responseData.put("defaultBranch", null);
                                log.debug("No default branch assigned for employee {}", employee.getEmployeeId());
                        }

                        // Get all accessible branches for the employee
                        List<EmployeeLocationAccess> locationAccessList = employeeLocationAccessRepository
                                        .findByEmployeeEmployeeIdAndIsActiveTrue(employee.getEmployeeId());

                        List<Map<String, Object>> accessibleBranches = new ArrayList<>();
                        if (locationAccessList != null && !locationAccessList.isEmpty()) {
                                for (EmployeeLocationAccess access : locationAccessList) {
                                        if (access.getLocation() != null) {
                                                Map<String, Object> branchData = new HashMap<>();
                                                branchData.put("locationId", access.getLocation().getLocationId());
                                                branchData.put("locationCode", access.getLocation().getLocationCode());
                                                branchData.put("locationName", access.getLocation().getLocationName());
                                                branchData.put("address", access.getLocation().getAddress());
                                                branchData.put("city", access.getLocation().getCity());
                                                branchData.put("state", access.getLocation().getState());
                                                branchData.put("isActive", access.getIsActive());
                                                accessibleBranches.add(branchData);
                                        }
                                }
                        }

                        responseData.put("accessibleBranches", accessibleBranches);
                        responseData.put("totalAccessibleBranches", accessibleBranches.size());

                        log.info("Successfully retrieved branch access information for employee {}",
                                        employee.getEmployeeId());
                        return ResponseEntity.ok(JsonResponse.of(true, responseData,
                                        "Branch access information loaded successfully"));

                } catch (Exception e) {
                        log.error("Unexpected error in getEmployeeBranchAccess: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(JsonResponse.of(false, null,
                                                        "Failed to retrieve branch access information: "
                                                                        + e.getMessage()));
                }
        }

        /**
         * Parse orgId string which can be single value or comma-separated values
         * @param orgId - e.g., "1" or "1,2,3"
         * @return Set of Integer org IDs (empty set if null/empty/0)
         */
        private Set<Integer> parseOrgIds(String orgId) {
                Set<Integer> result = new HashSet<>();
                if (orgId == null || orgId.trim().isEmpty()) {
                        return result;
                }
                String[] parts = orgId.split(",");
                for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.isEmpty()) {
                                continue;
                        }
                        try {
                                Integer value = Integer.parseInt(trimmed);
                                if (value != 0) {
                                        result.add(value);
                                }
                        } catch (NumberFormatException ex) {
                                log.debug("Ignoring invalid orgId value: {}", trimmed);
                        }
                }
                return result;
        }
}
