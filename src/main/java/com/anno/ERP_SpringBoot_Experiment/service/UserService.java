package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.ChangePassword;
import com.anno.ERP_SpringBoot_Experiment.model.dto.StopWork;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.CreateAccountRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.RefreshTokenRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ViolationHandlingRepository;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.helper.UserHelper;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUserAction;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class    UserService implements iUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserHelper userHelper;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CreateAccountRepository createAccountRepository;
    private final ViolationHandlingRepository violationHandlingRepository;
    private final iUserAction userActionLogService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
    private static final String OTP_FORMAT_PATTERN = "%0" + OTP_LENGTH + "d";

    @Value("${server.port}")
    private String serverPort;

    @Override
    @Transactional
    public String createUser(UserRegister body) throws MessagingException {

        if (!userHelper.isEmailFormat(body.getEmail())) {
            throw new CustomException("Email không đúng định dạng", HttpStatus.BAD_REQUEST);
        } else if (!body.getPassword().equals(body.getConfirmPassword())) {
            throw new CustomException("Mật khẩu không khớp", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByEmail(body.getEmail())
                .filter(u -> u.getStatus() == ActiveStatus.ACTIVE)
                .ifPresent(u -> { throw new CustomException("Email đã tồn tại.", HttpStatus.CONFLICT); });

        userRepository.findByUsername(body.getUserName())
                .ifPresent(existingUser -> {
                    if (!existingUser.getEmail().equals(body.getEmail())) {
                        throw new CustomException("Tên đăng nhập đã tồn tại với email khác. Hãy kiểm tra " + userHelper.maskEmail(existingUser.getEmail()) + ".", HttpStatus.CONFLICT);
                    }
                });

        CreateAccount createAccount = createAccountRepository.findByToken(body.getToken())
                .orElseThrow(() -> new CustomException("Token không hợp lệ.", HttpStatus.BAD_REQUEST));

        if (!createAccount.getEndTime().isBefore(LocalDateTime.now())) {
            throw new CustomException("Token lỗi.", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByUsernameAndEmail(body.getUserName(), body.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setFullName(body.getFullName());
                    newUser.setName(body.getUserName());
                    newUser.setEmail(body.getEmail());
                    newUser.setRoles(new HashSet<>(createAccount.getRoles()));
                    newUser.setPassword(passwordEncoder.encode(body.getPassword()));
                    newUser.setStatus(ActiveStatus.INACTIVE);
                    logger.info("Tạo user mới: {}", newUser.getUsername());
                    return newUser;
                });


        UserDetails tempUserDetailsForToken = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername()) // Lấy username từ đối tượng user đã có
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        String verificationToken = userHelper.createShortToken(tempUserDetailsForToken, 5 * 60 * 1000L); // 5 phút
        user.setEmailVerificationToken(verificationToken);
        user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        String type = "verifyAccount";
        String verificationUrl = "http://localhost:" + serverPort + "/api/auth/verify?token=" + user.getEmailVerificationToken() + "&username=" + user.getUsername() + "&type=" + type;
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationUrl);
        } catch (MessagingException e) {
            logger.error("Lỗi gửi email xác thực cho {}: {}", user.getEmail(), e.getMessage(), e);
            throw new MessagingException("Lỗi gửi email xác thực: " + e.getMessage());
        }

        return (user.getCreatedAt() != null) ?
                "Email đã tồn tại nhưng chưa xác thực. Một email xác thực mới đã được gửi đến " + userHelper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra." :
                "Một email xác thực đã được gửi đến " + userHelper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.";
    }

    @Override
    @Transactional
    public AuthResponse loginUser(UserLogin body) {

        User user;
        String usernameOrEmail = body.getUsernameOrEmail();
        Optional<User> foundUserByUsername = userRepository.findByUsername(usernameOrEmail);

        user = foundUserByUsername.orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new CustomException("Tên đăng nhập hoặc Email không tồn tại.", HttpStatus.NOT_FOUND)));

        if (user.getActive().equals(User.Active.INACTIVE)) {
            if (user.getEmailVerificationToken() == null || user.getTokenExpiryDate() == null || user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
                UserDetails tempUserDetailsForToken = org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password("")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();
                String newEmailToken = userHelper.createShortToken(tempUserDetailsForToken, 300000L);
                logger.debug("DEBUG: Token được tạo mới: {}", newEmailToken);

                user.setEmailVerificationToken(newEmailToken);
                user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(5));
                user = userRepository.save(user);
                logger.info("Tạo và gửi lại token xác thực cho user chưa active: {}", user.getUsername());
                logger.debug("DEBUG: Token sau khi lưu vào DB (từ đối tượng user): {}", user.getEmailVerificationToken());
            } else {
                logger.info("User {} đã có token xác thực còn hiệu lực.", user.getUsername());
                logger.debug("DEBUG: Token hiện tại của user (còn hiệu lực): {}", user.getEmailVerificationToken());
            }

            String type = "verifyAccount";
            String verificationUrl = "http://localhost:" + serverPort + "api/auth/verify?token=" + user.getEmailVerificationToken() + "&username=" + user.getUsername() + "&type=" + type;
            logger.debug("DEBUG: URL xác thực đầy đủ được tạo: {}", verificationUrl);
            logger.debug("DEBUG: Token trích xuất từ URL: {}", user.getEmailVerificationToken());

            try {
                emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationUrl);
            } catch (MessagingException e) {
                logger.error("Lỗi gửi lại email xác thực cho user chưa active {}: {}", user.getEmail(), e.getMessage(), e);
                return AuthResponse.builder()
                        .message("Lỗi trong quá trình gửi email xác thực. Vui lòng thử lại sau.")
                        .accessToken(null)
                        .refreshToken(null)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .userId(String.valueOf(user.getId()))
                        .roles(null)
                        .build();
            }

            return AuthResponse.builder()
                    .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến " + userHelper.maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                    .accessToken(null)
                    .refreshToken(null)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(String.valueOf(user.getId()))
                    .roles(null)
                    .build();
        }

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu không đúng.", HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String finalRefreshTokenString;
        DeviceInfo deviceInfoFromRequest = body.getDeviceInfo();

        if (deviceInfoFromRequest == null) {
            throw new CustomException("Thông tin thiết bị là bắt buộc để đăng nhập.", HttpStatus.BAD_REQUEST);
        }

        long currentRefreshTokenLifespanMillis = 2592000000L;
        long currentAccessTokenLifespanMillis = 900000L;

        List<RefreshToken> userRefreshTokens = refreshTokenRepository.findAllByUserInfo(user);
        RefreshToken targetRefreshToken = null;
        boolean deviceMatchedInExistingToken = false;

        for (RefreshToken rt : userRefreshTokens) {
            if (rt.getDeviceInfo() != null && !rt.getDeviceInfo().isEmpty()) {
                for (DeviceInfo existingDi : rt.getDeviceInfo()) {
                    if (userHelper.areDeviceInfoMatching(existingDi, deviceInfoFromRequest)) {
                        targetRefreshToken = rt;
                        if (!Objects.equals(existingDi.getIpAddress(), deviceInfoFromRequest.getIpAddress())) {
                            existingDi.setIpAddress(deviceInfoFromRequest.getIpAddress());
                            logger.debug("Cập nhật IP cho thiết bị của user {}: {} -> {}", user.getUsername(), existingDi.getDeviceType(), deviceInfoFromRequest.getIpAddress());
                        }
                        deviceMatchedInExistingToken = true;
                        break;
                    }
                }
            }
            if (deviceMatchedInExistingToken) break;
        }

        if (deviceMatchedInExistingToken && targetRefreshToken != null) {
            logger.info("Thiết bị cũ đăng nhập cho user: {}. Làm mới refresh token ID: {}", user.getUsername(), targetRefreshToken.getId());
            targetRefreshToken.setToken(jwtService.generateToken(userDetails, currentRefreshTokenLifespanMillis));
            targetRefreshToken.setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(currentRefreshTokenLifespanMillis)));
            refreshTokenRepository.save(targetRefreshToken);
            finalRefreshTokenString = targetRefreshToken.getToken();
        } else {
            logger.info("Thiết bị mới đăng nhập cho user: {}. Tạo refresh token mới.", user.getUsername());
            RefreshToken newRefreshToken = new RefreshToken();
            newRefreshToken.setUserInfo(user);
            newRefreshToken.setToken(jwtService.generateToken(userDetails, currentRefreshTokenLifespanMillis));
            newRefreshToken.setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(currentRefreshTokenLifespanMillis)));
            newRefreshToken.setDeviceInfo(Collections.singletonList(deviceInfoFromRequest));
            refreshTokenRepository.save(newRefreshToken);
            finalRefreshTokenString = newRefreshToken.getToken();
        }
        logger.info("Đăng nhập thành công cho user: {}", user.getUsername());
        String accessToken = jwtService.generateToken(userDetails, currentAccessTokenLifespanMillis);
        userActionLogService.log(user, Log.ActionType.LOGIN, null, "Đăng nhập", null);
        logger.info("Đã lưu thông tin người đăng nhập vào lịch sử hoạt động");
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(finalRefreshTokenString)
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(String.valueOf(user.getId()))
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRole().name())
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public String verifyAccount(String userName, String token, String type) {

        User user = userRepository.findByUsername(userName)
                .orElse(null);

        if (user == null) {
            return "Người dùng " + userName + " không tìm thấy để xác thực.";
        }

        switch (type) {
            case "verifyAccount":
                if (Objects.equals(token, user.getEmailVerificationToken()) && user.getTokenExpiryDate() != null && user.getTokenExpiryDate().isAfter(LocalDateTime.now())) {
                    user.setActive(User.Active.ACTIVE);
                    if (user.getCreatedAt() == null) {
                        user.setCreatedAt(LocalDateTime.now());
                    }
                    user = userRepository.save(user);
                    logger.info("Xác thực tài khoản user chưa active thành công: {}", user.getUsername());
                    return "Xác thực tài khoản thành công!";
                } else {
                    return "Token không hợp lệ hoặc đã hết hạn.";
                }

            default:
                return "Loại xác thực không hợp lệ: " + type;
        }
    }

    @Override
    @Transactional
    public String changePassword(ChangePassword changePasswordDto) throws CustomException {

        changePasswordDto.validatePasswordsMatch();
        User user = userRepository.findById(changePasswordDto.getId())
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        if (Objects.equals(changePasswordDto.getCodeResetPassword(), user.getCodeResetPassword())) {
            user.setPassword(passwordEncoder.encode(changePasswordDto.getPassword()));
            user.setCodeResetPassword(null);
            userRepository.save(user);
            logger.info("Đã đặt lại mật khẩu cho user: {}", user.getUsername());
        } else {
            throw new CustomException("Mã xác thực không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        return "Đặt lại mật khẩu thành công!";
    }

    @Override
    @Transactional
    public String sendCodeResetPassword(UUID id) throws MessagingException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        String code = String.format(OTP_FORMAT_PATTERN, SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
        user.setCodeResetPassword(code);
        userRepository.save(user);

        try {
            emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getUsername(), code);
            logger.info("Đã gửi mã OTP đặt lại mật khẩu cho user: {}", user.getUsername());
        } catch (MessagingException e) {
            logger.error("Lỗi gửi email OTP đặt lại mật khẩu cho {}: {}", user.getEmail(), e.getMessage(), e);
            throw new MessagingException("Lỗi gửi email đặt lại mật khẩu: " + e.getMessage(), e);
        }
        return String.format("Mã xác thực đặt lại mật khẩu đã được gửi đến email của user: %s.", user.getUsername());
    }

    @Override
    public String stopWork(StopWork stopWorkDto) {
        User user = userRepository.getUserByUsername(stopWorkDto.getHandledByUserName());
        User targetUser = userRepository.getUserByUsername(stopWorkDto.getTargetUser());
        if (user.getActive().equals(User.Active.ACTIVE)) {
            user.setActive(User.Active.LOCKED);
            ViolationHandling violationHandling = ViolationHandling
                    .builder()
                    .id(UUID.randomUUID())
                    .handledBy(user)
                    .targetUser(targetUser)
                    .action(stopWorkDto.getActionType())
                    .createdAt(LocalDateTime.now())
                    .startAt(stopWorkDto.getStartAt())
                    .endAt(stopWorkDto.getEndAt())
                    .reason(stopWorkDto.getReason())
                    .build();

            userRepository.save(user);
            violationHandlingRepository.save(violationHandling);
            logger.info("Người dùng {} đã bị xử lý vi phạm", user.getUsername());
        }else
            logger.error("Gặp lỗi khi xử lý người vi phạm {}", user.getUsername());
        return "Thực hiện dừng công việc thành công.";
    }

    @Override
    public String resumeWork(StopWork stopWorkDto) {
        User user = userRepository.getUserByUsername(stopWorkDto.getHandledByUserName());
        User targetUser = userRepository.getUserByUsername(stopWorkDto.getTargetUser());
        if (user.getActive().equals(User.Active.LOCKED)) {
            user.setActive(User.Active.ACTIVE);
            ViolationHandling violationHandling = ViolationHandling
                    .builder()
                    .id(UUID.randomUUID())
                    .handledBy(user)
                    .targetUser(targetUser)
                    .action(stopWorkDto.getActionType())
                    .createdAt(LocalDateTime.now())
                    .startAt(null)
                    .endAt(null)
                    .reason(stopWorkDto.getReason())
                    .build();

            userRepository.save(user);
            violationHandlingRepository.save(violationHandling);
            logger.info("Đã mở khóa {}", user.getUsername());
        }
            logger.error("Gặp lỗi khi mở khóa người vi phạm {}", user.getUsername());
        return "Mở khóa thành công.";
    }

    @Override
    public Object getUser(UserHelper.UserRequestType type, Object param) {
        switch (type) {
            case GET_ALL:
                return userRepository.findAll();

            case GET_BY_ID:
               if (param instanceof UUID id)
                   return userRepository.findById(id)
                           .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

            default:
                throw new UnsupportedOperationException("Loại yêu cầu không được hỗ trợ");
        }
    }

}