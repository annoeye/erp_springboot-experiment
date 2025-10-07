package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSender implements CommandLineRunner {
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            return;
        }

        final var account = new User();
        account.setId(UUID.randomUUID());
        account.setFullName("Ngô Ngọc Định");
        account.setName("ADMIN");
        account.setEmail("ADMIN@gmail.com");
        account.setPassword(passwordEncoder.encode("admin"));
        account.setStatus(ActiveStatus.ACTIVE);
        account.setRoles(
                (Set.of(RoleType.ADMIN, RoleType.USER, RoleType.SUPER_ADMIN)));

        this.userRepository.save(account);
    }
}
