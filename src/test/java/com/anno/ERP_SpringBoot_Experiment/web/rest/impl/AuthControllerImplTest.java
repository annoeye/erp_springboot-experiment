package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.model.enums.UserRank;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProfileRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.MyProfileResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(controllers = authControllerImpl.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController REST Unit Tests")
class AuthControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private iUser userService;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil securityUtil;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.service.JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private MyProfileResponse mockProfileResponse;

    @BeforeEach
    void setUp() {
        mockProfileResponse = MyProfileResponse.builder()
                .username("testuser")
                .fullName("Nguyen Van A")
                .email("testuser@example.com")
                .phoneNumber("0987654321")
                .avatarUrl("http://example.com/avatar.jpg")
                .dateOfBirth(new Date())
                .gender(Gender.MALE)
                .rank(UserRank.MEMBER)
                .status(ActiveStatus.ACTIVE)
                .roles(Set.of(RoleType.USER))
                .build();
    }

    @Nested
    @DisplayName("GET /api/auth/me — getMyProfile")
    class GetMyProfileTests {
        @Test
        @DisplayName("Should return 200 OK with profile data")
        void shouldGetProfile() throws Exception {
            when(userService.getMyProfile()).thenReturn(Response.ok(mockProfileResponse));

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"));

            verify(userService).getMyProfile();
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/me — updateMyProfile")
    class UpdateMyProfileTests {
        @Test
        @DisplayName("Should update profile successfully with valid data")
        void shouldUpdateProfile_Valid() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Nguyen Van B");
            request.setPhoneNumber("0123456789");
            request.setGender(Gender.FEMALE);

            MyProfileResponse updatedResponse = MyProfileResponse.builder()
                    .username("testuser")
                    .fullName("Nguyen Van B")
                    .email("testuser@example.com")
                    .phoneNumber("0123456789")
                    .gender(Gender.FEMALE)
                    .build();

            when(userService.updateMyProfile(any(UpdateProfileRequest.class)))
                    .thenReturn(Response.ok(updatedResponse));

            mockMvc.perform(put("/api/auth/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fullName").value("Nguyen Van B"))
                    .andExpect(jsonPath("$.data.phoneNumber").value("0123456789"))
                    .andExpect(jsonPath("$.data.gender").value("FEMALE"));

            verify(userService).updateMyProfile(any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when phone number is invalid")
        void shouldReturn400_InvalidPhone() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Nguyen Van B");
            request.setPhoneNumber("1234"); // Invalid phone number length

            mockMvc.perform(put("/api/auth/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateMyProfile(any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when fullName contains special characters")
        void shouldReturn400_InvalidName() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Nguyen @ B"); // Invalid characters
            request.setPhoneNumber("0987654321");

            mockMvc.perform(put("/api/auth/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateMyProfile(any(UpdateProfileRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/me/avatar — uploadAvatar")
    class UploadAvatarTests {
        @Test
        @DisplayName("Should upload avatar successfully with valid image file")
        void shouldUploadAvatar_Success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.png", "image/png", "image content".getBytes()
            );

            MyProfileResponse updatedResponse = MyProfileResponse.builder()
                    .username("testuser")
                    .fullName("Nguyen Van A")
                    .email("testuser@example.com")
                    .avatarUrl("avatar.png")
                    .build();

            when(userService.uploadAvatar(any())).thenReturn(Response.ok(updatedResponse));

            mockMvc.perform(multipart("/api/auth/me/avatar")
                            .file(file)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.avatarUrl").value("avatar.png"));

            verify(userService).uploadAvatar(any());
        }
    }
}
