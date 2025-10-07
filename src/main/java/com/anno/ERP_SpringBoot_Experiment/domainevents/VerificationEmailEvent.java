package com.anno.ERP_SpringBoot_Experiment.domainevents;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.Builder;

@Builder
public record VerificationEmailEvent(String email, String username, String emailVerificationToken, ActiveStatus purpose) {}

