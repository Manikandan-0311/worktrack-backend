package com.spearhead.ufc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.spearhead.ufc.dto.DepartmentDropdownDTO;
import com.spearhead.ufc.dto.EmployeeDropdownDTO;
import com.spearhead.ufc.dto.LocationDropdownDTO;
import com.spearhead.ufc.dto.OrgDropdownDTO;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.service.DropdownService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dropdown")
@CrossOrigin("*")
public class DropdownController {

	private static final Logger log = LoggerFactory.getLogger(DropdownController.class);

	@Autowired
	private DropdownService dropdownService;

	@GetMapping("/orgs")
	public ResponseEntity<Map<String, Object>> getOrgs() {
		log.info("Entered into getOrgs method - DropdownController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<OrgDropdownDTO> orgs = dropdownService.getOrgs();
			resp.put("success", true);
			resp.put("message", "Organization dropdown fetched successfully");
			resp.put("data", orgs);
		} catch (Exception e) {
			log.error("Error occurred in getOrgs method - DropdownController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch Organization dropdown. Please contact admin.");
		} finally {
			log.info("Exiting getOrgs method - DropdownController");
		}
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/locations")
	public ResponseEntity<Map<String, Object>> getLocations(@RequestParam Integer orgId) {
		log.info("Entered into getLocations method - DropdownController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<LocationDropdownDTO> locations = dropdownService.getLocations(orgId);
			resp.put("success", true);
			resp.put("message", "Location dropdown fetched successfully");
			resp.put("data", locations);
		} catch (Exception e) {
			log.error("Error occurred in getLocations method - DropdownController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch Location dropdown. Please contact admin.");
		} finally {
			log.info("Exiting getLocations method - DropdownController");
		}
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/departments")
	public ResponseEntity<Map<String, Object>> getDepartments(@RequestParam Integer orgId) {
		log.info("Entered into getDepartments method - DropdownController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<DepartmentDropdownDTO> departments = dropdownService.getDepartments(orgId);
			resp.put("success", true);
			resp.put("message", "Department dropdown fetched successfully");
			resp.put("data", departments);
		} catch (Exception e) {
			log.error("Error occurred in getDepartments method - DropdownController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch Department dropdown. Please contact admin.");
		} finally {
			log.info("Exiting getDepartments method - DropdownController");
		}
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/employees")
	public ResponseEntity<Map<String, Object>> getEmployees(@RequestParam(required = false) Integer orgId,
			@RequestParam(required = false) Integer locationId, @RequestParam(required = false) Integer departmentId) {

		log.info("Entered into getEmployees method - DropdownController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<EmployeeDropdownDTO> employees = dropdownService.getEmployees(orgId, locationId, departmentId);
			resp.put("success", true);
			resp.put("message", "Employee dropdown fetched successfully");
			resp.put("data", employees);
		} catch (Exception e) {
			log.error("Error occurred in getEmployees method - DropdownController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch Employee dropdown. Please contact admin.");
		} finally {
			log.info("Exiting getEmployees method - DropdownController");
		}
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/roles")
	public ResponseEntity<Map<String, Object>> getRoles(@RequestParam(required = false) Integer orgId) {

		log.info("Entered into getRoles method - DropdownController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<Role> employees = dropdownService.getRoles();
			resp.put("success", true);
			resp.put("message", "Roles dropdown fetched successfully");
			resp.put("data", employees);
		} catch (Exception e) {
			log.error("Error occurred in getRoles method - DropdownController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch Roles dropdown. Please contact admin.");
		} finally {
			log.info("Exiting getRoles method - DropdownController");
		}
		return ResponseEntity.ok(resp);
	}

}
