package com.anno.ERP_SpringBoot_Experiment.web.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController authorization contract")
class AuthControllerAuthorizationTest {

    @Test
    @DisplayName("GET /api/auth/me should allow any authenticated user")
    void getMyProfileShouldAllowAuthenticatedUser() throws NoSuchMethodException {
        PreAuthorize authorization = AuthController.class
                .getMethod("getMyProfile")
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("isAuthenticated()");
        assertThat(authorization.value()).doesNotContain("!hasRole('USER')");
    }
}
