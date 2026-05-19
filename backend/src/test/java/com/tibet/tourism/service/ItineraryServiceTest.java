package com.tibet.tourism.service;

import com.tibet.tourism.dto.itinerary.BookItineraryItemRequest;
import com.tibet.tourism.dto.itinerary.GenerateItineraryRequest;
import com.tibet.tourism.dto.itinerary.ItineraryResponse;
import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock private ItineraryRepository itineraryRepository;
    @Mock private ItineraryItemRepository itineraryItemRepository;
    @Mock private ScenicSpotRepository scenicSpotRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private HotelBookingService hotelBookingService;
    @Mock private OrderCenterService orderCenterService;

    private ItineraryService itineraryService;
    private User user;
    private ScenicSpot potala;
    private Hotel hotel;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(
                itineraryRepository,
                itineraryItemRepository,
                scenicSpotRepository,
                hotelRepository,
                roomTypeRepository,
                bookingRepository,
                hotelBookingService,
                orderCenterService
        );

        user = new User();
        user.setId(1L);
        user.setUsername("traveler");
        user.setNickname("Traveler");

        potala = new ScenicSpot();
        potala.setId(10L);
        potala.setName("布达拉宫");
        potala.setLocation("拉萨");
        potala.setCategory(ScenicSpot.Category.CULTURAL);
        potala.setTicketPrice(BigDecimal.valueOf(200));
        potala.setAltitude("3650米");
        potala.setVisitCount(1000);

        hotel = new Hotel();
        hotel.setId(20L);
        hotel.setName("拉萨精选酒店");
        hotel.setLocation("拉萨");

        roomType = new RoomType();
        roomType.setId(30L);
        roomType.setHotel(hotel);
        roomType.setName("舒适双床房");
        roomType.setPrice(BigDecimal.valueOf(520));
        roomType.setCapacity(2);
    }

    @Test
    void generateItineraryCreatesStructuredBookableDays() {
        when(scenicSpotRepository.findAllWithoutTags()).thenReturn(List.of(potala));
        when(hotelRepository.findAll()).thenReturn(List.of(hotel));
        when(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(20L)).thenReturn(List.of(roomType));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateItineraryRequest request = new GenerateItineraryRequest();
        request.setDays(2);
        request.setBudget("comfort");
        request.setPreference("cultural");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setTravelers(2);

        ItineraryResponse response = itineraryService.generateItinerary(user, request);

        assertEquals(2, response.itineraryDays().size());
        assertTrue(response.totalEstimatedCost().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.itineraryDays().get(0).items().stream()
                .anyMatch(item -> "BOOK_SPOT".equals(item.bookingAction())));
        assertTrue(response.itineraryDays().get(0).items().stream()
                .anyMatch(item -> "BOOK_HOTEL".equals(item.bookingAction())));
    }

    @Test
    void bookSpotItemCreatesBookingAndMarksItemBooked() {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(100L);
        itinerary.setUser(user);

        ItineraryDay day = new ItineraryDay();
        day.setId(101L);
        day.setItinerary(itinerary);
        day.setTravelDate(LocalDate.now().plusDays(1));

        ItineraryItem item = new ItineraryItem();
        item.setId(102L);
        item.setDay(day);
        item.setItemType(ItineraryItem.ItemType.SCENIC_SPOT);
        item.setTitle("布达拉宫");
        item.setScenicSpot(potala);
        item.setBookingAction(ItineraryItem.BookingAction.BOOK_SPOT);
        item.setBookingStatus(ItineraryItem.BookingStatus.BOOKABLE);
        item.setEstimatedCost(BigDecimal.valueOf(200));

        when(itineraryItemRepository.findById(102L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(900L);
            return booking;
        });

        BookItineraryItemRequest request = new BookItineraryItemRequest();
        request.setTravelers(2);

        var response = itineraryService.bookItem(user, 100L, 102L, request);

        assertEquals("SPOT_BOOKING", response.bookingType());
        assertEquals(900L, response.bookingId());
        assertEquals(ItineraryItem.BookingStatus.BOOKED, item.getBookingStatus());
        verify(itineraryItemRepository).save(item);
    }
}
