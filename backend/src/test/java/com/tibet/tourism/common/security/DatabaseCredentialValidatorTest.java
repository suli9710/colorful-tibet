package com.tibet.tourism.common.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseCredentialValidatorTest {

    @Test
    void strictModeRejectsRootUser() {
        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("root", "strong-local-db-password", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be root");
    }

    @Test
    void prodProfileRejectsRootUserEvenWhenStrictFlagIsFalse() {
        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("ROOT", "strong-local-db-password", false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be root");
    }

    @Test
    void strictModeRejectsBlankPassword() {
        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("colorful_tibet_app", " ", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password must be configured");
    }

    @Test
    void strictModeRejectsKnownPublishedPasswordDefaults() {
        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("colorful_tibet_app", "031224", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published default");

        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("colorful_tibet_app", "root123456", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published default");
    }

    @Test
    void strictModeRejectsPlaceholderPasswords() {
        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("colorful_tibet_app", "change-me", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");

        assertThatThrownBy(() -> DatabaseCredentialValidator.validate("colorful_tibet_app", "replace-with-random", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void strictModeAcceptsLeastPrivilegedUserWithNonPlaceholderPassword() {
        assertThatCode(() -> DatabaseCredentialValidator.validate(
                "colorful_tibet_app",
                "local-random-db-password-2026",
                true,
                false)).doesNotThrowAnyException();
    }

    @Test
    void nonStrictLocalAndTestProfilesAllowBlankPasswordForLightweightTests() {
        assertThatCode(() -> DatabaseCredentialValidator.validate(
                "colorful_tibet_app",
                "",
                false,
                false)).doesNotThrowAnyException();
    }
}
