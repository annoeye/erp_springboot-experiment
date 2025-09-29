package com.anno.ERP_SpringBoot_Experiment.service.event.device;

import com.anno.ERP_SpringBoot_Experiment.event.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.RefreshToken;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.RefreshTokenRepository;
import com.anno.ERP_SpringBoot_Experiment.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetailsServiceImpl;
import com.anno.ERP_SpringBoot_Experiment.service.UserService.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.event.base.BaseEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SaveDeviceInfoListener extends BaseEventListener {

    public SaveDeviceInfoListener(EmailService emailService,
                                  RefreshTokenRepository refreshTokenRepository,
                                  Helper helper,
                                  JwtService jwtService,
                                  UserDetailsServiceImpl userDetailsService,
                                  @Value("${server.port}") String serverPort) {
        super(emailService, refreshTokenRepository, helper, jwtService, userDetailsService);
        this.serverPort = serverPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public DeviceInfoResponse handleDeviceInfo(SaveDeviceInfo body) {
        DeviceInfo deviceInfo = body.deviceInfo();
        User user = body.userInfo();

        List<RefreshToken> userRefreshTokens = refreshTokenRepository.findAllByUserInfo(user);
        RefreshToken targetRefreshToken = null;
        boolean deviceMatchedInExistingToken = false;
        long currentRefreshTokenLifespanMillis = 2592000000L;
        long currentAccessTokenLifespanMillis = 900000L;
        String finalRefreshTokenString;

        for (RefreshToken token : userRefreshTokens) {
            if (token.getDeviceInfos() != null && token.getAuthCode().getPurpose() == body.purpose()) {
                for (DeviceInfo existingDi : token.getDeviceInfos()) {
                    if (helper.areDeviceInfoMatching(existingDi, deviceInfo)) {
                        targetRefreshToken = token;
                        if (!Objects.equals(existingDi.getIpAddress(), deviceInfo.getIpAddress())) {
                            existingDi.setIpAddress(deviceInfo.getIpAddress());
                            log.debug("Cập nhật IP cho thiết bị của user {}: {} -> {}", user.getUsername(), existingDi.getDeviceType(), deviceInfo.getIpAddress());
                        }
                        deviceMatchedInExistingToken = true;
                        break;
                    }
                }
                if (deviceMatchedInExistingToken) break;
            }
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        if (deviceMatchedInExistingToken) {
            log.info("Thiết bị cũ đăng nhập cho user: {}. Làm mới refresh token ID: {}", user.getUsername(), targetRefreshToken.getId());
            targetRefreshToken.getAuthCode().setCode(jwtService.generateToken(userDetails, currentRefreshTokenLifespanMillis));
            targetRefreshToken.getAuthCode().setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(currentRefreshTokenLifespanMillis)));
            refreshTokenRepository.save(targetRefreshToken);
            finalRefreshTokenString = targetRefreshToken.getAuthCode().getCode();
        } else {
            log.info("Thiết bị mới đăng nhập cho user: {}. Tạo refresh token mới.", user.getUsername());
            RefreshToken newRefreshToken = new RefreshToken();
            newRefreshToken.setUserInfo(user);
            newRefreshToken.getAuthCode().setCode(jwtService.generateToken(userDetails, currentRefreshTokenLifespanMillis));
            newRefreshToken.getAuthCode().setPurpose(ActiveStatus.LOGIN_VERIFICATION);
            newRefreshToken.getAuthCode().setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(currentRefreshTokenLifespanMillis)));
            newRefreshToken.setDeviceInfos(Collections.singletonList(new DeviceInfo(deviceInfo)));
            refreshTokenRepository.save(newRefreshToken);
            finalRefreshTokenString = newRefreshToken.getAuthCode().getCode();
        }

        log.info("Đăng nhập thành công cho user: {}", user.getUsername());
        String accessToken = jwtService.generateToken(userDetails, currentAccessTokenLifespanMillis);

        return DeviceInfoResponse
                .builder()
                .accessToken(accessToken)
                .finalRefreshTokenString(finalRefreshTokenString)
                .build();
    }
}
