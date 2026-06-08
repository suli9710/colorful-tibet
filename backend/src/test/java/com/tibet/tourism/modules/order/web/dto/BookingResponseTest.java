package com.tibet.tourism.modules.order.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingResponseTest {

    @Test
    void fromEntityWhitelistsPublicFieldsOnly() throws Exception {
        User user = new User();
        user.setId(12L);
        user.setUsername("traveler");
        user.setNickname("Tibet Guest");
        user.setPhone("18800001111");
        user.setIpAddress("ip-hash");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");

        ScenicSpot spot = new ScenicSpot();
        spot.setId(34L);
        spot.setName("Potala Palace");
        spot.setLocation("Lhasa");
        spot.setImageUrl("/images/potala.jpg");

        Booking booking = new Booking();
        booking.setId(56L);
        booking.setUser(user);
        booking.setSpot(spot);
        booking.setVisitDate(LocalDate.of(2026, 6, 1));
        booking.setTicketCount(2);
        booking.setTotalPrice(new BigDecimal("400.00"));
        booking.setStatus(Booking.Status.PENDING);

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(BookingResponse.fromEntity(booking));

        assertThat(json).contains("Potala Palace");
        assertThat(json)
                .doesNotContain("user")
                .doesNotContain("username")
                .doesNotContain("traveler")
                .doesNotContain("Tibet Guest")
                .doesNotContain("phone")
                .doesNotContain("18800001111")
                .doesNotContain("ipAddress")
                .doesNotContain("ip-hash")
                .doesNotContain("allowedLoginFingerprintHash")
                .doesNotContain("fingerprint-hash");
    }
}
