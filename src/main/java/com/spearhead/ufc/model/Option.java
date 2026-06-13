/**
 * 
 */
package com.spearhead.ufc.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author manikandan.m Each questions options
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "option", schema = "compliance")
public class Option {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "option_id")
	private int optionId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;

	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	private QuestionBank question;

	@Column(name = "option_value")
	private String optionValue;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "created_dt")
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private Integer updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;
}
