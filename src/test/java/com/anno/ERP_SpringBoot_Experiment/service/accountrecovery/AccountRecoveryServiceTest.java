package com.anno.ERP_SpringBoot_Experiment.service.accountrecovery;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private RecoveryTokenStore recoveryTokenStore;

  private AccountRecoveryService accountRecoveryService;

  @BeforeEach
  void setUp() {
    accountRecoveryService = new AccountRecoveryService(userRepository, recoveryTokenStore);
  }

  @Test
  void issue_CreatesNewTokenWhenEmailHasNoToken() {
    User user = activeUser("user@example.com");

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(recoveryTokenStore.findTokenByEmail("user@example.com")).thenReturn(Optional.empty());

    var result = accountRecoveryService.issue("user@example.com");

    assertThat(result.user()).isSameAs(user);
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.token()).isNotBlank();
    verify(recoveryTokenStore).save(eq("user@example.com"), eq(result.token()), eq(Duration.ofHours(24)));
  }

  @Test
  void issue_ReusesAndExtendsExistingToken() {
    User user = activeUser("user@example.com");

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(recoveryTokenStore.findTokenByEmail("user@example.com")).thenReturn(Optional.of("existing-token"));

    var result = accountRecoveryService.issue("user@example.com");

    assertThat(result.token()).isEqualTo("existing-token");
    verify(recoveryTokenStore).extend("user@example.com", "existing-token", Duration.ofHours(24));
  }

  @Test
  void issue_RejectsInactiveUser() {
    User user = activeUser("user@example.com");
    user.setStatus(ActiveStatus.INACTIVE);

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> accountRecoveryService.issue("user@example.com"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    verifyNoInteractions(recoveryTokenStore);
  }

  @Test
  void resolve_ReturnsUserFromToken() {
    User user = activeUser("user@example.com");

    when(recoveryTokenStore.findEmailByToken("token")).thenReturn(Optional.of("user@example.com"));
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    var result = accountRecoveryService.resolve("token");

    assertThat(result.user()).isSameAs(user);
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.token()).isEqualTo("token");
  }

  @Test
  void resolve_RejectsExpiredToken() {
    when(recoveryTokenStore.findEmailByToken("expired")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountRecoveryService.resolve("expired"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void consume_DeletesRecoveryToken() {
    User user = activeUser("user@example.com");
    var recoveryToken = new RecoveryToken(user, "token", "user@example.com");

    accountRecoveryService.consume(recoveryToken);

    verify(recoveryTokenStore).consume("user@example.com", "token");
  }

  private User activeUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setStatus(ActiveStatus.ACTIVE);
    return user;
  }
}
