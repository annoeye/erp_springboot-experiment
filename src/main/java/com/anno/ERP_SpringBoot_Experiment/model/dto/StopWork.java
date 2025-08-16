package com.anno.ERP_SpringBoot_Experiment.model.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Log;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class StopWork {

    String targetUser;
    String handledByUserName;
    String reason;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Log.ActionType actionType;


}
