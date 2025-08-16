package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditInfo {

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "created_by", updatable = false)
    String createdBy;
    @Column(name = "updated_by")
    String updatedBy;
    @Column(name = "deleted_by")
    String deletedBy;
}
