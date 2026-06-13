package com.spearhead.ufc.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.repository.DepartmentRepository;
import com.spearhead.ufc.repository.LocationRepository;
import com.spearhead.ufc.repository.OrgRepository;
import com.spearhead.ufc.repository.RoleRepository;
import com.spearhead.ufc.repository.SubmissionRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.EmployeeLocationAccessRepository;

import com.spearhead.ufc.dto.EmployeeGetByIdResponseDTO;
import com.spearhead.ufc.dto.EmployeeLocationAccessItemDTO;
import com.spearhead.ufc.dto.LocationBasicDTO;
import com.spearhead.ufc.dto.OrgBasicDTO;
import com.spearhead.ufc.dto.BranchBasicDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeService {

	@Autowired
	private EmployeeRepository employeeRepository;
	@Autowired
	private OrgRepository orgRepository;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private DepartmentRepository departmentRepository;
	@Autowired
	private RoleRepository roleRepository;

	@Autowired(required = false)
	private EmployeeLocationAccessRepository employeeLocationAccessRepository;

	@Autowired
	private SubmissionRepository submissionRepository;

	public EmployeeService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	public List<Employee> getEmployees(Integer orgId, Integer locationId, Integer departmentId, Integer employeeId) {
		log.info("Entered into getEmployees method - EmployeeService");
		try {
			Integer orgIdParam = (orgId != null && orgId != 0) ? orgId : null;
			Integer locationIdParam = (locationId != null && locationId != 0) ? locationId : null;
			Integer departmentIdParam = (departmentId != null && departmentId != 0) ? departmentId : null;
			Integer employeeIdParam = (employeeId != null && employeeId != 0) ? employeeId : null;

			List<Employee> employees = employeeRepository.findEmployeesList(
					orgIdParam, locationIdParam, departmentIdParam, employeeIdParam);
			return employees;
		} catch (Exception e) {
			log.error("Error occurred in getEmployees method - EmployeeService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch employees: " + e.getMessage(), e);
		} finally {
			log.info("Exited from getEmployees method - EmployeeService");
		}
	}

	public Employee addEmployee(Employee emp, List<EmployeeLocationAccess> locationAccessList, Integer createdBy) {
		log.info("Entered into addEmployee method - EmployeeService");
		try {
			// Set references if only IDs are provided
			if (emp.getOrg() != null && emp.getOrg().getOrgId() != null)
				emp.setOrg(orgRepository.getReferenceById(emp.getOrg().getOrgId()));
			if (emp.getLocation() != null && emp.getLocation().getLocationId() != null)
				emp.setLocation(locationRepository.getReferenceById(emp.getLocation().getLocationId()));
			if (emp.getDepartment() != null && emp.getDepartment().getDepartmentId() != null)
				emp.setDepartment(departmentRepository.getReferenceById(emp.getDepartment().getDepartmentId()));
			if (emp.getRoleId() != null && emp.getRoleId().getRoleId() != null)
				emp.setRoleId(roleRepository.getReferenceById(emp.getRoleId().getRoleId()));

			// Ensure created metadata is set, then save employee to get the ID
			if (emp.getCreatedDt() == null) {
				emp.setCreatedDt(OffsetDateTime.now());
			}
			if (emp.getCreatedBy() == null && createdBy != null) {
				emp.setCreatedBy(createdBy);
			}
			if (emp.getSpecialPermissionFlag() == null) {
				emp.setSpecialPermissionFlag(false);
			}
			Employee saved = employeeRepository.save(emp);
			log.info("Employee saved with ID: {}", saved.getEmployeeId());

			// Save location access if provided
			if (locationAccessList != null && !locationAccessList.isEmpty() && employeeLocationAccessRepository != null) {
				log.info("Saving {} location access records for employee {}", locationAccessList.size(), saved.getEmployeeId());
				// Persist new access records (fresh save, no cleanup needed)
				for (EmployeeLocationAccess ela : locationAccessList) {
					ela.setEmployee(saved);
					// Load Location and Org entity references if only IDs are provided
					if (ela.getLocation() != null && ela.getLocation().getLocationId() != null) {
						ela.setLocation(locationRepository.getReferenceById(ela.getLocation().getLocationId()));
					}
					if (ela.getOrg() != null && ela.getOrg().getOrgId() != null) {
						ela.setOrg(orgRepository.getReferenceById(ela.getOrg().getOrgId()));
					}
					if (ela.getCreatedBy() == null) ela.setCreatedBy(createdBy);
					if (ela.getIsActive() == null) ela.setIsActive(true);
					employeeLocationAccessRepository.save(ela);
				}
				log.info("Location access records saved successfully");
			}
			return saved;
		} catch (Exception e) {
			log.error("Error occurred in addEmployee method - EmployeeService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to add employee: " + e.getMessage(), e);
		} finally {
			log.info("Exited from addEmployee method - EmployeeService");
		}
	}

	public Employee updateEmployee(Employee emp, List<EmployeeLocationAccess> locationAccessList, Integer updatedBy) {
		log.info("Entered into updateEmployee method - EmployeeService");
		try {
			OffsetDateTime now = OffsetDateTime.now();
			Integer employeeId = emp.getEmployeeId();

			// Insert mode through /update: set both created and updated audit fields.
			if (employeeId == null || employeeId <= 0) {
				if (emp.getCreatedDt() == null) {
					emp.setCreatedDt(now);
				}
				if (emp.getCreatedBy() == null && updatedBy != null) {
					emp.setCreatedBy(updatedBy);
				}
				emp.setUpdatedDt(now);
				if (updatedBy != null) {
					emp.setUpdatedBy(updatedBy);
				}
				return addEmployee(emp, locationAccessList, updatedBy);
			}

			Employee existing = employeeRepository.findById(employeeId)
					.orElseThrow(() -> new RuntimeException("Employee not found"));

			existing.setEmployeeCode(emp.getEmployeeCode());
			existing.setFirstName(emp.getFirstName());
			existing.setLastName(emp.getLastName());
			existing.setEmailId(emp.getEmailId());
			existing.setPhoneNumber(emp.getPhoneNumber());
			existing.setIsActive(emp.getIsActive());
			existing.setJoinDate(emp.getJoinDate());
			existing.setRelieveDate(emp.getRelieveDate());
			existing.setRemarks(emp.getRemarks());
			existing.setSalary(emp.getSalary());
			existing.setIncentive(emp.getIncentive());
			existing.setSpecialPermissionFlag(emp.getSpecialPermissionFlag());
			existing.setUpdatedBy(updatedBy);
			existing.setUpdatedDt(now);

			if (emp.getLocation() != null && emp.getLocation().getLocationId() != null)
				existing.setLocation(locationRepository.getReferenceById(emp.getLocation().getLocationId()));
			if (emp.getDepartment() != null && emp.getDepartment().getDepartmentId() != null)
				existing.setDepartment(departmentRepository.getReferenceById(emp.getDepartment().getDepartmentId()));
			if (emp.getRoleId() != null && emp.getRoleId().getRoleId() != null)
				existing.setRoleId(roleRepository.getReferenceById(emp.getRoleId().getRoleId()));

			// Save employee first
			Employee updated = employeeRepository.save(existing);
			log.info("Employee updated with ID: {}", updated.getEmployeeId());

			// Update location access if provided (soft update: enable/disable instead of delete/recreate)
			if (locationAccessList != null && !locationAccessList.isEmpty() && employeeLocationAccessRepository != null) {
				log.info("Updating {} location access records for employee {}", locationAccessList.size(), employeeId);
				
				// Fetch existing location access rows for this employee
				List<EmployeeLocationAccess> existingAccess = employeeLocationAccessRepository.findByEmployeeEmployeeId(employeeId);
				Set<Integer> requestedLocationIds = new HashSet<>();
				
				// Build set of location IDs in the current request
				for (EmployeeLocationAccess ela : locationAccessList) {
					if (ela.getLocation() != null && ela.getLocation().getLocationId() != null) {
						requestedLocationIds.add(ela.getLocation().getLocationId());
					}
				}
				
				// Mark existing rows not in request as inactive (soft delete)
				for (EmployeeLocationAccess existing1 : existingAccess) {
					if (!requestedLocationIds.contains(existing1.getLocation().getLocationId())) {
						log.info("Marking location {} as inactive for employee {}", existing1.getLocation().getLocationId(), employeeId);
						existing1.setIsActive(false);
						existing1.setUpdatedBy(updatedBy);
						existing1.setUpdatedDt(now);
						employeeLocationAccessRepository.save(existing1);
					}
				}
				
				// Upsert rows in request
				for (EmployeeLocationAccess ela : locationAccessList) {
					Integer locationId = ela.getLocation() != null ? ela.getLocation().getLocationId() : null;
					if (locationId == null) continue;
					
					// Check if this location already exists for the employee
					boolean exists = existingAccess.stream()
						.anyMatch(e -> e.getLocation() != null && e.getLocation().getLocationId().equals(locationId));
					
					if (exists) {
						// Update existing: set isActive based on request
						for (EmployeeLocationAccess existing1 : existingAccess) {
							if (existing1.getLocation() != null && existing1.getLocation().getLocationId().equals(locationId)) {
								existing1.setIsActive(ela.getIsActive() != null ? ela.getIsActive() : true);
								existing1.setUpdatedBy(updatedBy);
								existing1.setUpdatedDt(now);
								employeeLocationAccessRepository.save(existing1);
								log.info("Updated location {} to isActive={} for employee {}", locationId, existing1.getIsActive(), employeeId);
								break;
							}
						}
					} else {
						// Create new: load Location and Org entity references
						ela.setEmpLocationAccessId(null);
						ela.setEmployee(updated);
						if (ela.getLocation() != null && ela.getLocation().getLocationId() != null) {
							ela.setLocation(locationRepository.getReferenceById(ela.getLocation().getLocationId()));
						}
						if (ela.getOrg() != null && ela.getOrg().getOrgId() != null) {
							ela.setOrg(orgRepository.getReferenceById(ela.getOrg().getOrgId()));
						}
						ela.setIsActive(ela.getIsActive() != null ? ela.getIsActive() : true);
						if (ela.getCreatedBy() == null) ela.setCreatedBy(updatedBy);
						if (ela.getUpdatedBy() == null) ela.setUpdatedBy(updatedBy);
						ela.setCreatedDt(now);
						ela.setUpdatedDt(now);
						employeeLocationAccessRepository.save(ela);
						log.info("Created new location {} for employee {}", locationId, employeeId);
					}
				}
				log.info("Location access records updated successfully");
			}

			return updated;
		} catch (Exception e) {
			log.error("Error occurred in updateEmployee method - EmployeeService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to update employee: " + e.getMessage(), e);
		} finally {
			log.info("Exited from updateEmployee method - EmployeeService");
		}
	}

	public List<Submission> employeeDashboard(Submission submission) {
		log.info("Enter into the employeeDashboard method - EmployeeService");
		try {
			if (submission.getEmployee() == null || submission.getEmployee().getEmployeeId() == null) {
				throw new RuntimeException("Employee is required for dashboard");
			}
			List<Submission> employeeData = submissionRepository.findByEmployeeId(submission.getEmployee().getEmployeeId());
			return employeeData;
		} catch (Exception e) {
			log.error("Error Occured into the employeeDashboard method - EmployeeService", e);
			throw e;
		} finally {
			log.info("Exited from the employeeDashboard method - EmployeeService");
		}
	}

	public void saveEmployeeLocationAccess(Integer employeeId, Integer orgId, Integer defaultLocationId, List<Integer> enabledLocationIds, Integer createdBy) {
		if (employeeLocationAccessRepository == null) {
			throw new IllegalStateException("EmployeeLocationAccessRepository bean not available");
		}
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		Org org = null;
		if (orgId != null) {
			org = orgRepository.findById(orgId).orElse(null);
		}
		if (org == null) {
			throw new IllegalArgumentException("Org not found or not provided");
		}

		Set<Integer> locationIds = new HashSet<>();
		if (enabledLocationIds != null) {
			locationIds.addAll(enabledLocationIds);
		}
		if (defaultLocationId != null) {
			locationIds.add(defaultLocationId);
		}
		if (locationIds.isEmpty()) {
			throw new IllegalArgumentException("At least the default location must be provided");
		}

		// Replace existing access set for idempotency
		employeeLocationAccessRepository.deleteByEmployeeEmployeeId(employeeId);

		List<EmployeeLocationAccess> toSave = new ArrayList<>();
		for (Integer locId : locationIds) {
			Location location = locationRepository.findById(locId)
					.orElseThrow(() -> new IllegalArgumentException("Location not found: " + locId));
			EmployeeLocationAccess ela = new EmployeeLocationAccess();
			ela.setEmployee(employee);
			ela.setOrg(org);
			ela.setLocation(location);
			ela.setIsActive(true);
			ela.setCreatedBy(createdBy);
			toSave.add(ela);
		}

		employeeLocationAccessRepository.saveAll(toSave);
	}

	/**
	 * Get employee full name (firstName + lastName) by employee ID
	 * 
	 * @param employeeId the employee ID
	 * @return the full name of the employee, or null if not found
	 */
	public String getEmployeeFullName(Integer employeeId) {
		log.debug("Fetching employee name for ID: {}", employeeId);
		if (employeeId == null) {
			return null;
		}
		try {
			Employee employee = employeeRepository.findById(employeeId).orElse(null);
			if (employee != null) {
				String fullName = employee.getFirstName();
				if (employee.getLastName() != null && !employee.getLastName().trim().isEmpty()) {
					fullName = fullName + " " + employee.getLastName();
				}
				log.debug("Retrieved employee name for ID {}: {}", employeeId, fullName);
				return fullName;
			}
			log.debug("Employee not found for ID: {}", employeeId);
			return null;
		} catch (Exception e) {
			log.error("Error fetching employee name for ID {}: {}", employeeId, e.getMessage(), e);
			return null;
		}
	}
	public EmployeeGetByIdResponseDTO getEmployeeByIdWithAccess(Integer employeeId) {
    log.info("Entered into getEmployeeByIdWithAccess method - EmployeeService");
    try {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be valid");
        }
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Fetch location access records
        List<EmployeeLocationAccess> locationAccessList = employeeLocationAccessRepository != null 
            ? employeeLocationAccessRepository.findByEmployeeEmployeeId(employeeId)
            : new ArrayList<>();
        
        // Build response DTO
        EmployeeGetByIdResponseDTO response = new EmployeeGetByIdResponseDTO();
        response.setEmployeeId(employee.getEmployeeId());
        response.setFirstName(employee.getFirstName());
		response.setEmployee(employee);
        
        // Map organization
        if (employee.getOrg() != null) {
            OrgBasicDTO orgDto = new OrgBasicDTO();
            orgDto.setOrgId(employee.getOrg().getOrgId());
            orgDto.setOrgName(employee.getOrg().getOrgName());
            response.setOrganization(orgDto);
        }
        
        // Map branch/location (assuming Location is used as branch)
        if (employee.getLocation() != null) {
            BranchBasicDTO branchDto = new BranchBasicDTO();
            branchDto.setBranchId(employee.getLocation().getLocationId());
            branchDto.setBranchName(employee.getLocation().getLocationName());
            response.setBranch(branchDto);
        }
        
        // Map location access items
        List<EmployeeLocationAccessItemDTO> accessItems = new ArrayList<>();
        for (EmployeeLocationAccess access : locationAccessList) {
            if (access.getIsActive() != null && access.getIsActive()) {
                EmployeeLocationAccessItemDTO itemDto = new EmployeeLocationAccessItemDTO();
                if (access.getLocation() != null) {
                    LocationBasicDTO locDto = new LocationBasicDTO();
                    locDto.setLocationId(access.getLocation().getLocationId());
                    locDto.setLocationName(access.getLocation().getLocationName());
                    itemDto.setLocation(locDto);
                }
                accessItems.add(itemDto);
            }
        }
        response.setEmployeeLocationAccess(accessItems);
        
        return response;
    } catch (IllegalArgumentException e) {
        log.error("Validation error in getEmployeeByIdWithAccess: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("Error occurred in getEmployeeByIdWithAccess method - EmployeeService: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to fetch employee: " + e.getMessage(), e);
    } finally {
        log.info("Exited from getEmployeeByIdWithAccess method - EmployeeService");
    }
}
}