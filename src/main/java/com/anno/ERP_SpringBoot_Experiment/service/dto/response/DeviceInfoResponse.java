package com.anno.ERP_SpringBoot_Experiment.service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceInfoResponse {
    String finalRefreshTokenString;
    String accessToken;
}
