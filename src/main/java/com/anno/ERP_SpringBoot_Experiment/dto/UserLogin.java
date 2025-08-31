package com.anno.ERP_SpringBoot_Experiment.dto;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "Thông tin thiết bị không được để trống.")
    @Valid
    DeviceInfo deviceInfo;
}