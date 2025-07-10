package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.entity.UserActionLog;
import com.anno.ERP_SpringBoot_Experiment.repository.UserActionLogRepository;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUserActionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserActionLogService implements iUserActionLog {

    private final UserActionLogRepository userActionLogRepository;

    @Override
    public void log(User user, UserActionLog.ActionType action, Long targetId, String description, String targetType) {
        UserActionLog log = UserActionLog.builder()
                .user(user)
                .actionType(action)
                .targetId(targetId)
                .targetType(targetType)
                .createdAt(LocalDateTime.now())
                .description(description)
                .build();
        userActionLogRepository.save(log);
    }
}
