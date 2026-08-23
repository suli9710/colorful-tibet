package com.tibet.tourism.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.modules.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AdminMfaPolicyTest {

    private static final String SUPER_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final String ADMIN_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    private static final String EDITOR_SECRET = "MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK";

    private TotpService totpService;
    private CacheKeyHasher cacheKeyHasher;

    @BeforeEach
    void setUp() {
        cacheKeyHasher = new CacheKeyHasher("admin-mfa-policy-test-cache-key");
        totpService = new TotpService(
                (org.springframework.data.redis.core.StringRedisTemplate) null,
                cacheKeyHasher);
    }

    @Test
    void ordinaryAdministratorRequiresItsOwnConfiguredCode() {
        AdminMfaPolicy policy = policy("admin=" + ADMIN_SECRET);
        User admin = user("ADMIN", User.Role.ADMIN);

        assertThat(policy.verify(admin, "")).isEqualTo(AdminMfaPolicy.Verification.REQUIRED);

        String currentCode = totpService.generateCodeForTime(ADMIN_SECRET, Instant.now());
        assertThat(policy.verify(admin, currentCode)).isEqualTo(AdminMfaPolicy.Verification.VERIFIED);
    }

    @Test
    void administratorWithoutSecretFailsClosed() {
        AdminMfaPolicy policy = policy("");

        assertThat(policy.verify(user("admin", User.Role.ADMIN), "123456"))
                .isEqualTo(AdminMfaPolicy.Verification.UNCONFIGURED);
    }

    @Test
    void administratorCannotUseAnotherAdministratorsCode() {
        AdminMfaPolicy policy = policy("admin=" + ADMIN_SECRET + ";editor=" + EDITOR_SECRET);
        String editorCode = totpService.generateCodeForTime(EDITOR_SECRET, Instant.now());

        assertThat(policy.verify(user("admin", User.Role.ADMIN), editorCode))
                .isEqualTo(AdminMfaPolicy.Verification.INVALID);
    }

    @Test
    void acceptedCodeCannotBeReplayedForTheSameAdministrator() {
        AdminMfaPolicy policy = policy("admin=" + ADMIN_SECRET);
        User admin = user("admin", User.Role.ADMIN);
        String currentCode = totpService.generateCodeForTime(ADMIN_SECRET, Instant.now());

        assertThat(policy.verify(admin, currentCode)).isEqualTo(AdminMfaPolicy.Verification.VERIFIED);
        assertThat(policy.verify(admin, currentCode)).isEqualTo(AdminMfaPolicy.Verification.INVALID);
    }

    @Test
    void configuredSuperAdminUsesDedicatedLegacySecret() {
        AdminMfaPolicy policy = policy("admin=" + ADMIN_SECRET);
        String currentCode = totpService.generateCodeForTime(SUPER_SECRET, Instant.now());

        assertThat(policy.verify(user("LZH", User.Role.ADMIN), currentCode))
                .isEqualTo(AdminMfaPolicy.Verification.VERIFIED);
    }

    @Test
    void ordinaryUserDoesNotRequireMfaEvenIfANameIsConfigured() {
        AdminMfaPolicy policy = policy("traveler=" + ADMIN_SECRET);

        assertThat(policy.verify(user("traveler", User.Role.USER), ""))
                .isEqualTo(AdminMfaPolicy.Verification.NOT_REQUIRED);
    }

    @Test
    void opaqueBindingChangesWhenNamedAdministratorSecretRotates() {
        AdminMfaPolicy original = policy("admin=" + ADMIN_SECRET);
        AdminMfaPolicy rotated = policy("admin=" + EDITOR_SECRET);

        String originalBinding = original.currentBinding("ADMIN");
        String rotatedBinding = rotated.currentBinding("admin");

        assertThat(originalBinding)
                .matches("[0-9a-f]{32}")
                .doesNotContain("admin", ADMIN_SECRET);
        assertThat(rotatedBinding).matches("[0-9a-f]{32}").isNotEqualTo(originalBinding);
        assertThat(original.matchesCurrentBinding("admin", originalBinding)).isTrue();
        assertThat(rotated.matchesCurrentBinding("admin", originalBinding)).isFalse();
    }

    @Test
    void missingCredentialCannotMatchAnOldBinding() {
        String oldBinding = policy("admin=" + ADMIN_SECRET).currentBinding("admin");

        AdminMfaPolicy credentialRemoved = policy("");

        assertThat(credentialRemoved.currentBinding("admin")).isEmpty();
        assertThat(credentialRemoved.matchesCurrentBinding("admin", oldBinding)).isFalse();
        assertThat(credentialRemoved.matchesCurrentBinding("admin", "")).isFalse();
    }

    @Test
    void superAdministratorBindingTracksDedicatedSecretRotation() {
        AdminMfaPolicy original = policy(SUPER_SECRET, "admin=" + ADMIN_SECRET);
        AdminMfaPolicy rotated = policy(EDITOR_SECRET, "admin=" + ADMIN_SECRET);

        String oldBinding = original.currentBinding("LZH");

        assertThat(oldBinding).matches("[0-9a-f]{32}");
        assertThat(original.matchesCurrentBinding("lzh", oldBinding)).isTrue();
        assertThat(rotated.currentBinding("lzh")).isNotEqualTo(oldBinding);
        assertThat(rotated.matchesCurrentBinding("lzh", oldBinding)).isFalse();
    }

    @Test
    void equivalentSecretFormattingProducesTheSameBinding() {
        AdminMfaPolicy canonical = policy("admin=" + ADMIN_SECRET);
        AdminMfaPolicy formatted = policy("admin=" + ADMIN_SECRET.toLowerCase().replaceAll("(.{8})(?!$)", "$1-"));

        assertThat(formatted.currentBinding("admin")).isEqualTo(canonical.currentBinding("admin"));
    }

    @Test
    void rejectsSecretReusedBySuperAdminWithDifferentFormatting() {
        assertThatThrownBy(() -> policy("admin=GEZD-GNBV-GY3T-QOJQ-GEZD-GNBV-GY3T-QOJQ"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("independent");
    }

    @Test
    void rejectsSecretReusedByTwoNamedAdministrators() {
        assertThatThrownBy(() -> policy("admin=" + ADMIN_SECRET + ";editor=" + ADMIN_SECRET.toLowerCase()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("independent");
    }

    @Test
    void rejectsDuplicateNormalizedUsernames() {
        assertThatThrownBy(() -> policy("Admin=" + ADMIN_SECRET + ";admin=" + SUPER_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate usernames");
    }

    @Test
    void rejectsEmptyOrMalformedMapEntries() {
        assertThatThrownBy(() -> policy("admin=" + ADMIN_SECRET + ";"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty entries");
        assertThatThrownBy(() -> policy("admin=" + ADMIN_SECRET + ";;editor=" + EDITOR_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty entries");
        assertThatThrownBy(() -> policy("admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username=Base32Secret");
    }

    @Test
    void rejectsWeakOrPlaceholderNamedSecrets() {
        assertThatThrownBy(() -> policy("admin=JBSWY3DPEHPK3PXP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128 bits");

        assertThatThrownBy(() -> policy("admin=" + ADMIN_SECRET + "A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonical Base32 encoding");
        assertThatThrownBy(() -> policy("admin=replace-with-another-base32-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void strictModeRejectsPublishedOrLowDiversitySecrets() {
        assertThatThrownBy(() -> new AdminMfaPolicy(
                totpService,
                cacheKeyHasher,
                "lzh",
                SUPER_SECRET,
                "",
                true,
                new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("high-entropy");

        assertThatThrownBy(() -> new AdminMfaPolicy(
                totpService,
                cacheKeyHasher,
                "lzh",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
                "admin=ABABABABABABABABABABABABABABABAB",
                true,
                new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("high-entropy");
    }

    private AdminMfaPolicy policy(String namedSecrets) {
        return policy(SUPER_SECRET, namedSecrets);
    }

    private AdminMfaPolicy policy(String superSecret, String namedSecrets) {
        return new AdminMfaPolicy(
                totpService,
                cacheKeyHasher,
                "lzh",
                superSecret,
                namedSecrets,
                false,
                new MockEnvironment());
    }

    private static User user(String username, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
