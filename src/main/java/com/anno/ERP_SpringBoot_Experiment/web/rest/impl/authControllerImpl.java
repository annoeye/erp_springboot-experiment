package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.mapper.UserMapper;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.*;
import com.anno.ERP_SpringBoot_Experiment.service.impl.iUser;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class authControllerImpl implements authController {

    private final iUser userService;
    private final UserMapper userMapper;

    @Override
    public Response<AuthResponse> login(final UserLoginRequest body) throws MessagingException {
        return userService.loginUser(body);
    }

    @Override
    public Response<RegisterResponse> register(final UserRegisterRequest body) throws MessagingException {
        return userService.createUser(body);
    }

    @Override
    public Response<?> verifyAccount(
            final String code,
            final ActiveStatus purpose,
            final AccountVerificationRequest body)
    {
        return userService.verifyAccount(code, purpose, body);
    }

    @Override
    public Response<?>  sendPasswordResetCode(final String email) throws MessagingException {
        return Response.ok(userService.sendCodeResetPassword(email));
    }

    @Override
    public Response<PagingResponse<UserDto>> search(UserSearchRequest request) {
        final Page<UserDto> users = userService.search(request);
        final PagingRequest page = request.getPaging();
        return Response.ok(
                PagingResponse.<UserDto>builder()
                        .contents(users.getContent())
                        .paging(new PageableData()
                                .setPageNumber(page.getPage() - 1)
                                .setTotalPage(users.getTotalPages())
                                .setPageSize(page.getSize())
                                .setTotalRecord(users.getTotalElements())
                        )
                        .build()
        );
    }

//    @Override
//    public ResponseEntity<?> logout(HttpServletRequest request) {
//        userService.logoutUser(request);
//        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công."));
//    }
}
