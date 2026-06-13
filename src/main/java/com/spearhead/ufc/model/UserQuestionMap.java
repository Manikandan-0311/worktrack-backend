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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author manikandan.m user and question mapping table
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_question_mapping", schema = "compliance")
public class UserQuestionMap {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_question_map_id")
	private Integer userQuestionMapId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

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
