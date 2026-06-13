/**
 * 
 */
package com.spearhead.ufc.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spearhead.ufc.dto.SpecialPermissionDTO;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.SpecialPermission;
import com.spearhead.ufc.service.SpecialPermissionService;

/**
 * @author manikandan.m
 * 
 *         this controller for special permission
 */

@RestController
@RequestMapping("/specialPermission")
@CrossOrigin(origins = "*")
public class SpecialPermissionController {
	private static final Logger log = LoggerFactory.getLogger(SpecialPermissionController.class);

	@Autowired
	SpecialPermissionService specialPermissionService;

	@PostMapping("/add")
	public JsonResponse add(@RequestBody SpecialPermission specialPermission) {
		log.info("Enter into add method - SpecialPermissionController");
		try {
			this.specialPermissionService.add(specialPermission);
			return JsonResponse.of(true, null, "Permission created.");
		} catch (Exception e) {
			log.error("Error Occured into the add method - SpecialPermissionController", e);
			return JsonResponse.of(false, null, "Failed to add Special Permission.");
		} finally {
			log.info("Exited from the add method - SpecialPermissionController");
		}
	}

	@PostMapping("/list")
	public JsonResponse list(@RequestBody SpecialPermissionDTO specialPermission) {
		log.info("Enter into list method - SpecialPermissionController");
		try {
			List<SpecialPermissionDTO> special = specialPermissionService.list(specialPermission);
			return JsonResponse.of(true, special, "Permissions fetched.");
		} catch (Exception e) {
			log.error("Error Occured into the list method - SpecialPermissionController", e);
			return JsonResponse.of(false, null, "Failed to fetch Special Permissions.");
		} finally {
			log.info("Exited from the list method - SpecialPermissionController");
		}
	}

}
