package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndGetFineractClientId() {
        User user = User.builder()
                .fullName("John Doe")
                .name("johndoe")
                .password("securePassword123")
                .email("john.doe@example.com")
                .status(ActiveStatus.ACTIVE)
                .fineractClientId("12345")
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        User fetchedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertThat(fetchedUser).isNotNull();
        assertThat(fetchedUser.getFineractClientId()).isEqualTo("12345");
    }
}
