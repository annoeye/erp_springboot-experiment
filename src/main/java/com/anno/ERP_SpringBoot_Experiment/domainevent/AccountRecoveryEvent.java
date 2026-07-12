package com.anno.ERP_SpringBoot_Experiment.domainevent;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import lombok.Builder;

@Builder
public record AccountRecoveryEvent(User user, String token) {}
