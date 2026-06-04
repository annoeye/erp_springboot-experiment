package com.anno.ERP_SpringBoot_Experiment.integration;

import com.anno.ERP_SpringBoot_Experiment.ErpSpringBootExperimentApplication;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ErpSpringBootExperimentApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mock external services
    @MockitoBean
    protected MinioClient minioClient;

    @MockitoBean
    protected JavaMailSender mailSender;

    @MockitoBean
    protected RedissonClient redissonClient;

    @MockitoBean
    protected KafkaAdmin kafkaAdmin;

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected RedisService redisService;

    // Token cho các role
    protected String userAccessToken;
    protected String adminAccessToken;

    @BeforeEach
    void setUpMocksAndUsers() {
        // Redis mock
        doNothing().when(redisService).setValueWithExpiry(anyString(), any(), anyLong(), any(TimeUnit.class));
        doNothing().when(redisService).setValue(anyString(), any());
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.getValue(anyString())).thenReturn("test-value");

        // Tạo user và JWT token thật
        setupAuthTokens();
    }

    private void setupAuthTokens() {
        // Tạo USER token
        User user = createUser("testuser_" + System.nanoTime(), "Test User",
                RoleType.USER);
        userAccessToken = jwtService.generateToken(user, 86400000L);

        // Tạo ADMIN token
        User admin = createUser("testadmin_" + System.nanoTime(), "Admin User",
                RoleType.ADMIN);
        adminAccessToken = jwtService.generateToken(admin, 86400000L);
    }

    private User createUser(String name, String fullName, RoleType type) {
        if (userRepository.findByName(name).isPresent()) {
            return userRepository.findByName(name).get();
        }
        User user = new User();
        user.setName(name);
        user.setFullName(fullName);
        user.setEmail(name + "@test.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setStatus(ActiveStatus.ACTIVE);
        user.setRoles(Set.of(type));
        return userRepository.save(user);
    }

    // ==================== Request Builders ====================

    protected MockHttpServletRequestBuilder postJson(String url, Object body) {
        return postJson(url, body, null);
    }

    protected MockHttpServletRequestBuilder postJson(String url, Object body, String bearerToken) {
        MockHttpServletRequestBuilder builder = post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(body));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return builder;
    }

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== Response Helpers ====================

    protected ResultActions perform(MockHttpServletRequestBuilder requestBuilder, ResultMatcher statusMatcher) throws Exception {
        return mockMvc.perform(requestBuilder).andExpect(statusMatcher);
    }

    @SuppressWarnings("unchecked")
    protected <T> T parseResponse(ResultActions result, Class<?> parametrized, Class<?>... parameterClasses) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return (T) objectMapper.readValue(json, type);
    }
}
