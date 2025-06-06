package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements iUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String createUser(UserRegister body) {

        userRepository.findByEmail(body.getEmail()).ifPresent(existingUser -> {
            throw new CustomException("Email đã tồn tại.", HttpStatus.CONFLICT);
        });
        userRepository.findByUsername(body.getUserName()).ifPresent(existingUser -> {
            throw new CustomException("Tên đăng nhập đã tồn tại.", HttpStatus.CONFLICT);
        });

            User user = User.builder()
            .userName(body.getUserName())
            .email(body.getEmail())
            .password(passwordEncoder.encode(body.getPassword()))
            .active(User.Active.INACTIVE)
            .build();

            userRepository.save(user);
            return "Yêu cầu xác nhận tài khoản.";
    }

    @Override
    public String loginUser(UserLogin body) {

        User user; String usernameOrEmail = body.getUserName();
        Optional<User> foundUser = userRepository.findByUsername(usernameOrEmail);

        user = foundUser.orElseGet(() -> userRepository.findByByEmail(usernameOrEmail)
                .orElseThrow(() -> new CustomException("Tên đăng nhập hoặc Email không tồn tại.", HttpStatus.NOT_FOUND)));

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu không đúng.", HttpStatus.UNAUTHORIZED);
        }

        return "Đăng nhập thành công!";
    }

    private boolean isEmailFormat(String input) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return input != null && input.matches(emailRegex);
    }
}