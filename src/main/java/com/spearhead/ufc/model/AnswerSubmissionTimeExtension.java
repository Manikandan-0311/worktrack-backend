package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "answer_submission_time_extension", schema = "compliance", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "question_date" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerSubmissionTimeExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extension_id")
    private Integer extensionId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    @Column(name = "reason")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "granted_by", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Employee grantedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_dt", updatable = false, insertable = false)
    private OffsetDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;

    @Column(name = "is_holiday")
    private Boolean isHoliday;

    @Column(name = "permission_status_id")
    private Integer permissionStatusId;
}
