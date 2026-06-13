package com.spearhead.ufc.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User submitted answer for the questions (Daily submission)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "compliance_submission",
    schema = "compliance",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_employee_question_day",
            columnNames = { "employee_id", "question_id", "question_date" }
        )
    }
)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private int submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank questionBank;

    /**
     * Logical date for which the question belongs
     * (VERY IMPORTANT for repeated daily questions)
     */
    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    /**
     * Actual submission timestamp
     * (Can be next day if time extension is granted)
     */
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "answer")
    private String answer;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "marks_awarded")
    private int marksAwarded;


    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_dt")
    private OffsetDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;

    @Column(name = "is_active")
    private boolean isActive;

    /**
     * Auto-set submission time if not provided
     */
    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = OffsetDateTime.now();
        }
    }
}
