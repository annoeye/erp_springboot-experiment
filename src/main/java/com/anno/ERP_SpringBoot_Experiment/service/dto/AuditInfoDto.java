package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditEntry;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo}
 */
@Value
public class AuditInfoDto implements Serializable {
    LocalDateTime createdAt;
    String createdBy;
    LocalDateTime updatedAt;
    List<AuditEntry> updateHistory;
    LocalDateTime deletedAt;
    String deletedBy;
}