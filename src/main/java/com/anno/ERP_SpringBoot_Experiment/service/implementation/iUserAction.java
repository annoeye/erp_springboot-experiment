package com.anno.ERP_SpringBoot_Experiment.service.implementation;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Log;

import java.util.List;

public interface iUserAction {
    void log(User user, Log.ActionType action, String targetId, String description, String targetType);
    String createAccount(List<String> roleName);
}
