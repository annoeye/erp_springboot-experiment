package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AccountVerificationRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.RefreshTokenRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserLoginRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UserRegisterRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iUser;
import com.anno.ERP_SpringBoot_Experiment.web.rest.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class authControllerImpl implements AuthController {

    private final iUser userService;

    @Override
    public Response<AuthResponse> login(final UserLoginRequest body) {
        return userService.loginUser(body);
    }

    @Override
    public Response<RegisterResponse> register(final UserRegisterRequest body) {
        return userService.createUser(body);
    }

    @Override
    public Response<String> verifyEmail(final String code) {
        return userService.verifyEmail(code);
    }

    @Override
    public Response<String> resetPassword(
            final String code,
            final AccountVerificationRequest body) {
        return userService.resetPassword(code, body);
    }

    @Override
    public Response<AuthResponse> refreshToken(final RefreshTokenRequest body) {
        return userService.refreshToken(body);
    }

    @Override
    public Response<String> sendPasswordResetCode(final String email) {
        return userService.sendCodeResetPassword(email);
    }

//    @Override
//    public Response<PagingResponse<UserDto>> search(UserSearchRequest request) {
//        final Page<UserDto> users = userService.search(request);
//        final PagingRequest page = request.getPaging();
//        return Response.ok(
//                PagingResponse.<UserDto>builder()
//                        .contents(users.getContent())
//                        .paging(new PageableData()
//                                .setPageNumber(page.getPage() - 1)
//                                .setTotalPage(users.getTotalPages())
//                                .setPageSize(page.getSize())
//                                .setTotalRecord(users.getTotalElements())
//                        )
//                        .build()
//        );
//    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        userService.logoutUser(request);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công."));
    }
}
