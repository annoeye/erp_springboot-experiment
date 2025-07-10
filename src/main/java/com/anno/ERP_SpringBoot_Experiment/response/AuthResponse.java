package com.anno.ERP_SpringBoot_Experiment.response;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

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
    User.Gender gender;
    List<String> roles;
    LocalDateTime accessTokenExpiry;
    LocalDateTime refreshTokenExpiry;
}
