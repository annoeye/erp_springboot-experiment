package com.anno.ERP_SpringBoot_Experiment.response;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.query.Page;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUserResponse {

    int totalPages;
    int totalElements;
    int number;
    int numberOfElements;
    List<User> content;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class User{
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
}
