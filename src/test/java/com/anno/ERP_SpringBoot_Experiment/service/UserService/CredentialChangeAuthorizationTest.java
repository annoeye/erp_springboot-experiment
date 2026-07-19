package com.anno.ERP_SpringBoot_Experiment.service.UserService;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.service.accountrecovery.AccountRecoveryService;
import com.anno.ERP_SpringBoot_Experiment.service.accountrecovery.RecoveryToken;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialChangeAuthorizationTest {

  @Mock
  private AccountRecoveryService accountRecoveryService;
  @Mock
  private SecurityUtil securityUtil;

  private CredentialChangeAuthorization authorization;

  @BeforeEach
  void setUp() {
    authorization = new CredentialChangeAuthorization(accountRecoveryService, securityUtil);
  }

  @Test
  void resolveFromRecoveryToken_ReturnsUserFromTokenEmail() {
    User user = new User();
    user.setEmail("user@example.com");
    var recoveryToken = new RecoveryToken(user, "abc", "user@example.com");

    when(accountRecoveryService.resolve("abc")).thenReturn(recoveryToken);

    var result = authorization.resolveFromRecoveryToken("abc");

    assertThat(result.user()).isSameAs(user);
    assertThat(result.recoveryToken()).isSameAs(recoveryToken);
    assertThat(result.recoveryTokenBased()).isTrue();
  }

  @Test
  void resolveFromRecoveryToken_RejectsMissingToken() {
    when(accountRecoveryService.resolve(" ")).thenThrow(
        new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Token khôi phục không được để trống."));

    assertThatThrownBy(() -> authorization.resolveFromRecoveryToken(" "))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void resolveFromRecoveryToken_RejectsExpiredToken() {
    when(accountRecoveryService.resolve("expired")).thenThrow(
        new BusinessException(ErrorCode.INVALID_CREDENTIALS,
            "Token khôi phục không hợp lệ hoặc đã hết hạn."));

    assertThatThrownBy(() -> authorization.resolveFromRecoveryToken("expired"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void resolveFromRecoveryTokenOrSession_FallsBackToCurrentUser() {
    User user = new User();
    user.setEmail("session@example.com");

    when(securityUtil.getCurrentUser()).thenReturn(Optional.of(user));

    var result = authorization.resolveFromRecoveryTokenOrSession(null);

    assertThat(result.user()).isSameAs(user);
    assertThat(result.recoveryTokenBased()).isFalse();
  }

  @Test
  void consumeRecoveryToken_DeletesBothRecoveryKeys() {
    User user = new User();
    user.setEmail("user@example.com");
    var recoveryToken = new RecoveryToken(user, "abc", "user@example.com");

    when(accountRecoveryService.resolve("abc")).thenReturn(recoveryToken);

    var result = authorization.resolveFromRecoveryToken("abc");
    authorization.consumeRecoveryToken(result);

    verify(accountRecoveryService).consume(recoveryToken);
  }
}
