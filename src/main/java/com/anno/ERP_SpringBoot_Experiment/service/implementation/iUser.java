package com.anno.ERP_SpringBoot_Experiment.service.implementation;


import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.response.GetUserResponse;
import com.anno.ERP_SpringBoot_Experiment.response.RegisterResponse;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;

public interface iUser {
    RegisterResponse createUser(UserRegister body) throws MessagingException;
    AuthResponse loginUser(UserLogin body) throws MessagingException;
    ResponseEntity<?> verifyAccount(String code, ActiveStatus type, AccountVerificationDto request);
    void sendCodeResetPassword(String email) throws MessagingException;
    ResponseEntity<?> getUser(ActiveStatus type);
}