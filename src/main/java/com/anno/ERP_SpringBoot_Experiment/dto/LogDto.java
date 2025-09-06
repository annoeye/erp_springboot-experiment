package com.anno.ERP_SpringBoot_Experiment.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class LogDto {
    ActiveStatus status;
    User performedBy;
    UUID targetId;
    String description;

    public LogDto(ActiveStatus status, User performedBy, UUID targetId, String description) {
        if (this.description == null) this.description = null;
        else this.description = description;
        if (this.targetId == null) this.targetId = null;
        else this.targetId = targetId;
        this.status = status;
        this.performedBy = performedBy;
    }
}
