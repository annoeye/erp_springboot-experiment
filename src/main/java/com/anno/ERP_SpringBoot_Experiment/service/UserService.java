package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.ChangePassword;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.model.entity.DeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.RefreshToken;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.RefreshTokenRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService implements iUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${server.port}")
    private String serverPort;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
    private static final String OTP_FORMAT_PATTERN = "%0" + OTP_LENGTH + "d";

    @Override
    @Transactional
    public String createUser(UserRegister body) throws MessagingException {

        if (!isEmailFormat(body.getEmail())) {
            throw new CustomException("Email không đúng định dạng", HttpStatus.BAD_REQUEST);
        }
        userRepository.findByEmail(body.getEmail()).ifPresent(existingUser -> {
            throw new CustomException("Email đã tồn tại.", HttpStatus.CONFLICT);
        });
        userRepository.findByUsername(body.getUserName()).ifPresent(existingUser -> {
            throw new CustomException("Tên đăng nhập đã tồn tại.", HttpStatus.CONFLICT);
        });

        UserDetails tempUserDetailsForToken = org.springframework.security.core.userdetails.User.builder()
                .username(body.getUserName())
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        User user = User.builder()
                .userName(body.getUserName())
                .email(body.getEmail())
                .password(passwordEncoder.encode(body.getPassword()))
                .emailVerificationToken(createShortToken(tempUserDetailsForToken, 5000L * 60))
                .tokenExpiryDate(LocalDateTime.now().plusMinutes(5))
                .active(User.Active.INACTIVE)
                .build();
        userRepository.save(user);
        logger.info("Tạo tài khoản user chưa active: {}", user.getUsername());

        String type = "verifyAccount";
        String verificationUrl = "http://localhost:" + serverPort + "/auth/verify?token=" + user.getEmailVerificationToken()+ "&username=" + user.getUsername() + "&type=" + type;
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationUrl);
        } catch (MessagingException e) {
            throw new MessagingException("Lỗi gửi email xác thực: " + e.getMessage());
        }
        return "Yêu cầu xác nhận tài khoản.";
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
                String newEmailToken = createShortToken(tempUserDetailsForToken, 300000L);
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
            String verificationUrl = "http://localhost:" + serverPort + "/auth/verify?token=" + user.getEmailVerificationToken() + "&username=" + user.getUsername() + "&type=" + type;
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
                        .userId(user.getId())
                        .roles(null)
                        .build();
            }

            return AuthResponse.builder()
                    .message("Tài khoản chưa được xác thực. Một email xác thực đã được gửi (lại) đến " + maskEmail(user.getEmail()) + ". Vui lòng kiểm tra.")
                    .accessToken(null)
                    .refreshToken(null)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getId())
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
                    if (areDeviceInfoMatching(existingDi, deviceInfoFromRequest)) {
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
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(finalRefreshTokenString)
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(user.getId())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRole().name())
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public String verifyAccount(String userName, String token, String type) {

        User user = userRepository.findByEmail(userName)
                .orElse(null);

        if (user == null) {
            return "Người dùng '" + userName + "' không tìm thấy để xác thực.";
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
    public String sendCodeResetPassword(Long id) throws MessagingException {
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

    private String createShortToken(UserDetails userDetails, long expirationTimeMillis) {
        return jwtService.generateToken(userDetails, expirationTimeMillis);
    }

    private boolean isEmailFormat(String input) {
        return input != null && Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", input);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 5) {
            return localPart + domain;
        }

        String firstTwo = localPart.substring(0, 2);
        String lastThree = localPart.substring(localPart.length() - 3);
        int starCount = localPart.length() - 5;
        String stars = "*".repeat(starCount);

        return firstTwo + stars + lastThree + domain;
    }

    private boolean areDeviceInfoMatching(DeviceInfo d1, DeviceInfo d2) {
        if (d1 == null || d2 == null) return false;
        String d1Type = (d1.getDeviceType() != null) ? d1.getDeviceType().trim().toLowerCase() : null;
        String d2Type = (d2.getDeviceType() != null) ? d2.getDeviceType().trim().toLowerCase() : null;

        String d1Os = (d1.getOsName() != null) ? d1.getOsName().trim().toLowerCase() : null;
        String d2Os = (d2.getOsName() != null) ? d2.getOsName().trim().toLowerCase() : null;

        boolean typeMatch = Objects.equals(d1Type, d2Type);
        boolean osMatch = Objects.equals(d1Os, d2Os);

        return typeMatch && osMatch;
    }
}