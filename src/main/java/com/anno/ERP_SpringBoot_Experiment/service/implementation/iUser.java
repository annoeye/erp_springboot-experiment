package com.anno.ERP_SpringBoot_Experiment.service.implementation;


import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.ChangePassword;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import jakarta.mail.MessagingException;

public interface iUser {
    String createUser(UserRegister body) throws MessagingException;
    AuthResponse loginUser(UserLogin body) throws MessagingException;
    String verifyAccount(String userName, String token, String type);
    String changePassword(ChangePassword changePasswordDto) throws CustomException, MessagingException;
    String sendCodeResetPassword(Long id) throws MessagingException;
}