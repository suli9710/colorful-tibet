package com.tibet.tourism.modules.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class UserSerializationPrivacyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userSerializationOmitsSensitiveAndInternalFields() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("login-name");
        user.setPassword("hashed-password");
        user.setNickname("Public Nickname");
        user.setPhone("13800138000");
        user.setIpAddress("203.0.113.99");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");
        user.setSessionVersion(9L);
        user.setMustChangePassword(true);

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).contains("\"id\":7");
        assertThat(json).doesNotContain(
                "password",
                "hashed-password",
                "phone",
                "13800138000",
                "ipAddress",
                "203.0.113.99",
                "allowedLoginFingerprintHash",
                "fingerprint-hash",
                "sessionVersion",
                "mustChangePassword");
    }
}
