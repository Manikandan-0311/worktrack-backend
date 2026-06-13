/**
 * Author: Somaskandhan
 * Location Service for managing location business logic
 */
package com.spearhead.ufc.service;

import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.repository.LocationRepository;
import com.spearhead.ufc.repository.OrgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrgRepository orgRepository;

    /**
     * Get all active locations
     */
    public List<Location> getAllLocations() {
        log.info("Fetching all active locations");
        try {
            List<Location> locations = locationRepository.findByIsActiveTrue();
            // Populate updatedEmployeeName for each location based on updatedBy field
            for (Location location : locations) {
                populateUpdatedEmployeeName(location);
            }
            log.info("Retrieved {} active locations", locations.size());
            return locations;
        } catch (Exception e) {
            log.error("Error fetching all locations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all locations: " + e.getMessage());
        }
    }

    /**
     * Get all locations including active and inactive
     */
    public List<Location> getAllLocationsIncludingInactive() {
        log.info("Fetching all locations including inactive");
        try {
            List<Location> locations = locationRepository.findAll();
            for (Location location : locations) {
                populateUpdatedEmployeeName(location);
            }
            log.info("Retrieved {} total locations", locations.size());
            return locations;
        } catch (Exception e) {
            log.error("Error fetching all locations including inactive: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all locations: " + e.getMessage());
        }
    }

    /**
     * Get locations by organization ID
     */
    public List<Location> getLocationsByOrgId(Integer orgId) {
        log.info("Fetching locations for organization ID: {}", orgId);
        try {
            if (orgId == null || orgId <= 0) {
                log.error("Invalid organization ID: {}", orgId);
                throw new IllegalArgumentException("Invalid organization ID provided");
            }
            List<Location> locations = locationRepository.findByOrgOrgIdAndIsActiveTrue(orgId);
            // Populate updatedEmployeeName for each location based on updatedBy field
            for (Location location : locations) {
                populateUpdatedEmployeeName(location);
            }
            log.info("Retrieved {} locations for organization ID: {}", locations.size(), orgId);
            return locations;
        } catch (IllegalArgumentException e) {
            log.error("Validation error in getLocationsByOrgId: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching locations for organization {}: {}", orgId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve locations for organization: " + e.getMessage());
        }
    }

    /**
     * Get locations by organization ID including active and inactive
     */
    public List<Location> getLocationsByOrgIdIncludingInactive(Integer orgId) {
        log.info("Fetching all locations (including inactive) for organization ID: {}", orgId);
        try {
            if (orgId == null || orgId <= 0) {
                log.error("Invalid organization ID: {}", orgId);
                throw new IllegalArgumentException("Invalid organization ID provided");
            }
            List<Location> locations = locationRepository.findByOrgOrgId(orgId);
            for (Location location : locations) {
                populateUpdatedEmployeeName(location);
            }
            log.info("Retrieved {} total locations for organization ID: {}", locations.size(), orgId);
            return locations;
        } catch (IllegalArgumentException e) {
            log.error("Validation error in getLocationsByOrgIdIncludingInactive: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching locations (including inactive) for organization {}: {}", orgId, e.getMessage(),
                    e);
            throw new RuntimeException("Failed to retrieve locations for organization: " + e.getMessage());
        }
    }

    /**
     * Get a specific location by ID
     */
    public Location getLocationById(Integer locationId) {
        log.info("Fetching location with ID: {}", locationId);
        try {
            if (locationId == null || locationId <= 0) {
                log.error("Invalid location ID: {}", locationId);
                throw new IllegalArgumentException("Invalid location ID provided");
            }
            Location location = locationRepository.findById(locationId).orElse(null);
            if (location != null) {
                // Populate updatedEmployeeName based on updatedBy field
                populateUpdatedEmployeeName(location);
                log.info("Location found with ID: {}", locationId);
            } else {
                log.warn("Location not found with ID: {}", locationId);
            }
            return location;
        } catch (IllegalArgumentException e) {
            log.error("Validation error in getLocationById: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching location with ID {}: {}", locationId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve location: " + e.getMessage());
        }
    }

    /**
     * Add a new location
     */
    public Location addLocation(Location location) {
        log.info("Adding new location: {} in organization: {}", 
                location.getLocationName(), location.getOrg().getOrgId());
        try {
            // Validate required fields
            if (location.getLocationCode() == null || location.getLocationCode().trim().isEmpty()) {
                log.error("Location code is required");
                throw new IllegalArgumentException("Location code is required");
            }
            if (location.getLocationName() == null || location.getLocationName().trim().isEmpty()) {
                log.error("Location name is required");
                throw new IllegalArgumentException("Location name is required");
            }
            if (location.getOrg() == null || location.getOrg().getOrgId() == null) {
                log.error("Organization is required");
                throw new IllegalArgumentException("Organization is required");
            }
            
            Integer orgId = location.getOrg().getOrgId();
            Org org = orgRepository.findById(orgId).orElse(null);
            if (org == null) {
                log.error("Organization not found with ID: {}", orgId);
                throw new IllegalArgumentException("Organization not found with ID: " + orgId);
            }
            // Use managed entity to avoid transient/detached org reference on save.
            location.setOrg(org);
            
            // Check if location with same code already exists in this organization
            Location existingByCode = locationRepository.findByOrgOrgIdAndLocationCode(orgId, location.getLocationCode().trim());
            if (existingByCode != null) {
                log.warn("Branch with code '{}' already exists in organization {} (ID: {})", 
                        location.getLocationCode(), orgId, existingByCode.getLocationId());
                throw new IllegalArgumentException("Branch with code '" + location.getLocationCode() + "' already exists in this organization. Please use a different Branch code.");
            }
            
            // Check if location with same name already exists in this organization
            Location existingByName = locationRepository.findByOrgOrgIdAndLocationName(orgId, location.getLocationName().trim());
            if (existingByName != null) {
                log.warn("Branch with name '{}' already exists in organization {} (ID: {})", 
                        location.getLocationName(), orgId, existingByName.getLocationId());
                throw new IllegalArgumentException("Branch with name '" + location.getLocationName() + "' already exists in this organization. Please use a different Branch name.");
            }

            location.setIsActive(true);
            Location savedLocation = locationRepository.save(location);
            log.info("Branch added successfully with ID: {}, Code: {}", 
                    savedLocation.getLocationId(), savedLocation.getLocationCode());
            return savedLocation;

        } catch (IllegalArgumentException e) {
            log.error("Validation error in addLocation: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation in addLocation: {}", e.getMessage());
            String message = extractDuplicateErrorMessage(e, location);
            throw new IllegalArgumentException(message);
        } catch (Exception e) {
            log.error("Error adding location: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add location: " + e.getMessage());
        }
    }

    /**
     * Update an existing location
     */
    public Location updateLocation(Location location) {
        log.info("Updating location with ID: {}", location.getLocationId());
        try {
            // Validate required fields
            if (location.getLocationId() == null || location.getLocationId() <= 0) {
                log.error("Invalid location ID for update");
                throw new IllegalArgumentException("Invalid location ID for update");
            }
            if (location.getLocationCode() == null || location.getLocationCode().trim().isEmpty()) {
                log.error("Location code is required");
                throw new IllegalArgumentException("Location code is required");
            }
            if (location.getLocationName() == null || location.getLocationName().trim().isEmpty()) {
                log.error("Location name is required");
                throw new IllegalArgumentException("Location name is required");
            }

            // Check if location exists
            Location existingLocation = locationRepository.findById(location.getLocationId()).orElse(null);
            if (existingLocation == null) {
                log.warn("Location not found with ID: {} for update", location.getLocationId());
                throw new IllegalArgumentException("Location not found with ID: " + location.getLocationId());
            }
            
            Integer payloadOrgId = (location.getOrg() != null) ? location.getOrg().getOrgId() : null;
            Integer orgId = (payloadOrgId != null) ? payloadOrgId
                    : (existingLocation.getOrg() != null ? existingLocation.getOrg().getOrgId() : null);
            if (orgId == null) {
                log.error("Organization ID is missing for location update. locationId={}", location.getLocationId());
                throw new IllegalArgumentException("Organization is required for location update");
            }
            Org org = orgRepository.findById(orgId).orElse(null);
            if (org == null) {
                log.error("Organization not found with ID: {}", orgId);
                throw new IllegalArgumentException("Organization not found with ID: " + orgId);
            }
            // Always attach managed org reference before save.
            location.setOrg(org);
            
            Location existingByCode = locationRepository.findByOrgOrgIdAndLocationCode(orgId, location.getLocationCode().trim());
            if (existingByCode != null && !existingByCode.getLocationId().equals(location.getLocationId())) {
                log.warn("Branch with code '{}' already exists in organization {} (ID: {})", 
                        location.getLocationCode(), orgId, existingByCode.getLocationId());
                throw new IllegalArgumentException("Branch with code '" + location.getLocationCode() + "' already exists in this organization. Please use a different location code.");
            }
            
            // Check if another location with same name already exists in this organization (exclude current location)
            Location existingByName = locationRepository.findByOrgOrgIdAndLocationName(orgId, location.getLocationName().trim());
            if (existingByName != null && !existingByName.getLocationId().equals(location.getLocationId())) {
                log.warn("Branch with name '{}' already exists in organization {} (ID: {})", 
                        location.getLocationName(), orgId, existingByName.getLocationId());
                throw new IllegalArgumentException("Branch with name '" + location.getLocationName() + "' already exists in this organization. Please use a different Branch name.");
            }

            Location updatedLocation = locationRepository.save(location);
            log.info("Branch updated successfully with ID: {}, Code: {}", 
                    updatedLocation.getLocationId(), updatedLocation.getLocationCode());
            return updatedLocation;

        } catch (IllegalArgumentException e) {
            log.error("Validation error in updateLocation: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation in updateLocation: {}", e.getMessage());
            String message = extractDuplicateErrorMessage(e, location);
            throw new IllegalArgumentException(message);
        } catch (Exception e) {
            log.error("Error updating location: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update location: " + e.getMessage());
        }
    }
    
    /**
     * Extract a user-friendly error message from DataIntegrityViolationException
     */
    private String extractDuplicateErrorMessage(DataIntegrityViolationException e, Location location) {
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMessage.contains("uk_location_code_per_org") || errorMessage.contains("branch_code")) {
            return "Branch code '" + location.getLocationCode() + "' already exists in this organization. Please use a different location code.";
        } else if (errorMessage.contains("uk_location_name_per_org") || errorMessage.contains("branch_name")) {
            return "Branch with name '" + location.getLocationName() + "' already exists in this organization. Please use a different Branch name.";
        } else if (errorMessage.contains("duplicate") || errorMessage.contains("unique")) {
            return "A Branch with the same code or name already exists in this organization. Please use different values.";
        }
        
        return "Failed to save Branch due to data conflict. Please check the Branch code and name are unique within the organization.";
    }

    /**
     * Delete a Branch (hard delete)
     */
    public void deleteLocation(Integer locationId) {
        log.info("Deleting location with ID: {}", locationId);
        try {
            if (locationId == null || locationId <= 0) {
                log.error("Invalid location ID for deletion");
                throw new IllegalArgumentException("Invalid location ID for deletion");
            }

            if (!locationRepository.existsById(locationId)) {
                log.warn("Location not found with ID: {} for deletion", locationId);
                throw new IllegalArgumentException("Location not found with ID: " + locationId);
            }

            locationRepository.deleteById(locationId);
            log.info("Location deleted successfully with ID: {}", locationId);

        } catch (IllegalArgumentException e) {
            log.error("Validation error in deleteLocation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting location: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete location: " + e.getMessage());
        }
    }

    /**
     * Helper method to populate updatedEmployeeName based on updatedBy field
     */
    private void populateUpdatedEmployeeName(Location location) {
        if (location == null) {
            return;
        }
        if (location.getUpdatedBy() != null) {
            String employeeName = employeeService.getEmployeeFullName(location.getUpdatedBy());
            location.setUpdatedEmployeeName(employeeName);
        }
        if (location.getCreatedBy() != null) {
            String creatorName = employeeService.getEmployeeFullName(location.getCreatedBy());
            location.setCreatedEmployeeName(creatorName);
        }
    }
}
