package com.anno.ERP_SpringBoot_Experiment.domainevents;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.Builder;

@Builder
public record SaveDeviceInfo(User userInfo, DeviceInfo deviceInfo, ActiveStatus purpose) {}
