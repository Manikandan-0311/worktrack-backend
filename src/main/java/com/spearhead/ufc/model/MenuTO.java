package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "menu", schema = "base")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class MenuTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "menu_name", nullable = false, unique = true)
    private String menuName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_dt")
    private OffsetDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;
}
