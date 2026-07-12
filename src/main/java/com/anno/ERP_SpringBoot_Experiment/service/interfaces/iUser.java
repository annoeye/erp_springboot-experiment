package com.anno.ERP_SpringBoot_Experiment.service.interfaces;


import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProfileRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.MyProfileResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import jakarta.servlet.http.HttpServletRequest;

public interface iUser {
    Response<RegisterResponse> createUser(final UserRegisterRequest body);
    Response<AuthResponse> loginUser(final UserLoginRequest body);
    Response<String> verifyEmail(final String code);
    Response<String> resetPassword(final String code, final AccountVerificationRequest request);
    Response<String> recoverAccount(final String email);
    Response<String> validateResetToken(final String token);
    Response<AuthResponse> refreshToken(final RefreshTokenRequest request);
//    Page<UserDto> search(final UserSearchRequest request);
//    Page<UserSearchRequest> search(final UserSearchRequest request);
    void logoutUser(HttpServletRequest request);
    Response<MyProfileResponse> getMyProfile();
    Response<MyProfileResponse> updateMyProfile(final UpdateProfileRequest request);
    Response<MyProfileResponse> uploadAvatar(final org.springframework.web.multipart.MultipartFile file);
}