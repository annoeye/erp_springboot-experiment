package com.anno.ERP_SpringBoot_Experiment.service.implementation;


import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;

public interface iUser {
    String createUser(UserRegister body);
    String loginUser(UserLogin body);
}