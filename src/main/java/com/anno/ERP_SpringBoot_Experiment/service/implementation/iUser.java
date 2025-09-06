package com.anno.ERP_SpringBoot_Experiment.service.implementation;


import com.anno.ERP_SpringBoot_Experiment.dto.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import jakarta.mail.MessagingException;

public interface iUser {
    String createUser(UserRegister body) throws MessagingException;
    AuthResponse loginUser(UserLogin body) throws MessagingException;
    void verifyAccount(String code, ActiveStatus type, AccountVerificationDto request);
    void sendCodeResetPassword(String email) throws MessagingException;
}