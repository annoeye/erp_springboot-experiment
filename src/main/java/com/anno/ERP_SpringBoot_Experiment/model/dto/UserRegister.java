package com.anno.ERP_SpringBoot_Experiment.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegister {
    @NotEmpty(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String userName;

    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotEmpty(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotEmpty(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}