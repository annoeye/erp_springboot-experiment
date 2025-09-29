package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.event.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.event.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.event.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.response.GetUserResponse;
import com.anno.ERP_SpringBoot_Experiment.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.event.device.SaveDeviceInfoListener;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
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
    private final ApplicationEventPublisher eventPublisher;
    private final SaveDeviceInfoListener deviceInfoEventListener;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
    private static final String OTP_FORMAT_PATTERN = "%0" + OTP_LENGTH + "d";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Value("${frontend.url}") private String frontendUrl;


    @Override
    @Transactional
    public RegisterResponse createUser(UserRegister body) {

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

        userRepository.findByName(body.getName())
                .ifPresent(existingUser -> {
                    if (!existingUser.getEmail().equals(body.getEmail())) {
                        throw new CustomException(
                                "Tên đăng nhập đã tồn tại với email khác. Hãy kiểm tra "
                                        + helper.maskEmail(existingUser.getEmail()) + ".",
                                HttpStatus.CONFLICT
                        );
                    }
                });

        Optional<User> optionalUser = userRepository.findByNameAndEmail(body.getName(), body.getEmail());

        User user;
        boolean someCondition;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            someCondition = true;
        } else {
            user = new User();
            user.setFullName(body.getFullName());
            user.setName(body.getName());
            user.setEmail(body.getEmail());
            user.setRoles(Collections.singleton(RoleType.USER));
            user.setPassword(passwordEncoder.encode(body.getPassword()));
            user.setStatus(ActiveStatus.INACTIVE);

            if (user.getAuditInfo() != null) {
                user.getAuditInfo().setCreatedAt(LocalDateTime.now());
            }
            someCondition = false;

            log.info("Tạo user mới: {}", user.getName());
        }

        user.getAuthCode().setCode(UUID.randomUUID().toString());
        user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
        user.getAuthCode().setPurpose(ActiveStatus.EMAIL_VERIFICATION);

        userRepository.save(user);

        eventPublisher.publishEvent(
                VerificationEmailEvent.builder()
                        .emailVerificationToken(user.getAuthCode().getCode())
                        .email(user.getEmail())
                        .username(user.getName())
                        .purpose(ActiveStatus.EMAIL_VERIFICATION)
                        .build()
        );
        return RegisterResponse.builder()
                .message(
                        someCondition
                                ? ("Email đã tồn tại nhưng chưa xác thực. Một email xác thực mới đã được gửi đến "
                                + helper.maskEmail(user.getEmail())
                                + ". Vui lòng kiểm tra.")
                                : ("Một email xác thực đã được gửi đến "
                                + helper.maskEmail(user.getEmail())
                                + ". Vui lòng kiểm tra.")
                )
                .build();

    }

    @Override
    @Transactional
    public AuthResponse loginUser(UserLogin body) {

        User user;
        String usernameOrEmail = body.getUsernameOrEmail();

        user =  userRepository.findByNameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new CustomException("Tên đăng nhập hoặc Email không tồn tại.", HttpStatus.NOT_FOUND));

        if (user.getStatus().equals(ActiveStatus.INACTIVE)) {
            if (user.getAuthCode().getCode() != null || user.getAuthCode().getExpiryDate() != null ) {
                user.getAuthCode().setCode(UUID.randomUUID().toString());
                user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
                user = userRepository.save(user);
                log.info("Tạo và gửi lại token xác thực cho user chưa active: {}", user.getUsername());
            }

            eventPublisher.publishEvent(
                    VerificationEmailEvent.builder()
                            .emailVerificationToken(user.getAuthCode().getCode())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .purpose(ActiveStatus.EMAIL_VERIFICATION)
                            .build()
            );

            return AuthResponse.builder()
                    .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến " + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                    .email(user.getEmail())
                    .build();
        }

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu không đúng.", HttpStatus.UNAUTHORIZED);
        }
        DeviceInfoResponse result = deviceInfoEventListener.handleDeviceInfo(
                new SaveDeviceInfo(
                        user,
                        body.getDeviceInfo(),
                        ActiveStatus.LOGIN_VERIFICATION)
        );


        return AuthResponse.builder()
                .message("Đăng nhập thành công.")
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(result.getAccessToken())
                .refreshToken(result.getFinalRefreshTokenString())
                .userId(String.valueOf(user.getId()))
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyAccount(String code, ActiveStatus type, AccountVerificationDto request) {

        User user = userRepository.findByAuthCode(code)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại để xác thực.", HttpStatus.NOT_FOUND));

        return switch (type) {
            case ActiveStatus.EMAIL_VERIFICATION -> {
                if (Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.EMAIL_VERIFICATION) {
                    user.getAuthCode().setCode(null);
                    user.getAuthCode().setExpiryDate(null);
                    user.getAuthCode().setPurpose(null);
                    user.setStatus(ActiveStatus.ACTIVE);

                    userRepository.save(user);
                    log.info("Xác thực tài khoản user chưa active thành công: {}", user.getUsername());
                } else {
                    yield ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendUrl + "/verify?token=" + type + "&status=failure"))
                            .build();
                }
                yield ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendUrl + "/verify?token=" + type))
                        .build();
            }
            case ActiveStatus.CHANGE_PASSWORD -> {
                if (Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.CHANGE_PASSWORD) {
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
                } else {
                    yield ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendUrl + "/verify?token=" + type + "&status=failure"))
                            .build();
                }
                yield ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendUrl + "/verify?token=" + type))
                        .build();
            }
            default -> throw new CustomException("Loại xác thực không hợp lệ: " + type, HttpStatus.BAD_REQUEST);
        };
    }


    @Override
    @Transactional
    public void sendCodeResetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        String code = String.format(OTP_FORMAT_PATTERN, SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
        user.getAuthCode().setCode(code);
        user.getAuthCode().setPurpose(ActiveStatus.EMAIL_VERIFICATION);
        user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        eventPublisher.publishEvent(
                SendCodeResetPassword.builder()
                        .user(user)
                        .code(code)
                        .build()
        );

        log.info("Đã gửi mã xác thực đổi mật khẩu cho người dùng: {}", user.getUsername());
    }

    @Override
    public ResponseEntity<?> getUser(ActiveStatus type) {
        return switch (type) {
            case ActiveStatus.GET_ALL -> {
                Page<User> users = userRepository.findAll(PageRequest.of(0, 10));
                yield ResponseEntity.status(HttpStatus.OK).body(helper.mapToResponse(users));

            }
            default -> throw new CustomException("Loại xác thực không hợp lệ: " + type, HttpStatus.BAD_REQUEST);
        };
    }
}
