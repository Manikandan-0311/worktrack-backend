package com.spearhead.ufc.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.spearhead.ufc.dto.OptionDTO;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_question_mapping", schema = "compliance", uniqueConstraints = {
        @UniqueConstraint(name = "uq_role_question", columnNames = { "org_id", "role_id", "question_id" })
})
public class RoleQuestionMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_question_map_id")
    private Integer roleQuestionMapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank question;

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

    @Transient
    private String[] options;

    @Transient
    private List<OptionDTO> optionDetails;

    @Transient
    private List<Integer> deletedOptionIds;

    // Legacy flat ID fields for request compatibility
    @Transient
    private Integer orgId;

    @Transient
    private Integer roleId;

    // Support assigning a question to multiple roles in one request
    @Transient
    private List<Integer> roleIds;

    @Transient
    private Integer questionId;

    @Transient
    private Integer locationId;

    @Transient
    private String questionText;

    @Transient
    private String questionType;

    @Transient
    private Double weightage;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;

    @Transient
    private String createdEmployeeName;

    @Transient
    private String updatedEmployeeName;

    @Transient
    private Boolean reasonFlag;

    @Transient
    private Object validAnswer;

    @JsonSetter("options")
    public void setOptionsFromJson(JsonNode rawOptions) {
        if (rawOptions == null || rawOptions.isNull()) {
            this.options = null;
            this.deletedOptionIds = null;
            return;
        }

        List<String> normalizedOptions = new ArrayList<>();
        List<Integer> deletedIds = new ArrayList<>();
        if (rawOptions.isArray()) {
            for (JsonNode node : rawOptions) {
                if (node == null || node.isNull()) {
                    continue;
                }
                if (node.isTextual() || node.isNumber() || node.isBoolean()) {
                    String val = node.asText();
                    if (val != null && !val.isBlank()) {
                        normalizedOptions.add(val.trim());
                    }
                    continue;
                }
                if (node.isObject()) {
                    JsonNode deletedNode = node.get("isDeleted");
                    boolean isDeleted = deletedNode != null && deletedNode.asBoolean(false);
                    if (isDeleted) {
                        JsonNode optionIdNode = node.get("optionId");
                        if (optionIdNode != null && optionIdNode.canConvertToInt()) {
                            deletedIds.add(optionIdNode.asInt());
                        }
                        continue;
                    }
                    JsonNode valueNode = node.get("optionValue");
                    if (valueNode == null || valueNode.isNull()) {
                        valueNode = node.get("value");
                    }
                    if (valueNode != null && !valueNode.isNull()) {
                        String val = valueNode.asText();
                        if (val != null && !val.isBlank()) {
                            normalizedOptions.add(val.trim());
                        }
                    }
                }
            }
        } else if (rawOptions.isObject()) {
            JsonNode valueNode = rawOptions.get("optionValue");
            if (valueNode == null || valueNode.isNull()) {
                valueNode = rawOptions.get("value");
            }
            if (valueNode != null && !valueNode.isNull()) {
                String val = valueNode.asText();
                if (val != null && !val.isBlank()) {
                    normalizedOptions.add(val.trim());
                }
            }
        } else {
            String val = rawOptions.asText();
            if (val != null && !val.isBlank()) {
                normalizedOptions.add(val.trim());
            }
        }

        this.options = normalizedOptions.toArray(String[]::new);
        this.deletedOptionIds = deletedIds.isEmpty() ? null : deletedIds;
    }

}
