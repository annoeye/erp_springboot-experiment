package com.anno.ERP_SpringBoot_Experiment.service.implementation;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.entity.UserActionLog;

public interface iUserActionLog {
    void log(User user, UserActionLog.ActionType action, String targetId, String description, String targetType);
}
