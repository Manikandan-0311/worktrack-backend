package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "question_bank", schema = "compliance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // @ManyToOne
    // @JoinColumn(name = "department_id", nullable = false)
    // private Department department;

    @Column(name = "question_text", nullable = false)
    private String questionText; // TEXT

    @Column(name = "question_type", nullable = false)
    private String questionType; // VARCHAR(20)

    @Column(name = "weightage", nullable = false)
    private Double weightage; // NUMERIC(5,2)

    @Column(name = "is_active")
    private Boolean isActive; // BOOLEAN

    @Column(name = "created_by")
    private Integer createdBy; // INTEGER

    @Column(name = "created_dt")
    private OffsetDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;

    @Column(name = "from_date")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private java.time.LocalDate fromDate;

    @Column(name = "to_date")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private java.time.LocalDate toDate;

    @Column(name = "reason_flag")
    private Boolean reasonFlag;

    // if true means then only calculate the cost for the question and add to the total cost of the Incentive
    @Column(name = "valid_answer")
    private String validAnswer;
}