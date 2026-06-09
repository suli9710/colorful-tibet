package com.tibet.tourism.modules.hotel.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.user.domain.User;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class HotelBookingPiiAuditServiceTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final HotelBookingPiiAuditService auditService = new HotelBookingPiiAuditService(meterRegistry);

    @Test
    void revealAllowedEmitsStructuredSecurityEventWithoutRawPii() {
        User actor = user(2L, User.Role.ADMIN);
        User owner = user(1L, User.Role.USER);
        HotelBooking booking = new HotelBooking();
        booking.setId(99L);
        booking.setUser(owner);
        booking.setGuestName("Alice Zhang");
        booking.setPhone("13800138000");
        booking.setNote("Late arrival with oxygen request");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            auditService.recordRevealAllowed(actor, booking);

            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.get(0);
            assertThat(event.getFormattedMessage()).isEqualTo("Hotel booking PII reveal audited");
            assertKeyValue(event, "security_event", "hotel_booking_pii_reveal");
            assertKeyValue(event, "result", "allowed");
            assertKeyValue(event, "actor_user_id", 2L);
            assertKeyValue(event, "booking_id", 99L);
            assertKeyValue(event, "booking_owner_user_id", 1L);
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                    .doesNotContain("Alice Zhang")
                    .doesNotContain("13800138000")
                    .doesNotContain("oxygen request");
            assertRevealMetric("allowed", "none", 1.0);
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void revealRejectedEmitsReasonWithoutRawPii() {
        User actor = user(3L, User.Role.USER);
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            auditService.recordRevealRejected(actor, 100L, "not_admin");

            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.get(0);
            assertThat(event.getFormattedMessage()).isEqualTo("Hotel booking PII reveal rejected");
            assertKeyValue(event, "security_event", "hotel_booking_pii_reveal");
            assertKeyValue(event, "result", "rejected");
            assertKeyValue(event, "reason", "not_admin");
            assertKeyValue(event, "actor_user_id", 3L);
            assertKeyValue(event, "booking_id", 100L);
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                    .doesNotContain("Alice")
                    .doesNotContain("13800138000");
            assertRevealMetric("rejected", "not_admin", 1.0);
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void revealRejectedEmitsExpectedReasonsWithoutRawPrincipalData() {
        User actor = user(3L, User.Role.ADMIN);
        actor.setUsername("alice.secret");
        actor.setPassword("raw.jwt.token");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            String[] reasons = {"unauthenticated", "missing_authority", "not_admin", "not_found"};
            for (String reason : reasons) {
                auditService.recordRevealRejected(actor, 100L, reason);
            }

            assertThat(appender.list).hasSize(4);
            for (int index = 0; index < reasons.length; index++) {
                ILoggingEvent event = appender.list.get(index);
                assertThat(event.getFormattedMessage()).isEqualTo("Hotel booking PII reveal rejected");
                assertKeyValue(event, "security_event", "hotel_booking_pii_reveal");
                assertKeyValue(event, "result", "rejected");
                assertKeyValue(event, "reason", reasons[index]);
                assertKeyValue(event, "actor_user_id", 3L);
                assertKeyValue(event, "actor_role", "ADMIN");
                assertKeyValue(event, "booking_id", 100L);
                assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                        .doesNotContain("alice.secret")
                        .doesNotContain("raw.jwt.token");
                assertRevealMetric("rejected", reasons[index], 1.0);
            }
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void revealRejectedNormalizesBlankMetricReason() {
        auditService.recordRevealRejected(null, 100L, " ");

        assertRevealMetric("rejected", "unknown", 1.0);
    }

    private void assertRevealMetric(String result, String reason, double count) {
        assertThat(meterRegistry.get("app.security.hotel.booking.pii.reveal.events")
                .tag("result", result)
                .tag("reason", reason)
                .counter()
                .count()).isEqualTo(count);
    }

    private void assertKeyValue(ILoggingEvent event, String key, Object value) {
        assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
            assertThat(pair.key).isEqualTo(key);
            assertThat(pair.value).isEqualTo(value);
        });
    }

    private User user(Long id, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(HotelBookingPiiAuditService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(HotelBookingPiiAuditService.class);
        logger.detachAppender(appender);
    }
}
