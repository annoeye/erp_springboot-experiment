package com.anno.ERP_SpringBoot_Experiment.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class UserRegister {
    @NotBlank(message = "Tên dùng để đăng nhập không được để trống")
    @Size(min = 3, max = 20, message = "Đảm bảo nhập đúng độ dài của Tên đăng nhập! 3 => Name <= 20.")
    String userName;

    @NotBlank(message = "Mật khẩu không được để trống.")
    String password;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ!")
    String email;
}