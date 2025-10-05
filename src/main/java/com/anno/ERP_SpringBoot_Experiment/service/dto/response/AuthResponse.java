package com.anno.ERP_SpringBoot_Experiment.service.dto.response;

import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {
    String message;
    String accessToken;
    String refreshToken;
    String username;
    String avatarUrl;
    String email;
    String userId;
    String phoneNumber;
    Gender gender;
    @Builder.Default
    Set<RoleType> roles = new HashSet<>();
}
