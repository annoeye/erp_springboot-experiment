package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo}
 */
@Value
public class AuditInfoDto implements Serializable {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    String createdBy;
    String updatedBy;
    String deletedBy;
}