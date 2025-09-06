package com.anno.ERP_SpringBoot_Experiment.response;

import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
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
    Set<RoleType> roles = new HashSet<>();
    LocalDateTime accessTokenExpiry;
    LocalDateTime refreshTokenExpiry;
}
