package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.RoleQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleQuestionMappingRepository extends JpaRepository<RoleQuestionMapping, Integer> {

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.role.roleId = :roleId AND rqm.org.orgId = :orgId AND rqm.isActive = true")
        List<RoleQuestionMapping> findByRoleIdAndOrgIdAndIsActiveTrue(@Param("roleId") Integer roleId,
                        @Param("orgId") Integer orgId);

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.role.roleId = :roleId AND rqm.isActive = true AND rqm.question.isActive = true")
        List<RoleQuestionMapping> findByRoleIdAndIsActiveTrue(Integer roleId);

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.isActive = true")
        List<RoleQuestionMapping> findAllActiveRoleQuestionMappings();

        Optional<RoleQuestionMapping> findFirstByOrg_OrgIdAndRole_RoleIdAndQuestion_QuestionId(Integer orgId,
                        Integer roleId, Integer questionId);

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.question.questionId = :questionId AND rqm.org.orgId = :orgId")
        List<RoleQuestionMapping> findByQuestionIdAndOrgId(@Param("questionId") Integer questionId,
                        @Param("orgId") Integer orgId);

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.question.questionId = :questionId AND rqm.isActive = true")
        List<RoleQuestionMapping> findByQuestionIdActive(@Param("questionId") Integer questionId);

        @Query("SELECT rqm FROM RoleQuestionMapping rqm WHERE rqm.org.orgId = :orgId AND rqm.isActive = true")
        List<RoleQuestionMapping> findByOrg_OrgIdAndIsActiveTrue(@Param("orgId") Integer orgId);

                                @Query(value = """
                                                                                                WITH emp_scope AS (
                                                                                                                SELECT e.employee_id, e.org_id, e.location_id AS default_location_id
                                                                                                                FROM base.employee e
                                                                                                                WHERE e.employee_id = :employeeId
                                                                                                                        AND e.org_id = :orgId
                                                                                                                        AND COALESCE(e.is_active, TRUE) = TRUE
                                                                                                ),
                                                                                                emp_branches AS (
                                                                                                                SELECT es.default_location_id AS location_id
                                                                                                                FROM emp_scope es
                                                                                                                WHERE es.default_location_id IS NOT NULL
                                                                                                                UNION
                                                                                                                SELECT ela.location_id
                                                                                                                FROM base.employee_location_access ela
                                                                                                                JOIN emp_scope es
                                                                                                                        ON es.employee_id = ela.employee_id
                                                                                                                 AND es.org_id = ela.org_id
                                                                                                                WHERE COALESCE(ela.is_active, TRUE) = TRUE
                                                                                                )
                                                                                                SELECT DISTINCT
                                                                                                                r.role_id,
                                                                                                                r.role_name,
                                                                                                                r.org_id,
                                                                                                                r.location_id AS branch_id
                                                                                                FROM compliance.role_question_mapping rqm
                                                                                                JOIN base.role r
                                                                                                        ON r.role_id = rqm.role_id
                                                                                                 AND r.org_id  = rqm.org_id
                                                                                                JOIN emp_scope es
                                                                                                        ON es.org_id = r.org_id
                                                                                                LEFT JOIN emp_branches eb
                                                                                                        ON eb.location_id = r.location_id
                                                                                                WHERE rqm.org_id = :orgId
                                                                                                        AND COALESCE(rqm.is_active, TRUE) = TRUE
                                                                                                        AND COALESCE(r.is_active, TRUE) = TRUE
                                                                                                        AND (
                                                                                                                                r.location_id IS NULL
                                                                                                                                OR eb.location_id IS NOT NULL
                                                                                                                        )
                                                                                                ORDER BY r.role_name
                                                                                                """, nativeQuery = true)
                                List<Object[]> findRoleListByOrgIdAndEmployeeIdWithBranchAccess(@Param("orgId") Integer orgId,
                                                                                                @Param("employeeId") Integer employeeId);
}
