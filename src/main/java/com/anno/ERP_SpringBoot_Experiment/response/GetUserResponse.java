package com.anno.ERP_SpringBoot_Experiment.response;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUserResponse {
    UUID id;
    String username;
    String fullName;
    String email;
    String numberPhone;
    Date dateOfBirth;
    Gender gender;
    String avatarUrl;
    ActiveStatus active;
    Set<String> roles;

}
