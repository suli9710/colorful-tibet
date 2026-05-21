package com.tibet.tourism.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private static final String RFC_6238_SHA1_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpService totpService = new TotpService();

    @Test
    void generatesRfc6238CompatibleSixDigitCode() {
        String code = totpService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.ofEpochSecond(59));

        assertThat(code).isEqualTo("287082");
    }

    @Test
    void validatesBase32SecretStrength() {
        assertThatCode(() -> totpService.validateSecret(RFC_6238_SHA1_SECRET)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidBase32Secret() {
        assertThatThrownBy(() -> totpService.validateSecret("invalid-secret!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid Base32");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> totpService.validateSecret("JBSWY3DPEHPK3PXP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128 bits");
    }

    @Test
    void rejectsNonNumericCode() {
        assertThat(totpService.isValidCode(RFC_6238_SHA1_SECRET, "abcdef")).isFalse();
    }
}
