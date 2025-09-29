package com.anno.ERP_SpringBoot_Experiment.controller;

import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class authApiController {

    private iUser userService;

    @PostMapping("/login")
    public ResponseEntity<?> login
            (@Valid @RequestBody UserLogin body) throws MessagingException {
        return ResponseEntity.ok(userService.loginUser(body));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register
            (@Valid @RequestBody UserRegister body) throws MessagingException {
        return ResponseEntity.ok(userService.createUser(body));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(
            @RequestParam("token") String code,
            @RequestParam("purpose") ActiveStatus purpose,
            @RequestBody(required = false) AccountVerificationDto body
    ) {
        return userService.verifyAccount(code, purpose, body);
    }

    @PostMapping("/send-reset-code/{email}")
    public ResponseEntity<?> sendPasswordResetCode
            (@PathVariable String email) throws MessagingException {
        userService.sendCodeResetPassword(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-user/{type}")
    public ResponseEntity<?> getUserInfo(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @PathVariable ActiveStatus type
    ) {
        return userService.getUser(type);
    }
}
