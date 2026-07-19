package com.anno.ERP_SpringBoot_Experiment.service.accountrecovery;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;

public record RecoveryToken(User user, String token, String email) {
}
