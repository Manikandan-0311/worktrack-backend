package com.spearhead.ufc.service;

import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.repository.OrgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrgService {
    private static final Logger log = LoggerFactory.getLogger(OrgService.class);

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private EmployeeService employeeService;

    public List<Org> getAllOrganizations() {
        log.debug("Fetching all organizations from database");
        try {
            List<Org> orgs = orgRepository.findAll();
            // Populate updatedEmployeeName for each organization based on updatedBy field
            for (Org org : orgs) {
                if (org.getUpdatedBy() != null) {
                    String employeeName = employeeService.getEmployeeFullName(org.getUpdatedBy());
                    org.setUpdatedEmployeeName(employeeName);
                }
                if (org.getCreatedBy() != null) {
                    String creatorName = employeeService.getEmployeeFullName(org.getCreatedBy());
                    org.setCreatedEmployeeName(creatorName);
                }
            }
            log.debug("Successfully retrieved {} organizations", orgs.size());
            return orgs;
        } catch (Exception e) {
            log.error("Error retrieving all organizations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve organizations", e);
        }
    }
    
    public Org addOrganization(Org org) {
        log.info("Adding new organization: {}", org.getOrgName());
        try {
            if (org.getOrgCode() == null || org.getOrgCode().trim().isEmpty()) {
                log.error("Organization code is required");
                throw new IllegalArgumentException("Organization code cannot be empty");
            }
            if (org.getOrgName() == null || org.getOrgName().trim().isEmpty()) {
                log.error("Organization name is required");
                throw new IllegalArgumentException("Organization name cannot be empty");
            }
            
            // Check if organization with same code already exists
            Org existingOrgByCode = orgRepository.findByOrgCode(org.getOrgCode().trim());
            if (existingOrgByCode != null) {
                log.warn("Organization with code '{}' already exists (ID: {})", org.getOrgCode(), existingOrgByCode.getOrgId());
                throw new IllegalArgumentException("Organization with code '" + org.getOrgCode() + "' already exists. Please use a different organization code.");
            }
            
            // Check if organization with same name already exists
            Org existingOrgByName = orgRepository.findByOrgName(org.getOrgName().trim());
            if (existingOrgByName != null) {
                log.warn("Organization with name '{}' already exists (ID: {})", org.getOrgName(), existingOrgByName.getOrgId());
                throw new IllegalArgumentException("Organization with name '" + org.getOrgName() + "' already exists. Please use a different organization name.");
            }
            
            org.setOrgId(null);
            Org savedOrg = orgRepository.save(org);
            log.info("Organization added successfully with ID: {}, Code: {}", savedOrg.getOrgId(), savedOrg.getOrgCode());
            return savedOrg;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while adding organization: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding organization: {}", e.getMessage());
            String message = extractDuplicateErrorMessage(e, org);
            throw new IllegalArgumentException(message);
        } catch (Exception e) {
            log.error("Error adding organization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add organization", e);
        }
    }

    public Org updateOrganization(Org org) {
        log.info("Updating organization with ID: {}", org.getOrgId());
        try {
            if (org.getOrgId() == null || org.getOrgId() <= 0) {
                log.error("Invalid organization ID for update: {}", org.getOrgId());
                throw new IllegalArgumentException("Invalid organization ID provided");
            }
            Org existingOrg = orgRepository.findById(org.getOrgId())
                    .orElseThrow(() -> {
                        log.error("Organization not found with ID: {}", org.getOrgId());
                        return new IllegalArgumentException("Organization ID not found for update");
                    });
            if (org.getOrgCode() == null || org.getOrgCode().trim().isEmpty()) {
                log.error("Organization code is required for update");
                throw new IllegalArgumentException("Organization code cannot be empty");
            }
            if (org.getOrgName() == null || org.getOrgName().trim().isEmpty()) {
                log.error("Organization name is required for update");
                throw new IllegalArgumentException("Organization name cannot be empty");
            }
            
            // Check if another organization with same code already exists (exclude current org)
            Org existingOrgByCode = orgRepository.findByOrgCode(org.getOrgCode().trim());
            if (existingOrgByCode != null && !existingOrgByCode.getOrgId().equals(org.getOrgId())) {
                log.warn("Organization with code '{}' already exists (ID: {})", org.getOrgCode(), existingOrgByCode.getOrgId());
                throw new IllegalArgumentException("Organization with code '" + org.getOrgCode() + "' already exists. Please use a different organization code.");
            }
            
            // Check if another organization with same name already exists (exclude current org)
            Org existingOrgByName = orgRepository.findByOrgName(org.getOrgName().trim());
            if (existingOrgByName != null && !existingOrgByName.getOrgId().equals(org.getOrgId())) {
                log.warn("Organization with name '{}' already exists (ID: {})", org.getOrgName(), existingOrgByName.getOrgId());
                throw new IllegalArgumentException("Organization with name '" + org.getOrgName() + "' already exists. Please use a different organization name.");
            }
            
            // Preserve immutable create-audit fields during update
            org.setCreatedBy(existingOrg.getCreatedBy());
            org.setCreatedDt(existingOrg.getCreatedDt());

            Org updatedOrg = orgRepository.save(org);
            log.info("Organization updated successfully with ID: {}, Code: {}", updatedOrg.getOrgId(), updatedOrg.getOrgCode());
            return updatedOrg;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while updating organization: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating organization: {}", e.getMessage());
            String message = extractDuplicateErrorMessage(e, org);
            throw new IllegalArgumentException(message);
        } catch (Exception e) {
            log.error("Error updating organization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update organization", e);
        }
    }
    
    /**
     * Extract a user-friendly error message from DataIntegrityViolationException
     */
    private String extractDuplicateErrorMessage(DataIntegrityViolationException e, Org org) {
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMessage.contains("uk_org_code") || errorMessage.contains("org_code")) {
            return "Organization with code '" + org.getOrgCode() + "' already exists. Please use a different organization code.";
        } else if (errorMessage.contains("uk_org_name") || errorMessage.contains("org_name")) {
            return "Organization with name '" + org.getOrgName() + "' already exists. Please use a different organization name.";
        } else if (errorMessage.contains("duplicate") || errorMessage.contains("unique")) {
            return "An organization with the same code or name already exists. Please use different values.";
        }
        
        return "Failed to save organization due to data conflict. Please check the organization code and name are unique.";
    }
}
