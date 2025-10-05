package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuthCode;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken extends IdentityOnly {

    User userInfo;

    AuthCode authCode = new AuthCode();

    List<DeviceInfo> deviceInfos;
}
