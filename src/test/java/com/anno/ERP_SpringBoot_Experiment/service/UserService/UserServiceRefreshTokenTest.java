package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.domainevent.SaveDeviceInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.DeviceInfoResponse;
import com.anno.ERP_SpringBoot_Experiment.mapper.UserMapper;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.DeviceInfoService;
import com.anno.ERP_SpringBoot_Experiment.service.RefreshTokenService;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceRefreshTokenTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Helper helper;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private DeviceInfoService deviceInfoService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private com.anno.ERP_SpringBoot_Experiment.service.KafkaService.ActiveLogService activeLogService;
    @Mock
    private RedisService redisService;
    @Mock
    private JwtService jwtService;
    @Mock
    private ObjectMapper objectMapper;

    private UserService userService;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final String REFRESH_TOKEN = "valid-refresh-jwt-token";
    private final String NEW_ACCESS_TOKEN = "new-access-token";
    private final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Use reflection or constructor to inject mocks
        userService = new UserService(
                userRepository, passwordEncoder, helper, eventPublisher,
                deviceInfoService, refreshTokenService, userDetailsService,
                userMapper, activeLogService,
                redisService, jwtService, objectMapper
        );

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setFullName("Test User");
        testUser.setPassword("encoded-password");
        testUser.setStatus(ActiveStatus.ACTIVE);
        testUser.setRoles(Set.of(RoleType.USER));
        testUser.setGender(Gender.MALE);
        testUser.setAvatarUrl("https://avatar.url");
        testUser.setPhoneNumber("0123456789");
    }

    @Test
    @DisplayName("Success: refresh token valid → new tokens returned")
    void refreshToken_Success() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        // Use reflection to set fields since they're private with @Getter
        var requestRef = setupRequest(request);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(REFRESH_TOKEN, testUser)).thenReturn(true);

        // Mock Redis hash with matching token
        Map<Object, Object> deviceTokens = new HashMap<>();
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", REFRESH_TOKEN);
        deviceTokens.put("windows:desktop", tokenData);

        when(redisService.hGetAll("user:refresh_tokens:" + USER_ID)).thenReturn(deviceTokens);
        when(objectMapper.convertValue(any(), any(TypeReference.class))).thenReturn(tokenData);

        when(deviceInfoService.createDeviceId(any())).thenReturn("windows:desktop");
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);

        // Mock new token generation
        DeviceInfoResponse deviceInfoResponse = DeviceInfoResponse.builder()
                .accessToken(NEW_ACCESS_TOKEN)
                .finalRefreshTokenString(NEW_REFRESH_TOKEN)
                .build();
        when(refreshTokenService.refreshSessionTokens(any(), any(), any(), anyString(), anyString()))
                .thenReturn(deviceInfoResponse);

        // Act
        Response<AuthResponse> response = userService.refreshToken(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(NEW_ACCESS_TOKEN, response.getData().getAccessToken());
        assertEquals(NEW_REFRESH_TOKEN, response.getData().getRefreshToken());
        assertEquals(TEST_EMAIL, response.getData().getEmail());
        assertEquals(String.valueOf(USER_ID), response.getData().getUserId());

        verify(jwtService).extractUsername(REFRESH_TOKEN);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(jwtService).isTokenValid(REFRESH_TOKEN, testUser);
        verify(redisService).hGetAll("user:refresh_tokens:" + USER_ID);
        verify(deviceInfoService).createDeviceId(any());
        verify(userDetailsService).loadUserByUsername(anyString());
        verify(refreshTokenService).refreshSessionTokens(any(), any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Error: user not found → throws BusinessException")
    void refreshToken_UserNotFound() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        setupRequest(request);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.refreshToken(request));
        assertTrue(exception.getMessage().contains("Người dùng không tồn tại"));
    }

    @Test
    @DisplayName("Error: token invalid/expired → throws BusinessException")
    void refreshToken_TokenInvalid() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        setupRequest(request);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(REFRESH_TOKEN, testUser)).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.refreshToken(request));
        assertTrue(exception.getMessage().contains("không hợp lệ"));
    }

    @Test
    @DisplayName("Error: no tokens in Redis → throws BusinessException")
    void refreshToken_NoTokensInRedis() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        setupRequest(request);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(REFRESH_TOKEN, testUser)).thenReturn(true);
        when(redisService.hGetAll("user:refresh_tokens:" + USER_ID)).thenReturn(Collections.emptyMap());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.refreshToken(request));
        assertTrue(exception.getMessage().contains("không tồn tại hoặc đã bị thu hồi"));
    }

    @Test
    @DisplayName("Error: token doesn't match any device → throws BusinessException")
    void refreshToken_TokenNotMatched() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        setupRequest(request);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(REFRESH_TOKEN, testUser)).thenReturn(true);

        // Redis has tokens but none match
        Map<Object, Object> deviceTokens = new HashMap<>();
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", "different-refresh-token");
        deviceTokens.put("windows:desktop", tokenData);

        when(redisService.hGetAll("user:refresh_tokens:" + USER_ID)).thenReturn(deviceTokens);
        when(objectMapper.convertValue(any(), any(TypeReference.class))).thenReturn(tokenData);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.refreshToken(request));
        assertTrue(exception.getMessage().contains("không khớp"));
    }

    /**
     * Helper to set the private fields of RefreshTokenRequest via reflection.
     */
    private RefreshTokenRequest setupRequest(RefreshTokenRequest request) {
        try {
            var refreshField = RefreshTokenRequest.class.getDeclaredField("refreshToken");
            refreshField.setAccessible(true);
            refreshField.set(request, REFRESH_TOKEN);

            var deviceField = RefreshTokenRequest.class.getDeclaredField("deviceInfo");
            deviceField.setAccessible(true);
            deviceField.set(request, new com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test fields", e);
        }
        return request;
    }
}
