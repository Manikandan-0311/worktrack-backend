package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.RequestJobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestJobInfoRepository extends JpaRepository<RequestJobInfo, Integer> {

    Optional<RequestJobInfo> findByRequestNo(String requestNo);

    List<RequestJobInfo> findByEmployee_EmployeeId(Integer employeeId);

    List<RequestJobInfo> findByRequestStatus(String requestStatus);

    List<RequestJobInfo> findByEmployee_EmployeeIdAndRequestStatus(Integer employeeId, String requestStatus);

    List<RequestJobInfo> findByJobType(String jobType);

    @Query("""
            SELECT r FROM RequestJobInfo r
            WHERE (:employeeId IS NULL OR r.employee.employeeId = :employeeId)
              AND (:requestStatus IS NULL OR r.requestStatus = :requestStatus)
              AND (:jobType IS NULL OR r.jobType = :jobType)
            ORDER BY r.requestDt DESC
            """)
    List<RequestJobInfo> findByFilters(
            @Param("employeeId") Integer employeeId,
            @Param("requestStatus") String requestStatus,
            @Param("jobType") String jobType);

    @Modifying
    @Transactional
    @Query("""
            UPDATE RequestJobInfo r
            SET r.requestStatus = :status,
                r.filePathUrl = :filePathUrl,
                r.requestEndDt = :endDt,
                r.updatedDt = :updatedDt
            WHERE r.requestId = :requestId
            """)
    int updateStatusAndFilePath(
            @Param("requestId") Integer requestId,
            @Param("status") String status,
            @Param("filePathUrl") String filePathUrl,
            @Param("endDt") OffsetDateTime endDt,
            @Param("updatedDt") OffsetDateTime updatedDt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE RequestJobInfo r
            SET r.requestStatus = :status,
                r.errorLog = :errorLog,
                r.requestEndDt = :endDt,
                r.updatedDt = :updatedDt
            WHERE r.requestId = :requestId
            """)
    int updateStatusWithError(
            @Param("requestId") Integer requestId,
            @Param("status") String status,
            @Param("errorLog") String errorLog,
            @Param("endDt") OffsetDateTime endDt,
            @Param("updatedDt") OffsetDateTime updatedDt);
}
