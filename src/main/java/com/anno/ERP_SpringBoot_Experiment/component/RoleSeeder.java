package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Role;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void seedRoles() {
        List<RoleType> missingRoles = Arrays.stream(RoleType.values())
                .filter(roleType -> roleRepository.findByName(roleType.name()).isEmpty())
                .toList();

        if (missingRoles.isEmpty()) {
            log.info("✅ Tất cả Role đã tồn tại.");
            return;
        }

        missingRoles.forEach(roleType -> {
            Role role = new Role();
            role.setName(roleType.name());
            roleRepository.save(role);
            log.info("➕ Đã thêm Role: {}", roleType.name());
        });
    }
}
