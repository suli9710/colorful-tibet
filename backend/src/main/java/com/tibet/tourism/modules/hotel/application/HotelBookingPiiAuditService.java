package com.tibet.tourism.modules.hotel.application;

import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.user.domain.User;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HotelBookingPiiAuditService {

    private static final Logger logger = LoggerFactory.getLogger(HotelBookingPiiAuditService.class);
    private static final String METRIC_REVEAL_EVENTS = "app.security.hotel.booking.pii.reveal.events";
    private static final String TAG_RESULT = "result";
    private static final String TAG_REASON = "reason";
    private static final String REASON_NONE = "none";
    private static final String REASON_UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;

    @Autowired
    public HotelBookingPiiAuditService(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(meterRegistryProvider.getIfAvailable());
    }

    HotelBookingPiiAuditService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRevealAllowed(User actor, HotelBooking booking) {
        recordRevealMetric("allowed", REASON_NONE);
        logger.atInfo()
                .addKeyValue("security_event", "hotel_booking_pii_reveal")
                .addKeyValue("result", "allowed")
                .addKeyValue("actor_user_id", userId(actor))
                .addKeyValue("actor_role", userRole(actor))
                .addKeyValue("booking_id", bookingId(booking))
                .addKeyValue("booking_owner_user_id", ownerId(booking))
                .log("Hotel booking PII reveal audited");
    }

    public void recordRevealRejected(User actor, Long bookingId, String reason) {
        recordRevealMetric("rejected", normalizeReason(reason));
        logger.atWarn()
                .addKeyValue("security_event", "hotel_booking_pii_reveal")
                .addKeyValue("result", "rejected")
                .addKeyValue("reason", reason)
                .addKeyValue("actor_user_id", userId(actor))
                .addKeyValue("actor_role", userRole(actor))
                .addKeyValue("booking_id", bookingId)
                .log("Hotel booking PII reveal rejected");
    }

    private void recordRevealMetric(String result, String reason) {
        if (meterRegistry == null) {
            return;
        }
        try {
            meterRegistry.counter(METRIC_REVEAL_EVENTS, TAG_RESULT, result, TAG_REASON, reason).increment();
        } catch (RuntimeException ex) {
            logger.atWarn()
                    .addKeyValue("security_event", "hotel_booking_pii_reveal")
                    .addKeyValue("metric", METRIC_REVEAL_EVENTS)
                    .addKeyValue("result", result)
                    .addKeyValue("reason", reason)
                    .addKeyValue("failure", ex.getClass().getSimpleName())
                    .log("Hotel booking PII reveal metric failed");
        }
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? REASON_UNKNOWN : reason;
    }

    private Long userId(User user) {
        return user == null ? null : user.getId();
    }

    private String userRole(User user) {
        return user == null || user.getRole() == null ? null : user.getRole().name();
    }

    private Long bookingId(HotelBooking booking) {
        return booking == null ? null : booking.getId();
    }

    private Long ownerId(HotelBooking booking) {
        return booking == null || booking.getUser() == null ? null : booking.getUser().getId();
    }
}
