package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Role;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleSeeder {

    private static final Logger logger = LoggerFactory.getLogger(RoleSeeder.class);

    @Autowired
    RoleRepository roleRepository;

    @PostConstruct
    public void seedRoles() {
        List<RoleType> missingRoles = Arrays.stream(RoleType.values())
                .filter(roleType -> roleRepository.findByName(roleType).isEmpty())
                .toList();

        if (missingRoles.isEmpty()) {
            logger.info("✅ Tất cả Role đã tồn tại.");
            return;
        }

        missingRoles.forEach(roleType -> {
            Role role = new Role();
            role.setName(roleType.name());
            roleRepository.save(role);
            logger.info("➕ Đã thêm Role: {}", roleType.name());
        });
    }
}
