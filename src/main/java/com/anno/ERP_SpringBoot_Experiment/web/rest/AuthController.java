package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.ChangeUsernameRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProfileRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.MyProfileResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
public interface AuthController {

        @PostMapping("/login")
        @ResponseStatus(HttpStatus.OK)
        Response<AuthResponse> login(@Valid @RequestBody final UserLoginRequest body);

        @PostMapping("/register")
        @ResponseStatus(HttpStatus.OK)
        Response<RegisterResponse> register(@Valid @RequestBody final UserRegisterRequest body);

        @GetMapping("/verify-email")
        @ResponseStatus(HttpStatus.OK)
        Response<String> verifyEmail(@RequestParam("token") final String code);

        @PostMapping("/reset-password")
        @ResponseStatus(HttpStatus.OK)
        Response<String> resetPassword(
                        @RequestParam("code") final String code,
                        @Valid @RequestBody final AccountVerificationRequest body);

        @GetMapping("/validate-reset-token")
        @ResponseStatus(HttpStatus.OK)
        Response<UserDto> validateResetToken(@RequestParam("token") final String token);

        @PostMapping("/refresh-token")
        @ResponseStatus(HttpStatus.OK)
        Response<AuthResponse> refreshToken(@Valid @RequestBody final RefreshTokenRequest body);

        @GetMapping("/recover-account/{email}")
        @ResponseStatus(HttpStatus.OK)
        Response<String> recoverAccount(@PathVariable final String email);

        @PostMapping("/logout")
        ResponseEntity<?> logout(final HttpServletRequest request);

        @GetMapping("/me")
        @ResponseStatus(HttpStatus.OK)
        @PreAuthorize("!hasRole('USER')")
        Response<MyProfileResponse> getMyProfile();

        @PutMapping("/me")
        @ResponseStatus(HttpStatus.OK)
        Response<MyProfileResponse> updateMyProfile(@Valid @RequestBody final UpdateProfileRequest body);

        @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
        @ResponseStatus(HttpStatus.OK)
        Response<MyProfileResponse> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file);

        @PutMapping("/change-username")
        @ResponseStatus(HttpStatus.OK)
        Response<String> changeUsername(@Valid @RequestBody final ChangeUsernameRequest body);
}

