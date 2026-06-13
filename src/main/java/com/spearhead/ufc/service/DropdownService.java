package com.spearhead.ufc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.spearhead.ufc.dto.DepartmentDropdownDTO;
import com.spearhead.ufc.dto.EmployeeDropdownDTO;
import com.spearhead.ufc.dto.LocationDropdownDTO;
import com.spearhead.ufc.dto.OrgDropdownDTO;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.repository.DepartmentRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.LocationRepository;
import com.spearhead.ufc.repository.OrgRepository;
import com.spearhead.ufc.repository.RoleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DropdownService {

	private static final Logger log = LoggerFactory.getLogger(DropdownService.class);

	@Autowired
	private OrgRepository orgRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private RoleRepository roleRepository;

	public List<OrgDropdownDTO> getOrgs() {
		log.info("Entered into getOrgs method - DropdownService");
		try {
			List<OrgDropdownDTO> orgList = orgRepository.findByIsActiveTrue().stream()
					.map(o -> new OrgDropdownDTO(o.getOrgId(), o.getOrgName())).collect(Collectors.toList());
			return orgList;
		} catch (Exception e) {
			log.error("Error occurred in getOrgs method - DropdownService", e);
			throw e;
		} finally {
			log.info("Exiting getOrgs method - DropdownService");
		}
	}

	public List<LocationDropdownDTO> getLocations(Integer orgId) {
		log.info("Entered into getLocations method - DropdownService");
		try {
			List<LocationDropdownDTO> locationList = locationRepository.findByOrgOrgIdAndIsActiveTrue(orgId).stream()
					.map(l -> new LocationDropdownDTO(l.getLocationId(), l.getLocationName()))
					.collect(Collectors.toList());
			return locationList;
		} catch (Exception e) {
			log.error("Error occurred in getLocations method - DropdownService", e);
			throw e;
		} finally {
			log.info("Exiting getLocations method - DropdownService");
		}
	}

	public List<DepartmentDropdownDTO> getDepartments(Integer orgId) {
		log.info("Entered into getDepartments method - DropdownService");
		try {
			List<DepartmentDropdownDTO> deptList = departmentRepository.findByOrgOrgIdAndIsActiveTrue(orgId).stream()
					.map(d -> new DepartmentDropdownDTO(d.getDepartmentId(), d.getDepartmentName()))
					.collect(Collectors.toList());
			return deptList;
		} catch (Exception e) {
			log.error("Error occurred in getDepartments method - DropdownService", e);
			throw e;
		} finally {
			log.info("Exiting getDepartments method - DropdownService");
		}
	}

	public List<EmployeeDropdownDTO> getEmployees(Integer orgId, Integer locationId, Integer departmentId) {
		log.info("Entered into getEmployees method - DropdownService ");
		try {
			List<EmployeeDropdownDTO> empList = employeeRepository
					.findEmployeesByFilters(orgId, locationId).stream()
					.map(e -> new EmployeeDropdownDTO(e.getEmployeeId(),
							e.getFirstName() + (e.getLastName() != null ? " " + e.getLastName() : "")))
					.collect(Collectors.toList());
			return empList;
		} catch (Exception e) {
			log.error("Error occurred in getEmployees method - DropdownService", e);
			throw e;
		} finally {
			log.info("Exiting getEmployees method - DropdownService");
		}
	}

	public List<Role> getRoles() {
		log.info("Entered into getRoles method - DropdownService");
		try {
			List<Role> role = roleRepository.findAll();
			return role;
		} catch (Exception e) {
			log.error("Error occurred in getRoles method - DropdownService", e);
			throw e;
		} finally {
			log.info("Exiting getRoles method - DropdownService");
		}
	}
}
