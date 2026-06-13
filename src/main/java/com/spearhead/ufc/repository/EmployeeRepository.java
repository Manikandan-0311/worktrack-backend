package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.spearhead.ufc.model.Employee;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
	@Query("""
			    SELECT e FROM Employee e
			    WHERE (:orgId IS NULL OR e.org.orgId = :orgId)
			      AND (:locId IS NULL OR e.location.locationId = :locId)
			      AND e.isActive = true
			""")
	List<Employee> findEmployeesByFilters(@Param("orgId") Integer orgId, @Param("locId") Integer locId);

	@Query("""
			    SELECT e FROM Employee e
			    WHERE (:orgId IS NULL OR e.org.orgId = :orgId)
			      AND (:locId IS NULL OR e.location.locationId = :locId)
			      AND (:roleId IS NULL OR e.roleId.roleId = :roleId)
			      AND e.isActive = true
			""")
	List<Employee> findEmployeesByFiltersAndRole(@Param("orgId") Integer orgId, @Param("locId") Integer locId,
			@Param("roleId") Integer roleId);

	@Query("SELECT e FROM Employee e " +
			"LEFT JOIN FETCH e.org " +
			"LEFT JOIN FETCH e.location " +
			"LEFT JOIN FETCH e.department " +
			"LEFT JOIN FETCH e.roleId " +
			"WHERE (:orgId IS NULL OR e.org.orgId = :orgId) " +
			"AND (:locationId IS NULL OR e.location.locationId = :locationId) " +
			"AND (:departmentId IS NULL OR e.department.departmentId = :departmentId) " +
			"AND (:employeeId IS NULL OR e.employeeId = :employeeId) " +
			"ORDER BY e.employeeId DESC")
	List<Employee> findEmployeesList(@Param("orgId") Integer orgId,
			@Param("locationId") Integer locationId,
			@Param("departmentId") Integer departmentId,
			@Param("employeeId") Integer employeeId);

	Optional<Employee> findByUsername(String username);

	Optional<Employee> findByEmailId(String emailId);

	Optional<Employee> findByEmailIdIgnoreCase(String emailId);

	// // Check if employee code exists for a specific org and location
	// @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee
	// e " +
	// "WHERE e.org.orgId = :orgId AND e.location.locationId = :locationId AND
	// LOWER(e.employeeCode) = LOWER(:employeeCode)")
	// boolean existsEmployeeCodeInOrgAndLocation(@Param("orgId") Integer orgId,
	// @Param("locationId") Integer locationId,
	// @Param("employeeCode") String employeeCode);

	@Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e " +
			"WHERE e.org.orgId = :orgId AND e.location.locationId = :locationId " +
			"AND LOWER(e.employeeCode) = LOWER(:employeeCode) AND e.isActive = true")
	boolean existsEmployeeCodeInOrgAndLocation(@Param("orgId") Integer orgId,
			@Param("locationId") Integer locationId,
			@Param("employeeCode") String employeeCode);

	// Check if employee code exists for a specific org and location, excluding a
	// given employee ID (for updates)
	@Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e " +
			"WHERE e.org.orgId = :orgId AND e.location.locationId = :locationId AND LOWER(e.employeeCode) = LOWER(:employeeCode) "
			+
			"AND e.employeeId != :employeeId")
	boolean existsEmployeeCodeInOrgAndLocationExcludingId(@Param("orgId") Integer orgId,
			@Param("locationId") Integer locationId,
			@Param("employeeCode") String employeeCode,
			@Param("employeeId") Integer employeeId);
}
