package com.anno.ERP_SpringBoot_Experiment.integration.flow;

import com.anno.ERP_SpringBoot_Experiment.integration.AbstractIntegrationTest;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.PagingRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test cho Refresh Token flow.
 * Login thật → nhận access + refresh token → dùng access token gọi API.
 */
@DisplayName("Refresh Token - Integration Test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenFlowTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUsername;
    private final String testPassword = "Pass123!";

    private String testEmail;

    @BeforeAll
    void setUp() {
        testUsername = "RefreshUser" + System.nanoTime();
        testEmail = testUsername + "@test.com";
        User user = new User();
        user.setEmail(testEmail);
        user.setFullName("Refresh User");
        user.setPassword(passwordEncoder.encode(testPassword));
        user.setStatus(ActiveStatus.ACTIVE);
        user.setRoles(Set.of(RoleType.USER));
        userRepository.save(user);
    }

    private String login() throws Exception {
        String loginJson = String.format(
                "{\"usernameOrEmail\":\"%s\",\"password\":\"%s\",\"deviceInfo\":{\"deviceName\":\"TestDevice\",\"ipAddress\":\"127.0.0.1\",\"userAgent\":\"JUnit\"}}",
                testEmail, testPassword);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        var responseType = objectMapper.getTypeFactory()
                .constructParametricType(Response.class, AuthResponse.class);
        Response<AuthResponse> response = objectMapper.readValue(json, responseType);

        assert response.getData() != null;
        return json;
    }

    @Test
    @DisplayName("Login returns both accessToken + refreshToken")
    void login_ReturnsBothTokens() throws Exception {
        String json = login();
        assert json.contains("accessToken") : "Response must contain accessToken";
        assert json.contains("refreshToken") : "Response must contain refreshToken";
    }

    @Test
    @DisplayName("Access and refresh tokens are different JWT strings")
    void tokens_AreDifferentJwts() throws Exception {
        String loginJson = String.format(
                "{\"usernameOrEmail\":\"%s\",\"password\":\"%s\",\"deviceInfo\":{\"deviceName\":\"TestDevice\",\"ipAddress\":\"127.0.0.1\",\"userAgent\":\"JUnit\"}}",
                testEmail, testPassword);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        var responseType = objectMapper.getTypeFactory()
                .constructParametricType(Response.class, AuthResponse.class);
        Response<AuthResponse> response = objectMapper.readValue(json, responseType);

        String at = response.getData().getAccessToken();
        String rt = response.getData().getRefreshToken();

        assert at != null : "accessToken must not be null";
        assert rt != null : "refreshToken must not be null";
        assert !at.equals(rt) : "accessToken and refreshToken must be different";
        assert at.split("\\.").length == 3 : "accessToken must be valid JWT (3 parts)";
        assert rt.split("\\.").length == 3 : "refreshToken must be valid JWT (3 parts)";
    }

    @Test
    @DisplayName("Access token dùng để gọi search API thành công")
    void accessToken_CanCallSearchApi() throws Exception {
        // Login → lấy token
        String loginJson = String.format(
                "{\"usernameOrEmail\":\"%s\",\"password\":\"%s\",\"deviceInfo\":{\"deviceName\":\"TestDevice\",\"ipAddress\":\"127.0.0.1\",\"userAgent\":\"JUnit\"}}",
                testEmail, testPassword);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String json = loginResult.getResponse().getContentAsString();

        // Fix for display purpose - same approach
        var responseType = objectMapper.getTypeFactory()
                .constructParametricType(Response.class, AuthResponse.class);
        Response<AuthResponse> response = objectMapper.readValue(json, responseType);
        String token = response.getData().getAccessToken();

        // Dùng token gọi search API
        var searchReq = new com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest();
        var paging = new PagingRequest();
        paging.setSize(10);
        searchReq.setPaging(paging);

        mockMvc.perform(postJson("/api/merchandise/search-Category", searchReq, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contents").isArray());
    }
}
