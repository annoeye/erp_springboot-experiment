package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.event.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.event.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.event.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.RefreshToken;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.RefreshTokenRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetailsServiceImpl;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class Event {

    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Helper helper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    @Value("${server.port}") private String serverPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerificationEmailEvent body) {
        try {
            String verificationUrl = "http://localhost:" + serverPort + "/api/auth/verify?token=" + body.emailVerificationToken() + "&username=" +  body.username() + "&purpose=" + body.purpose();
            emailService.sendVerificationEmail(
                    body.email(),
                    body.username(),
                    verificationUrl
            );
        }catch (MessagingException e) {
            log.error("Gửi email xác thực thất bại cho user: {}", body.username(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public DeviceInfoResponse handleDeviceInfo(SaveDeviceInfo body) {

        DeviceInfo deviceInfo = body.deviceInfo();
        User user = body.userInfo();

        List<RefreshToken> userRefreshTokens = refreshTokenRepository.findAllByUserInfo(body.userInfo());
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
            newRefreshToken.getAuthCode().setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(currentRefreshTokenLifespanMillis)));
            newRefreshToken.setDeviceInfos(Collections.singletonList(new DeviceInfo(deviceInfo)));
            refreshTokenRepository.save(newRefreshToken);
            finalRefreshTokenString = newRefreshToken.getAuthCode().getCode();
        }
        log.info("Đăng nhập thành công cho user: {}", user.getUsername());
        String accessToken = jwtService.generateToken(userDetails, currentAccessTokenLifespanMillis);
        // log
        log.info("Đã lưu thông tin người đăng nhập vào lịch sử hoạt động");

        return DeviceInfoResponse
                .builder()
                .accessToken(accessToken)
                .finalRefreshTokenString(finalRefreshTokenString)
                .build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handSendCodeResetPassword(SendCodeResetPassword body) throws MessagingException {
        User user = body.user();
        String code = body.code();
        try {
            emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getUsername(), code);
            log.info("Đã gửi mã OTP đặt lại mật khẩu cho user: {}", user.getUsername());
        } catch (MessagingException e) {
            log.error("Lỗi gửi email OTP đặt lại mật khẩu cho {}: {}", user.getEmail(), e.getMessage(), e);
            throw new MessagingException("Lỗi gửi email đặt lại mật khẩu: " + e.getMessage(), e);
        }
    }
}
