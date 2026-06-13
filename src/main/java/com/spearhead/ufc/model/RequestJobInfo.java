package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "request_job_info",
    schema = "base",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk1_request_job_info", columnNames = {"request_no", "employee_id"})
    }
)
public class RequestJobInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "request_no", nullable = false, length = 50)
    private String requestNo;

    @Column(name = "job_type", length = 30)
    private String jobType;

    @Column(name = "program_name", nullable = false, length = 200)
    private String programName;

    @Column(name = "file_path_url", length = 200)
    private String filePathUrl;

    @Column(name = "request_status", length = 30)
    private String requestStatus = "PENDING";

    @Column(name = "error_log", length = 500)
    private String errorLog;

    @Column(name = "requested_by", length = 20)
    private String requestedBy;

    @Column(name = "request_dt", insertable = true, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime requestDt;

    @Column(name = "request_end_dt")
    private OffsetDateTime requestEndDt;

    @Column(name = "updated_dt",
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedDt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_employee_id"))
    private Employee employee;
}
