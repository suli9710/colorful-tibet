package com.tibet.tourism.service;

import com.tibet.tourism.dto.specialty.HighlandAssessmentRequest;
import com.tibet.tourism.entity.Itinerary;
import com.tibet.tourism.entity.ItineraryDay;
import com.tibet.tourism.entity.ItineraryItem;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.TibetTravelKit;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.ItineraryRepository;
import com.tibet.tourism.repository.TibetTravelKitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TibetTravelKitServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private TibetTravelKitRepository travelKitRepository;

    private TibetTravelKitService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new TibetTravelKitService(itineraryRepository, travelKitRepository);
        user = new User();
        user.setId(1L);
        user.setUsername("traveler");
    }

    @Test
    void buildTravelKitUsesItineraryAltitudeAndMapPins() {
        Itinerary itinerary = itineraryWithHighAltitudeSpot();
        when(itineraryRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(itinerary));
        when(travelKitRepository.findFirstByItineraryIdAndUserIdAndValidUntilAfterOrderByGeneratedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(travelKitRepository.save(any(TibetTravelKit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.buildTravelKit(user, 100L);

        assertEquals(100L, response.itineraryId());
        assertEquals("HIGH", response.highlandAssessment().riskLevel());
        assertEquals(5200, response.highlandAssessment().maxAltitudeMeters());
        assertEquals(1, response.offlinePackage().mapPins().size());
        assertTrue(response.realtimeAlerts().stream().anyMatch(alert -> "ALTITUDE".equals(alert.type())));
        assertFalse(response.phrasebook().isEmpty());
        verify(travelKitRepository).save(any(TibetTravelKit.class));
    }

    @Test
    void assessManualRiskTreatsMedicalHistoryAndFastPaceAsExtreme() {
        HighlandAssessmentRequest request = new HighlandAssessmentRequest(
                null,
                68,
                2,
                "LOW",
                true,
                false,
                true,
                3,
                5000,
                "FAST"
        );

        var response = service.assessHighlandRisk(user, request);

        assertEquals("EXTREME", response.riskLevel());
        assertTrue(response.riskScore() >= 80);
        assertEquals(3, response.dailyAdvice().size());
    }

    @Test
    void cultureTipsAndPhrasebookCanBeFiltered() {
        var photoTips = service.getCultureTips("photography");
        var templePhrases = service.getPhrasebook("temple");

        assertEquals(1, photoTips.size());
        assertEquals("PHOTOGRAPHY", photoTips.get(0).scene());
        assertFalse(templePhrases.isEmpty());
        assertTrue(templePhrases.stream().allMatch(phrase -> "TEMPLE".equals(phrase.category())));
    }

    private Itinerary itineraryWithHighAltitudeSpot() {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(100L);
        itinerary.setUser(user);
        itinerary.setTitle("拉萨到珠峰适应行程");
        itinerary.setDays(2);
        itinerary.setPreference("natural");

        ItineraryDay day = new ItineraryDay();
        day.setId(101L);
        day.setDayNumber(2);
        day.setTitle("珠峰观景");
        day.setRegion("珠峰");

        ScenicSpot spot = new ScenicSpot();
        spot.setId(10L);
        spot.setName("珠峰大本营");
        spot.setAltitude("5200米");
        spot.setLatitude(BigDecimal.valueOf(28.141));
        spot.setLongitude(BigDecimal.valueOf(86.852));

        ItineraryItem item = new ItineraryItem();
        item.setId(102L);
        item.setItemType(ItineraryItem.ItemType.SCENIC_SPOT);
        item.setTitle("珠峰大本营");
        item.setAltitudeMeters(5200);
        item.setScenicSpot(spot);

        day.addItem(item);
        itinerary.addDay(day);
        return itinerary;
    }
}
