package com.anno.ERP_SpringBoot_Experiment.model.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.DeviceInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class UserLogin {
    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập hoặc email phải từ 3 đến 50 ký tự.")
    String usernameOrEmail;

    @NotBlank(message = "Mật khẩu không được để trống.")
    String password;

    @NotBlank(message = "Thông tin thiết bị không được để trống.")
    DeviceInfo deviceInfo;
}