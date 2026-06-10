package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.domainevent.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.domainevent.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.domainevent.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.mapper.UserMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.KafkaService.ActiveLogService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.DeviceInfoService;
import com.anno.ERP_SpringBoot_Experiment.service.RefreshTokenService;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iUser;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
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
  private final DeviceInfoService deviceInfoService;
  private final RefreshTokenService refreshTokenService;
  private final UserDetailsService userDetailsService;
  private static final int OTP_LENGTH = 6;
  private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
  private static final String OTP_FORMAT_PATTERN = "%0" + OTP_LENGTH + "d";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  @Value("${frontend.url}")
  private String frontendUrl;
  private final UserMapper userMapper;
  private final ActiveLogService activeLogService;
  private final RedisService redisService;
  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public Response<RegisterResponse> createUser(UserRegisterRequest body) {

    if (!helper.isEmailFormat(body.getEmail())) {
      throw new BusinessException(ErrorCode.INVALID_FORMAT, "Email không đúng định dạng");
    }
    if (!body.getPassword().equals(body.getConfirmPassword())) {
      throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mật khẩu không khớp");
    }

    userRepository.findByEmail(body.getEmail()).filter(u -> u.getStatus() == ActiveStatus.ACTIVE).ifPresent(u -> {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email đã tồn tại.");
    });

    userRepository.findByName(body.getName()).ifPresent(existingUser -> {
      if (!existingUser.getEmail().equals(body.getEmail())) {
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Tên đăng nhập đã tồn tại với email khác.")
            .with("email", helper.maskEmail(existingUser.getEmail()));
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
      user.setEmail(body.getEmail());
      user.setRoles(Collections.singleton(RoleType.USER));
      user.setPassword(passwordEncoder.encode(body.getPassword()));
      user.setStatus(ActiveStatus.INACTIVE);

      user.setCreatedAt(LocalDateTime.now());
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
            ? String.format(
                "Email đã tồn tại nhưng chưa xác thực. Một email xác thực mới đã được gửi đến %s. Vui lòng kiểm tra.",
                helper.maskEmail(user.getEmail()))
            : String.format("Một email xác thực đã được gửi đến %s. Vui lòng kiểm tra.",
                helper.maskEmail(user.getEmail())))
        .build());
  }

  @Override
  @Transactional
  public Response<AuthResponse> loginUser(final UserLoginRequest body) {

    User user;
    String usernameOrEmail = body.getUsernameOrEmail();

    user = userRepository.findByNameOrEmail(usernameOrEmail)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
            "Tên đăng nhập hoặc Email không tồn tại."));

    if (user.getStatus().equals(ActiveStatus.INACTIVE)) { // check status
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
      ;

      return Response.loginResponse(HttpStatus.UNAUTHORIZED,
          AuthResponse.builder()
              .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến "
                  + helper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
              .email(user.getEmail())
              .build());
    }

    if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu không đúng.");
    }
    String deviceId = deviceInfoService.createDeviceId(body.getDeviceInfo());
    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    DeviceInfoResponse result = refreshTokenService.handleLoginTokens(user, userDetails, body.getDeviceInfo(), deviceId);

    return Response.ok(AuthResponse.builder()
        .message(result.getMessage() != null ? result.getMessage() : "Đăng nhập thành công.")
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
  public Response<String> verifyEmail(@NonNull final String code) {

    User user = userRepository.findByAuthCode(code)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại để xác thực."));

    boolean isCodeValid = Objects.equals(code, user.getAuthCode().getCode()) &&
        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
        user.getAuthCode().getPurpose() == ActiveStatus.EMAIL_VERIFICATION;

    if (isCodeValid) {
      user.getAuthCode().setCode(null);
      user.getAuthCode().setExpiryDate(null);
      user.getAuthCode().setPurpose(null);
      user.setStatus(ActiveStatus.ACTIVE);

      userRepository.save(user);
      log.info("Xác thực email thành công cho user: {}", user.getUsername());

      return Response.ok("Xác thực email thành công. Tài khoản của bạn đã được kích hoạt.");
    } else {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Mã xác thực email không hợp lệ hoặc đã hết hạn.");
    }
  }

  @Override
  @Transactional
  public Response<String> resetPassword(
      @NonNull final String code,
      @NonNull final AccountVerificationRequest request) {

    User user = userRepository.findByAuthCode(code)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại để xác thực."));

    boolean isCodeValid = Objects.equals(code, user.getAuthCode().getCode()) &&
        user.getAuthCode().getExpiryDate().isAfter(LocalDateTime.now()) &&
        user.getAuthCode().getPurpose() == ActiveStatus.CHANGE_PASSWORD;

    if (!isCodeValid) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Mã đổi mật khẩu không hợp lệ hoặc đã hết hạn.");
    }

    if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Dữ liệu mật khẩu mới bị thiếu.");
    }
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Mật khẩu xác nhận không trùng khớp.");
    }

    user.getAuthCode().setCode(null);
    user.getAuthCode().setExpiryDate(null);
    user.getAuthCode().setPurpose(null);
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

    userRepository.save(user);
    log.info("Đổi mật khẩu thành công cho user: {}", user.getUsername());

    return Response.ok("Mật khẩu đã được thay đổi thành công.");
  }

  @Override
  @Transactional
  public Response<String> sendCodeResetPassword(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));

    String code = String.format(OTP_FORMAT_PATTERN, SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
    user.getAuthCode().setCode(code);
    user.getAuthCode().setPurpose(ActiveStatus.CHANGE_PASSWORD);
    user.getAuthCode().setExpiryDate(LocalDateTime.now().plusMinutes(5));
    userRepository.save(user);

    eventPublisher.publishEvent(SendCodeResetPassword.builder().user(user).code(code).build());

    log.info("Đã gửi mã xác thực đổi mật khẩu cho người dùng: {}", user.getUsername());

    return Response
        .ok("Mã xác thực đổi mật khẩu đã được gửi đến " + helper.maskEmail(email) + ". Vui lòng kiểm tra.");
  }

  // @Override
  // public Page<UserDto> search(@NonNull final UserSearchRequest request) {
  // return userRepository.findAll(request.specification(), request.getPaging()
  // .pageable()).map(userMapper::toDto);
  // }

  @Override
  @Transactional
  public Response<AuthResponse> refreshToken(@NonNull final RefreshTokenRequest request) {
    final String refreshToken = request.getRefreshToken();
    final String username = jwtService.extractUsername(refreshToken);

    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
            "Người dùng không tồn tại."));

    if (!jwtService.isTokenValid(refreshToken, user)) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Refresh token không hợp lệ hoặc đã hết hạn.");
    }

    String refreshTokenKey = "user:refresh_tokens:" + user.getId();
    Map<Object, Object> allDeviceTokens = redisService.hGetAll(refreshTokenKey);

    if (allDeviceTokens == null || allDeviceTokens.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Refresh token không tồn tại hoặc đã bị thu hồi.");
    }

    String matchedDeviceId = null;
    Map<String, Object> matchedTokenData = null;

    for (Map.Entry<Object, Object> entry : allDeviceTokens.entrySet()) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenData = objectMapper.convertValue(
            entry.getValue(), new TypeReference<Map<String, Object>>() {
            });
        if (refreshToken.equals(tokenData.get("token"))) {
          matchedDeviceId = (String) entry.getKey();
          matchedTokenData = tokenData;
          break;
        }
      } catch (Exception e) {
        log.warn("Không thể parse token data cho device: {}", entry.getKey());
      }
    }

    if (matchedDeviceId == null || matchedTokenData == null) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
          "Refresh token không khớp với bất kỳ thiết bị nào.");
    }

    String newDeviceId = deviceInfoService.createDeviceId(request.getDeviceInfo());
    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    DeviceInfoResponse result = refreshTokenService.refreshSessionTokens(
        user, userDetails, request.getDeviceInfo(), newDeviceId, matchedDeviceId);

    log.info("Refresh token thành công cho user: {}", user.getUsername());

    return Response.ok(AuthResponse.builder()
        .message(result.getMessage() != null ? result.getMessage() : "Tạo mới token thành công.")
        .accessToken(result.getAccessToken())
        .refreshToken(result.getFinalRefreshTokenString())
        .userId(String.valueOf(user.getId()))
        .username(user.getUsername())
        .email(user.getEmail())
        .avatarUrl(user.getAvatarUrl())
        .gender(user.getGender())
        .phoneNumber(user.getPhoneNumber())
        .roles(user.getRoles())
        .build());
  }

  @Override
  public void logoutUser(HttpServletRequest request) {
    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return;
    }

    final String jwt = authHeader.substring(7);

    try {
      final String username = jwtService.extractUsername(jwt);
      if (username == null)
        return;

      userRepository.findByEmail(username).ifPresent(user -> {
        refreshTokenService.revokeAllUserTokens(user.getId());
        log.info("Người dùng {} đã đăng xuất, đã thu hồi tất cả Refresh Token và Session.", username);
      });
    } catch (Exception e) {
      log.warn("Không thể thu hồi Refresh Token khi logout: {}", e.getMessage());
    }
  }
}
