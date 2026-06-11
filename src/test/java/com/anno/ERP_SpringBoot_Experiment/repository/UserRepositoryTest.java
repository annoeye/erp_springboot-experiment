package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data slice test for {@link UserRepository}.
 * <p>
 * Uses an in-memory database (H2) and only loads JPA-related beans,
 * making tests fast and focused.
 */
@DataJpaTest
@DisplayName("UserRepository Data Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find user by email when exists")
    void findByEmail_whenUserExists_returnsUser() {
        // Arrange
        User user = User.builder()
                .fullName("Nguyen Van A")
                .email("nguyenvana@example.com")
                .password("test-password")
                .status(ActiveStatus.ACTIVE)
                .build();

        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByEmail("nguyenvana@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(found.get().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmail_whenUserDoesNotExist_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by auth code")
    void findByAuthCode_whenValidCode_returnsUser() {
        // Arrange
        User user = User.builder()
                .fullName("Tran Thi B")
                .email("tranthib@example.com")
                .password("test-password")
                .status(ActiveStatus.INACTIVE)
                .build();
        user.getAuthCode().setCode("abc-123-def");
        user.getAuthCode().setPurpose(ActiveStatus.EMAIL_VERIFICATION);

        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByAuthCode("abc-123-def");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("tranthib@example.com");
    }

    @Test
    @DisplayName("Should return empty when auth code does not exist")
    void findByAuthCode_whenInvalidCode_returnsEmpty() {
        Optional<User> found = userRepository.findByAuthCode("invalid-code");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by name or email")
    void findByNameOrEmail_whenMatchesEmail_returnsUser() {
        // Arrange
        User user = User.builder()
                .fullName("Le Van C")
                .email("levanc@example.com")
                .password("test-password")
                .status(ActiveStatus.ACTIVE)
                .build();

        entityManager.persistAndFlush(user);

        // Act — search by email
        Optional<User> found = userRepository.findByNameOrEmail("levanc@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Le Van C");
    }

    @Test
    @DisplayName("Should return empty for non-existent user")
    void findByNameOrEmail_whenNotExist_returnsEmpty() {
        Optional<User> found = userRepository.findByNameOrEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }
}
