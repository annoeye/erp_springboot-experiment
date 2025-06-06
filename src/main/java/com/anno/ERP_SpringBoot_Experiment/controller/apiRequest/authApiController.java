package com.anno.ERP_SpringBoot_Experiment.controller.apiRequest;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class authApiController {

    private iUser userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLogin body){
        userService.loginUser(body);
        return ResponseEntity.ok().body(userService);
    }
}
