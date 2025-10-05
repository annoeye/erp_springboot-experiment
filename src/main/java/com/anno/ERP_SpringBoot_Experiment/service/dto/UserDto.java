package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {
    UUID id;
    String username;
    String fullName;
    String email;
    String numberPhone;
    Date dateOfBirth;
    Gender gender;
    String avatarUrl;
    ActiveStatus active;
    Set<RoleType> roles;
}
