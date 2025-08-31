package com.anno.ERP_SpringBoot_Experiment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class AccountVerificationDto {
    /* =======  Change Password  ======= */
    @JsonProperty("new_password")
    String newPassword;
    @JsonProperty("confirm_password")
    String confirmPassword;
}
