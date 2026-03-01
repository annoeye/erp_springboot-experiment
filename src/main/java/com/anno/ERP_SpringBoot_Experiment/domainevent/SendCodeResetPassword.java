package com.anno.ERP_SpringBoot_Experiment.domainevent;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import lombok.Builder;

@Builder
public record SendCodeResetPassword(User user, String code) {}
