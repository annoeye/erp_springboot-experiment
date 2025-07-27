package com.anno.ERP_SpringBoot_Experiment.model.dto;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class ChangePassword {

    @NotBlank
    UUID id;

    @NotBlank(message = "Mã xác thực không được để trống")
    String codeResetPassword;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;

    @NotBlank(message = "Nhập lại mật khẩu không được để trống")
    String confirmPassword;

    public void validatePasswordsMatch() {
        if (!Objects.equals(password, confirmPassword)) {
            throw new CustomException("Mật khẩu và mật khẩu xác nhận phải trùng nhau.", HttpStatus.BAD_REQUEST);
        }
    }
}
