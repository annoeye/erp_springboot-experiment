package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
     * Mật khẩu mới.
     */
    @JsonProperty("new_password")
    String newPassword;

    /**
     * Xác nhận mật khẩu mới.
     */
    @JsonProperty("confirm_password")
    String confirmPassword;
}
