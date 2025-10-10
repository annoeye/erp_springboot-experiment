package com.anno.ERP_SpringBoot_Experiment.service.interfaces;


import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.RegisterResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.Page;

public interface iUser {
    Response<RegisterResponse> createUser(final UserRegisterRequest body) throws MessagingException;
    Response<AuthResponse> loginUser(final UserLoginRequest body) throws MessagingException;
    Response<?> verifyAccount(final String code, final ActiveStatus type, final AccountVerificationRequest request);
    Response<?> sendCodeResetPassword(final String email) throws MessagingException;
    Page<UserDto>  search(final UserSearchRequest request);
//    Page<UserSearchRequest> search(final UserSearchRequest request);
//    Response<?> logoutUser(HttpServletRequest request);
}