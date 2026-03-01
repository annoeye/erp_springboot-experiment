package com.anno.ERP_SpringBoot_Experiment.service.event.device;

import com.anno.ERP_SpringBoot_Experiment.domainevent.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetails.UserDetailsServiceImpl;
import com.anno.ERP_SpringBoot_Experiment.service.event.base.BaseEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SaveDeviceInfoListener extends BaseEventListener {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;
    private static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 15;

    public SaveDeviceInfoListener(EmailService emailService, JwtService jwtService, UserDetailsServiceImpl userDetailsService, RedisService redisService, ObjectMapper objectMapper, @Value("${server.port}") String serverPort) {
        super(emailService, jwtService, userDetailsService);
        this.serverPort = serverPort;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public DeviceInfoResponse handleDeviceInfo(SaveDeviceInfo body) {
        DeviceInfo newDeviceInfo = body.deviceInfo();
        User user = body.userInfo();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String deviceId = createDeviceId(newDeviceInfo);
        String refreshTokenKey = "user:refresh_tokens:" + user.getId();

        Object existingTokenData = redisService.hGet(refreshTokenKey, deviceId);

        if (existingTokenData != null) {
            Map<String, Object> tokenMap = objectMapper.convertValue(existingTokenData, Map.class);
            DeviceInfo oldDeviceInfo = objectMapper.convertValue(tokenMap.get("deviceInfo"), DeviceInfo.class);

            if (!Objects.equals(oldDeviceInfo.getIpAddress(), newDeviceInfo.getIpAddress())) {
                log.info("Cập nhật IP cho thiết bị của user {}: {} -> {}", user.getUsername(), oldDeviceInfo.getIpAddress(), newDeviceInfo.getIpAddress());
                oldDeviceInfo.setIpAddress(newDeviceInfo.getIpAddress());
                tokenMap.put("deviceInfo", oldDeviceInfo);
                redisService.hSet(refreshTokenKey, deviceId, tokenMap);
            }
        }

        long accessTokenExpiryMs = TimeUnit.MINUTES.toMillis(ACCESS_TOKEN_EXPIRATION_MINUTES);
        String accessToken = jwtService.generateToken(userDetails, accessTokenExpiryMs);

        long refreshTokenExpiryMs = TimeUnit.DAYS.toMillis(REFRESH_TOKEN_EXPIRATION_DAYS);
        String refreshToken = jwtService.generateToken(userDetails, refreshTokenExpiryMs);

        Map<String, Object> refreshTokenData = new HashMap<>();
        refreshTokenData.put("token", refreshToken);
        refreshTokenData.put("deviceInfo", newDeviceInfo);

        redisService.hSet(refreshTokenKey, deviceId, refreshTokenData);
        redisService.getExpire(refreshTokenKey, TimeUnit.DAYS);

        log.info("Đã lưu/cập nhật Refresh Token cho user: {}, thiết bị: {}", user.getUsername(), deviceId);

        String accessTokenKey = "access_token:" + accessToken;
        redisService.setValueWithExpiry(accessTokenKey, user.getUsername(), ACCESS_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        log.info("Đã lưu Access Token cho user: {}", user.getUsername());

        return DeviceInfoResponse.builder().accessToken(accessToken).finalRefreshTokenString(refreshToken).build();
    }

    private String createDeviceId(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return "unknown_device";
        }
        String os = deviceInfo.getOsName() != null ? deviceInfo.getOsName().trim().toLowerCase() : "unknown_os";
        String type = deviceInfo.getDeviceType() != null ? deviceInfo.getDeviceType().trim().toLowerCase() : "unknown_type";
        return os + ":" + type;
    }
}