package com.tibet.tourism.modules.hotel.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.hotel.application.HotelBookingService;
import com.tibet.tourism.modules.hotel.web.dto.PublicHotelResponse;
import com.tibet.tourism.modules.hotel.web.dto.PublicRoomTypeResponse;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HotelPublicReadContractTest {

    @Mock
    private HotelBookingService hotelBookingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HotelBookingController controller = new HotelBookingController(
                hotelBookingService, userRepository, riskAssessmentService, passthroughAuditService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void hotelListReturnsPublicDtosOnly() throws Exception {
        when(hotelBookingService.getAllHotels()).thenReturn(List.of(publicHotel()));

        mockMvc.perform(get("/api/hotel-bookings/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].name").value("Lhasa Hotel"))
                .andExpect(jsonPath("$[0].location").value("Lhasa"))
                .andExpect(jsonPath("$[0].priceRange").value("800-1200"))
                .andExpect(jsonPath("$[0].rating").value(4.8))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/hotel.jpg"))
                .andExpect(jsonPath("$[0].facilities").value("wifi, oxygen"))
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].roomTypes").doesNotExist())
                .andExpect(content().string(not(containsString("13900139000"))));
    }

    @Test
    void hotelDetailReturnsPublicDtoOnly() throws Exception {
        when(hotelBookingService.getHotel(5L)).thenReturn(Optional.of(publicHotel()));

        mockMvc.perform(get("/api/hotel-bookings/hotels/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Lhasa Hotel"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.roomTypes").doesNotExist())
                .andExpect(content().string(not(containsString("13900139000"))));
    }

    @Test
    void roomTypesReturnPublicDtosOnly() throws Exception {
        when(hotelBookingService.getRoomTypes(5L)).thenReturn(List.of(new PublicRoomTypeResponse(
                8L,
                "Deluxe King",
                new BigDecimal("880.00"),
                2,
                "/images/room.jpg",
                "oxygen, breakfast")));

        mockMvc.perform(get("/api/hotel-bookings/room-types/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].name").value("Deluxe King"))
                .andExpect(jsonPath("$[0].price").value(880.00))
                .andExpect(jsonPath("$[0].capacity").value(2))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/room.jpg"))
                .andExpect(jsonPath("$[0].amenities").value("oxygen, breakfast"))
                .andExpect(jsonPath("$[0].hotel").doesNotExist())
                .andExpect(jsonPath("$[0].sortOrder").doesNotExist());
    }

    @Test
    void hotelDetailReturnsNotFoundForMissingHotel() throws Exception {
        when(hotelBookingService.getHotel(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/hotel-bookings/hotels/404"))
                .andExpect(status().isNotFound());
    }

    private PublicHotelResponse publicHotel() {
        return new PublicHotelResponse(
                5L,
                "Lhasa Hotel",
                "Lhasa",
                "800-1200",
                new BigDecimal("4.8"),
                "/images/hotel.jpg",
                "wifi, oxygen");
    }
}
