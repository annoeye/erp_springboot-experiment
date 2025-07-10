package com.anno.ERP_SpringBoot_Experiment.response;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUserResponse {
    String id;
    String username;
    String fullName;
    String email;
    String password;
    String numberPhone;
    Date dateOfBirth;
    User.Gender gender;
    String avatarUrl;
    User.Active active;
    Set<String> roles;

}
