package com.anno.ERP_SpringBoot_Experiment.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private String username;
    private String email;
    private Long userId;
    private List<String> roles;
    private LocalDateTime accessTokenExpiry;
    private LocalDateTime refreshTokenExpiry;
}
