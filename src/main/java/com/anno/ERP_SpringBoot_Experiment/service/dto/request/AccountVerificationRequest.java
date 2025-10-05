package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class AccountVerificationRequest {
    /* =======  Change Password  ======= */
    @JsonProperty("new_password")
    String newPassword;
    @JsonProperty("confirm_password")
    String confirmPassword;
}
