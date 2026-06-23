package com.anno.ERP_SpringBoot_Experiment.service.dto.response;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.model.enums.UserRank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyProfileResponse {
    String username;
    String fullName;
    String email;
    String phoneNumber;
    String avatarUrl;
    Date dateOfBirth;
    Gender gender;
    UserRank rank;
    ActiveStatus status;
    Set<RoleType> roles;
}
