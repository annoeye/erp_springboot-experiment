package com.anno.ERP_SpringBoot_Experiment.model.base;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;

public interface Auditable {
    AuditInfo getAuditInfo();
}
