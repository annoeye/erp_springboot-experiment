package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.common.constants.AppConstant;
import com.anno.ERP_SpringBoot_Experiment.domainevents.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.domainevents.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.domainevents.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.mapper.UserMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.KafkaService.ActiveLogService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.event.device.SaveDeviceInfoListener;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iUser;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
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
    @Value("${frontend.url}")
    private String frontendUrl;
    private final UserMapper userMapper;
    private final ActiveLogService activeLogService;
    private final RedisService redisService;


    @Override
    @Transactional
    public Response<RegisterResponse> createUser(UserRegisterRequest body) {

        if (!helper.isEmailFormat(body.getEmail())) {
            throw new BusinessException("Email không đúng định dạng");
        }
        if (!body.getPassword().equals(body.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu không khớp");
        }

        userRepository.findByEmail(body.getEmail()).filter(u -> u.getStatus() == ActiveStatus.ACTIVE).ifPresent(u -> {
            throw new BusinessException("Email đã tồn tại.");
        });

        userRepository.findByName(body.getName()).ifPresent(existingUser -> {
            if (!existingUser.getEmail().equals(body.getEmail())) {
                throw new BusinessException("Tên đăng nhập đã tồn tại với email khác. Hãy kiểm tra " + helper.maskEmail(existingUser.getEmail()) + " nếu là bạn.");
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
                        .build());
        return Response.ok(RegisterResponse.builder()
                .message(someCondition
                        ? ("Email đã tồn tại nhưng chưa xác thực. Một email xác thực mới đã được gửi đến " + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                        : ("Một email xác thực đã được gửi đến " + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")).build());
    }

    @Override
    @Transactional
    public Response<AuthResponse> loginUser(final UserLoginRequest body) {

        User user;
        String usernameOrEmail = body.getUsernameOrEmail();

        user = userRepository.findByNameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new BusinessException(AppConstant.NOT_FOUND.getCode(), "Tên đăng nhập hoặc Email không tồn tại."));

        if (user.getStatus().equals(ActiveStatus.INACTIVE)) {
            if (user.getAuthCode().getCode() != null || user.getAuthCode().getExpiryDate() != null) {
                user.getAuthCode().setCode(UUID.randomUUID().toString());
                user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
                user = userRepository.save(user);
                log.info("Tạo và gửi lại token xác thực cho user chưa active: {}", user.getUsername());
            }

            eventPublisher.publishEvent(VerificationEmailEvent.builder()
                    .emailVerificationToken(user.getAuthCode().getCode()).email(user.getEmail())
                    .username(user.getUsername())
                    .purpose(ActiveStatus.EMAIL_VERIFICATION)
                    .build());

            return Response.ok(AuthResponse.builder()
                    .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến " + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                    .email(user.getEmail())
                    .build());
        }

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Mật khẩu không đúng.");
        }
        DeviceInfoResponse result = deviceInfoEventListener.handleDeviceInfo(new SaveDeviceInfo(user, body.getDeviceInfo(), ActiveStatus.LOGIN_VERIFICATION));

//        ActiveLogDto dto = ActiveLogDto.builder()
//                .performedBy(String.valueOf(user.getId()))
//                .status(ActiveStatus.LOGIN)
//                .build();
//        activeLogService.sendMessage(dto);

        return Response.ok(AuthResponse.builder()
                .message("Đăng nhập thành công.")
                .username(user.getUsername()).email(user.getEmail())
                .accessToken(result.getAccessToken())
                .refreshToken(result.getFinalRefreshTokenString())
                .userId(String.valueOf(user.getId()))
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .build());
    }

    @Override
    @Transactional
    public Response<?> verifyAccount(
            @NonNull final String code,
            @NonNull final ActiveStatus type,
            final AccountVerificationRequest request)
    {

        User user = userRepository.findByAuthCode(code)
                .orElseThrow(() -> new BusinessException(AppConstant.NOT_FOUND.getCode(), "Người dùng không tồn tại để xác thực."));

        return switch (type) {

            case ActiveStatus.EMAIL_VERIFICATION -> {

                boolean isCodeValid = Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.EMAIL_VERIFICATION;

                if (isCodeValid) {
                    user.getAuthCode().setCode(null);
                    user.getAuthCode().setExpiryDate(null);
                    user.getAuthCode().setPurpose(null);
                    user.setStatus(ActiveStatus.ACTIVE);

                    userRepository.save(user);
                    String local = frontendUrl + "/verify?token=" + type;
                    log.info("Xác thực tài khoản user chưa active thành công: {}", user.getUsername());

                    yield Response.found(local);
                } else {
                    throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Mã xác thực email không hợp lệ hoặc đã hết hạn.");
                }
            }

            case ActiveStatus.CHANGE_PASSWORD -> {

                boolean isCodeValid = Objects.equals(code, user.getAuthCode().getCode()) &&
                        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
                        user.getAuthCode().getPurpose() == ActiveStatus.CHANGE_PASSWORD;

                if (isCodeValid) {
                    if (request == null || request.getNewPassword() == null || request.getConfirmPassword() == null) {
                        throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Dữ liệu mật khẩu mới bị thiếu.");
                    }
                    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                        throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Mật khẩu xác nhận không trùng khớp.");
                    }

                    user.getAuthCode().setCode(null);
                    user.getAuthCode().setExpiryDate(null);
                    user.getAuthCode().setPurpose(null);
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

                    userRepository.save(user);
                    String local = frontendUrl + "/verify?token=" + type;
                    log.info("Đổi mật khẩu thành công cho user: {}", user.getUsername());
                    yield Response.found(local);
                } else {
                    throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Mã đổi mật khẩu không hợp lệ hoặc đã hết hạn.");
                }
            }

            default -> throw new BusinessException(AppConstant.BAD_REQUEST.getCode(), "Loại xác thực không hợp lệ: " + type);
        };
    }


    @Override
    @Transactional
    public Response<?> sendCodeResetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AppConstant.NOT_FOUND.getCode(), "Người dùng không tồn tại"));

        String code = String.format(OTP_FORMAT_PATTERN, SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
        user.getAuthCode().setCode(code);
        user.getAuthCode().setPurpose(ActiveStatus.CHANGE_PASSWORD);
        user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        eventPublisher.publishEvent(SendCodeResetPassword.builder().user(user).code(code).build());

        log.info("Đã gửi mã xác thực đổi mật khẩu cho người dùng: {}", user.getUsername());

        return Response.ok("Mã xác thực đổi mật khẩu đã được gửi đến " + helper.maskEmail(email) + ". Vui lòng kiểm tra.");
    }

    @Override
    public Page<UserDto> search(@NonNull final UserSearchRequest request) {
        return userRepository.findAll(request.specification(), request.getPaging()
                .pageable()).map(userMapper::toDto);
    }


    @Override
    public void logoutUser(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        jwt = authHeader.substring(7);
        String accessTokenKey = "access_token:" + jwt;

        if (redisService.hasKey(accessTokenKey)) {
            redisService.delete(accessTokenKey);
            log.info("Người dùng đã đăng xuất.");
        }
    }
}
