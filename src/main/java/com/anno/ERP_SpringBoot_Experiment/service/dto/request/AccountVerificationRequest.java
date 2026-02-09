package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để xác thực tài khoản (đổi mật khẩu).
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class AccountVerificationRequest {

    /**
     * Mật khẩu mới (bắt buộc, ít nhất 6 ký tự).
     */
    @JsonProperty("new_password")
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    String newPassword;

    /**
     * Xác nhận mật khẩu mới (bắt buộc).
     */
    @JsonProperty("confirm_password")
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;
}
