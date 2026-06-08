package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        @PostMapping("/refresh-token")
        @ResponseStatus(HttpStatus.OK)
        Response<AuthResponse> refreshToken(@Valid @RequestBody final RefreshTokenRequest body);

        @GetMapping("/send-reset-code/{email}")
        @ResponseStatus(HttpStatus.OK)
        Response<String> sendPasswordResetCode(@PathVariable final String email);

//        @PostMapping("/search")
//        Response<PagingResponse<UserDto>> search(@RequestBody final UserSearchRequest request);

        @PostMapping("/logout")
        ResponseEntity<?> logout(final HttpServletRequest request);

}
