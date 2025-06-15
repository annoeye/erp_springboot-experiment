package com.anno.ERP_SpringBoot_Experiment.model.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceInfo {
    String deviceType;
    String osName;
    String osVersion;
    String browserName;
    String browserVersion;
    int screenWidth;
    int screenHeight;
    String userAgent;
    String ipAddress;
    String language;
    String timeZone;
    String deviceId;
}