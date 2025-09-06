package com.anno.ERP_SpringBoot_Experiment.controller.apiRequest;

import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class authApiController {

    private iUser userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLogin body) throws MessagingException {
        return ResponseEntity.ok(userService.loginUser(body));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegister body) throws MessagingException {
        return ResponseEntity.ok(userService.createUser(body));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String code,
                                           @RequestParam("type") ActiveStatus type,
                                           AccountVerificationDto body
                                           ) {
        userService.verifyAccount(code, type, body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-reset-code/{email}")
    public ResponseEntity<?> sendPasswordResetCode(@PathVariable String email) throws MessagingException {
        userService.sendCodeResetPassword(email);
        return ResponseEntity.ok().build();
    }
}
