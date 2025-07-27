package com.anno.ERP_SpringBoot_Experiment.controller.apiRequest;

import com.anno.ERP_SpringBoot_Experiment.model.dto.ChangePassword;
import com.anno.ERP_SpringBoot_Experiment.model.dto.StopWork;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token,
                                           @RequestParam("username") String username,
                                           @RequestParam("type") String type) {
        return ResponseEntity.ok(userService.verifyAccount(username, token, type));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePassword body) throws MessagingException {
        return ResponseEntity.ok(userService.changePassword(body));
    }

    @PostMapping("/send-reset-code/{userId}")
    public ResponseEntity<?> sendPasswordResetCodeById(@PathVariable UUID    userId) throws MessagingException {
        return ResponseEntity.ok(userService.sendCodeResetPassword(userId));
    }

    @PostMapping("/stop-work")
    public ResponseEntity<String> stopWork(StopWork stopWork){
        return ResponseEntity.ok(userService.stopWork(stopWork));
    }

    @PostMapping("/resume-work")
    public ResponseEntity<String> resumeWork(StopWork stopWork) {
        return ResponseEntity.ok(userService.resumeWork(stopWork));
    }


}
