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
@Table(name = "role_menu_access", schema = "base")
public class RoleMenuAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_menu_access_id")
    private Integer roleMenuAccessId;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_dt")
    private OffsetDateTime createdDt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;

    @ManyToOne
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuTO menu;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
