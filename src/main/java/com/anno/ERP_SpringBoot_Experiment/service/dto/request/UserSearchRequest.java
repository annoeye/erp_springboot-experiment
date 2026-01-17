package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.UserSpecification;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.utils.FilterRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSearchRequest extends FilterRequest<User> {
    String fullName;
    String email;
    String numberPhone;

    @Override
    public Specification<User> specification() {
        return UserSpecification.builder()
                .withFullName(fullName)
                .withEmail(email)
                .withPhoneNumber(numberPhone)
                .build();
    }
}
