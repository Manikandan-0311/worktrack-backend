/**
 * 
 */
package com.spearhead.ufc.service;

import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spearhead.ufc.dto.SpecialPermissionDTO;
import com.spearhead.ufc.model.PermissionStatus;
import com.spearhead.ufc.model.SpecialPermission;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.SpecialPermissionRepository;
import com.spearhead.ufc.utils.PermissionStatusEnum;

/**
 * @author manikandan.m
 * 
 *         Special Permission service class
 */

@Service
public class SpecialPermissionService {
	private static final Logger log = LoggerFactory.getLogger(SpecialPermissionService.class);

	@Autowired
	SpecialPermissionRepository specialPermissionRepository;

	public void add(SpecialPermission specialPermission) {
		log.info("Enter into add method - SpecialPermissionService");
		try {
			PermissionStatus permissionStatus = new PermissionStatus();
			permissionStatus.setPermissionStatusId(PermissionStatusEnum.PENDING.permissionStatusId);
			specialPermission.setPermissionStatus(permissionStatus);
			Employee employee = new Employee();
			employee.setEmployeeId(specialPermission.getEmployee().getEmployeeId());
			specialPermission.setEmployee(employee);
			specialPermission.setCreatedBy(specialPermission.getEmployee().getEmployeeId());
			specialPermissionRepository.save(specialPermission);
		} catch (Exception e) {
			log.error("Error Occured into the add method - SpecialPermissionService");
			throw e;
		} finally {
			log.info("Existing from the add method - SpecialPermissionService");
		}
	}

	public List<SpecialPermissionDTO> list(SpecialPermissionDTO specialPermission) {
		log.info("Enter into add method - SpecialPermissionService");
		try {
			    List<SpecialPermissionDTO> special = specialPermissionRepository.list(specialPermission.getEmployeeId(),
				    specialPermission.getLocationId(), specialPermission.getDepartmentId());

			return special;
		} catch (Exception e) {
			log.error("Error Occured into the list method - SpecialPermissionService");
			throw e;
		} finally {
			log.info("Existing from the list method - SpecialPermissionService");
		}
	}
}
