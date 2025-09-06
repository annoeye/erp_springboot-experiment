package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.event.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.event.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.event.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements iUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Helper helper;
    private final UserDetailsService userDetailsService;
    private final Event userEvent;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
    private static final String OTP_FORMAT_PATTERN = "%0" + OTP_LENGTH + "d";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    @Override
    @Transactional
    public String createUser(UserRegister body) {

        if (!helper.isEmailFormat(body.getEmail())) {
            throw new CustomException("Email không đúng định dạng", HttpStatus.BAD_REQUEST);
        }
        if (!body.getPassword().equals(body.getConfirmPassword())) {
            throw new CustomException("Mật khẩu không khớp", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByEmail(body.getEmail())
                .filter(u -> u.getStatus() == ActiveStatus.ACTIVE)
                .ifPresent(u -> {
                    throw new CustomException("Email đã tồn tại.", HttpStatus.CONFLICT);
                });

        userRepository.findByUsername(body.getUserName())
                .ifPresent(existingUser -> {
                    if (!existingUser.getEmail().equals(body.getEmail())) {
                        throw new CustomException(
                                "Tên đăng nhập đã tồn tại với email khác. Hãy kiểm tra "
                                        + helper.maskEmail(existingUser.getEmail()) + ".",
                                HttpStatus.CONFLICT
                        );
                    }
                });

        User user = userRepository.findByUsernameAndEmail(body.getUserName(), body.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setFullName(body.getFullName());
                    newUser.setName(body.getUserName());
                    newUser.setEmail(body.getEmail());
                    newUser.setRoles(body.getRoles());
                    newUser.setPassword(passwordEncoder.encode(body.getPassword()));
                    newUser.setStatus(ActiveStatus.INACTIVE);

                    if (newUser.getAuditInfo() != null) {
                        newUser.getAuditInfo().setCreatedBy(body.getCreatedBy());
                    }

                    log.info("Tạo user mới: {}", newUser.getUsername());
                    return newUser;
                });

        user.getAuthCode().setCode(
                helper.createShortToken(
                        userDetailsService.loadUserByUsername(user.getUsername()),
                        5 * 60 * 1000L
                )
        );
        user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
        user.getAuthCode().setPurpose(ActiveStatus.EMAIL_VERIFICATION);

        userRepository.save(user);

        userEvent.handleVerificationEmail(
                VerificationEmailEvent.builder()
                        .emailVerificationToken(user.getAuthCode().getCode())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .purpose(ActiveStatus.EMAIL_VERIFICATION)
                        .build()
        );

        return (user.getAuditInfo().getCreatedBy() != null) ?
                "Email đã tồn tại nhưng chưa xác thực. Một email xác thực mới đã được gửi đến "
                        + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra." :
                "Một email xác thực đã được gửi đến "
                        + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.";
    }


    @Override
    @Transactional
    public AuthResponse loginUser(UserLogin body) {

        User user;
        String usernameOrEmail = body.getUsernameOrEmail();
        Optional<User> foundUserByUsername = userRepository.findByUsername(usernameOrEmail);

        user = foundUserByUsername.orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new CustomException("Tên đăng nhập hoặc Email không tồn tại.", HttpStatus.NOT_FOUND)));

        if (user.getStatus().equals(ActiveStatus.INACTIVE)) {
            if (user.getAuthCode().getCode() == null || user.getAuthCode().getExpiryDate() == null || user.getAuthCode().getExpiryDate().isBefore(LocalDateTime.now())) {
                UserDetails tempUserDetailsForToken = org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .build();
                String newEmailToken = helper.createShortToken(tempUserDetailsForToken, 300000L);
                log.debug("DEBUG: Token được tạo mới: {}", newEmailToken);

                user.getAuthCode().setCode(newEmailToken);
                user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
                user = userRepository.save(user);
                log.info("Tạo và gửi lại token xác thực cho user chưa active: {}", user.getUsername());
                log.debug("DEBUG: Token sau khi lưu vào DB (từ đối tượng user): {}",user.getAuthCode());
            } else {
                log.info("User {} đã có token xác thực còn hiệu lực.", user.getUsername());
                log.debug("DEBUG: Token hiện tại của user (còn hiệu lực): {}",user.getAuthCode());
            }

            userEvent.handleVerificationEmail(
                    VerificationEmailEvent.builder()
                            .emailVerificationToken(user.getAuthCode().getCode())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .purpose(ActiveStatus.EMAIL_VERIFICATION)
                            .build()
            );
            log.debug("DEBUG: Token trích xuất từ URL: {}",user.getAuthCode());

            return AuthResponse.builder()
                    .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến " + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(String.valueOf(user.getId()))
                    .build();
        }

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu không đúng.", HttpStatus.UNAUTHORIZED);
        }

        DeviceInfoResponse deviceInfoResponse = userEvent.handleDeviceInfo(
                SaveDeviceInfo.builder()
                        .userInfo(user)
                        .deviceInfo(body.getDeviceInfo())
                        .purpose(ActiveStatus.EMAIL_VERIFICATION)
                        .build()
        );

        return AuthResponse.builder()
                .accessToken(deviceInfoResponse.getAccessToken())
                .refreshToken(deviceInfoResponse.getFinalRefreshTokenString())
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(String.valueOf(user.getId()))
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .build();
    }

    @Override
    @Transactional
    public void verifyAccount(String code, ActiveStatus type, AccountVerificationDto request) {

        User user = userRepository.findByAuthCode_Code(code)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại để xác thực.", HttpStatus.NOT_FOUND));

        switch (type) {
            case ActiveStatus.EMAIL_VERIFICATION:
                if (Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate() != null &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.EMAIL_VERIFICATION)
                {
                    user.getAuthCode().setCode(null);
                    user.getAuthCode().setExpiryDate(null);
                    user.getAuthCode().setPurpose(null);
                    user.setStatus(ActiveStatus.ACTIVE);


                    userRepository.save(user);
                    log.info("Xác thực tài khoản user chưa active thành công: {}", user.getUsername());
                } else {
                    throw new CustomException("Xác thực thất bại.", HttpStatus.BAD_REQUEST);
                }
                break;

            case ActiveStatus.CHANGE_PASSWORD:
                if (Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate() != null &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.CHANGE_PASSWORD)
                {
                    user.getAuthCode().setCode(null);
                    user.getAuthCode().setExpiryDate(null);
                    user.getAuthCode().setPurpose(null);

                    if (request.getNewPassword().isEmpty() || request.getConfirmPassword().isEmpty()) {
                        throw new CustomException("Mật khẩu hoặc xác thực mật khẩu đang để trống.", HttpStatus.BAD_REQUEST);
                    }

                    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                        throw new CustomException("Mật khẩu xác nhận không trùng khớp.", HttpStatus.BAD_REQUEST);
                    }

                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);
                    log.info("Đổi mật khẩu thành công cho user: {}", user.getUsername());
                }
                else {
                    throw new CustomException("Mã xác thực mật khẩu không hợp lệ.", HttpStatus.BAD_REQUEST);
                }
                break;

            default:
                throw new CustomException("Loại xác thực không hợp lệ: " + type, HttpStatus.BAD_REQUEST);
        }
    }


    @Override
    @Transactional
    public void sendCodeResetPassword(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        String code = String.format(OTP_FORMAT_PATTERN, SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
        user.getAuthCode()
                .authCode(
                        code,
                        ActiveStatus.CHANGE_PASSWORD,
                        LocalDateTime.now().plusMinutes(5)
                );
        userRepository.save(user);
        userEvent.handSendCodeResetPassword(SendCodeResetPassword
                .builder()
                .user(user)
                .code(code)
                .build());
        log.info("Đã gửi mã xác thực đổi mật khẩu cho người dùng: {}", user.getUsername());
    }
}