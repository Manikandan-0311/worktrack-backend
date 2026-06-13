/**
 * 
 */
package com.spearhead.ufc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.dto.SpecialPermissionDTO;
import com.spearhead.ufc.model.SpecialPermission;

/**
 * @author manikandan.m Specail Permission ask and approval table
 * 
 *         asked by employee approved by Admin
 */

@Repository
public interface SpecialPermissionRepository extends JpaRepository<SpecialPermission, Integer> {
	@Query(value = """
				SELECT
					s.permission_id,
					s.reason,
					s.employee_id,
					s.permission_date,
					s.permission_status_id,
					s.remarks,
					s.granted_by,
					s.created_by,
					s.created_dt,
					s.permission_type,
					be.location_id,
					be.department_id
				FROM compliance.special_permissions s
				JOIN base.employee be ON s.employee_id = be.employee_id
				WHERE (:employeeId IS NULL OR be.employee_id = :employeeId)
				  AND (:locationId IS NULL OR be.location_id = :locationId)
				  AND (:departmentId IS NULL OR be.department_id = :departmentId)
			""", nativeQuery = true)
	List<SpecialPermissionDTO> list(@Param("employeeId") Integer employeeId, @Param("locationId") Integer locationId,
			@Param("departmentId") Integer departmentId);

}
