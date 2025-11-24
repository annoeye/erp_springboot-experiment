package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
public interface AuthController {

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    Response<AuthResponse> login
            (@Valid @RequestBody final UserLoginRequest body) throws MessagingException;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    Response<RegisterResponse>  register
            (@Valid @RequestBody final UserRegisterRequest body) throws MessagingException;

        @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    Response<?> verifyAccount(
            @RequestParam("token") final String code,
            @RequestParam("purpose") final ActiveStatus purpose,
            @RequestBody(required = false) final AccountVerificationRequest body
    );

    @GetMapping("/send-reset-code/{email}")
    @ResponseStatus(HttpStatus.OK)
    Response<?> sendPasswordResetCode
            (@PathVariable final String email) throws MessagingException;

    @PostMapping("/search")
    Response<PagingResponse<UserDto>> search(@RequestBody final UserSearchRequest request );


    @PostMapping("/logout")
    ResponseEntity<?> logout
            (final HttpServletRequest request);

}
