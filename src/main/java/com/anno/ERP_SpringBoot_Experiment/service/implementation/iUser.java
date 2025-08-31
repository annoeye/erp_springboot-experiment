package com.anno.ERP_SpringBoot_Experiment.service.implementation;


import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.dto.ChangePassword;
import com.anno.ERP_SpringBoot_Experiment.dto.StopWork;
import com.anno.ERP_SpringBoot_Experiment.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import Helper;
import jakarta.mail.MessagingException;

import java.util.UUID;

public interface iUser {
    String createUser(UserRegister body) throws MessagingException;
    AuthResponse loginUser(UserLogin body) throws MessagingException;
    String verifyAccount(String userName, String token, String type);
    String changePassword(ChangePassword changePasswordDto) throws CustomException, MessagingException;
    String sendCodeResetPassword(UUID id) throws MessagingException;
    String stopWork(StopWork stopWorkDto);
    String resumeWork(StopWork stopWorkDto);
    Object getUser(Helper.UserRequestType type, Object param);
}