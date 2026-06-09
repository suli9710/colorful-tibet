package com.tibet.tourism.modules.hotel.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingRequest;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class HotelBookingServiceTest {

    @Mock private HotelBookingRepository hotelBookingRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private OrderCenterService orderCenterService;
    @Mock private HotelBookingPiiAuditService piiAuditService;
    @Mock private HotelBookingPiiAccessGuard piiAccessGuard;

    private HotelBookingService hotelBookingService;
    private User user;
    private User admin;
    private Authentication adminPiiAuthentication;

    @BeforeEach
    void setUp() {
        hotelBookingService = new HotelBookingService(
                hotelBookingRepository,
                hotelRepository,
                roomTypeRepository,
                orderCenterService,
                piiAuditService,
                piiAccessGuard);

        user = new User();
        user.setId(1L);
        user.setUsername("traveler");
        user.setRole(User.Role.USER);

        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole(User.Role.ADMIN);
        adminPiiAuthentication = authentication(
                "admin", "ROLE_ADMIN", HotelBookingPiiAccessGuard.PII_READ_AUTHORITY);
    }

    @Test
    void userCanDeleteOwnCancelledHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CANCELLED);
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.of(booking));

        hotelBookingService.deleteBooking(user, 99L);

        assertNotNull(booking.getDeletedAt());
        verify(hotelBookingRepository).save(booking);
    }

    @Test
    void userCannotDeleteOwnActiveHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CONFIRMED);
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> hotelBookingService.deleteBooking(user, 99L));

        assertNull(booking.getDeletedAt());
        verify(hotelBookingRepository, never()).save(any());
    }

    @Test
    void userCannotDeleteSomeoneElsesHotelBooking() {
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> hotelBookingService.deleteBooking(user, 99L));

        verify(hotelBookingRepository, never()).findById(99L);
        verify(hotelBookingRepository, never()).save(any());
    }

    @Test
    void userCannotCancelAnonymizedHotelBooking() {
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> hotelBookingService.cancelBooking(user, 99L));

        verify(hotelBookingRepository, never()).findById(99L);
        verify(hotelBookingRepository, never()).save(any());
        verify(orderCenterService, never()).cancelLegacyMirror(any(), anyString(), any(), anyString());
    }

    @Test
    void userCannotDeleteAnonymizedHotelBooking() {
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> hotelBookingService.deleteBooking(user, 99L));

        verify(hotelBookingRepository, never()).findById(99L);
        verify(hotelBookingRepository, never()).save(any());
    }

    @Test
    void userCanCancelOwnHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CONFIRMED);
        when(hotelBookingRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.of(booking));
        when(hotelBookingRepository.save(booking)).thenReturn(booking);

        hotelBookingService.cancelBooking(user, 99L);

        assertEquals(HotelBooking.Status.CANCELLED, booking.getStatus());
        verify(hotelBookingRepository).save(booking);
        verify(orderCenterService).cancelLegacyMirror(eq(user), eq("LEGACY_HOTEL_BOOKING"), eq(99L), anyString());
    }

    @Test
    void createBookingMirrorsIntoUnifiedOrderCenter() {
        Hotel hotel = publicHotelEntity();
        RoomType roomType = roomType(hotel);
        HotelBookingRequest request = hotelBookingRequest(hotel.getId(), roomType.getId());
        when(hotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findByIdForUpdate(roomType.getId())).thenReturn(Optional.of(roomType));
        when(hotelBookingRepository.findOverlappingActiveBookingsForUpdate(
                eq(roomType.getId()), any(), eq(request.getCheckInDate()), eq(request.getCheckOutDate())))
                .thenReturn(List.of());
        when(hotelBookingRepository.save(any(HotelBooking.class))).thenAnswer(invocation -> {
            HotelBooking saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });

        HotelBooking saved = hotelBookingService.createBooking(user, request);

        assertEquals(77L, saved.getId());
        assertEquals(HotelBooking.Status.PENDING, saved.getStatus());
        verify(orderCenterService).createFromLegacyHotelBooking(saved);
    }

    @Test
    void stalePendingBookingExpiryContinuesPastFirstBatch() {
        List<HotelBooking> firstBatch = IntStream.range(0, 100)
                .mapToObj(index -> booking(1000L + index, user, HotelBooking.Status.PENDING))
                .toList();
        HotelBooking secondBatchBooking = booking(2000L, user, HotelBooking.Status.PENDING);
        PageRequest batchPage = PageRequest.of(0, 100);
        when(hotelBookingRepository.findStalePendingBookings(
                eq(HotelBooking.Status.PENDING), any(LocalDateTime.class), eq(batchPage)))
                .thenReturn(firstBatch, List.of(secondBatchBooking));

        hotelBookingService.expireStalePendingBookings();

        assertEquals(HotelBooking.Status.CANCELLED, firstBatch.get(0).getStatus());
        assertEquals(HotelBooking.Status.CANCELLED, secondBatchBooking.getStatus());
        verify(hotelBookingRepository, times(2)).findStalePendingBookings(
                eq(HotelBooking.Status.PENDING), any(LocalDateTime.class), eq(batchPage));
        verify(hotelBookingRepository, times(101)).save(any(HotelBooking.class));
        verify(orderCenterService, times(101)).cancelLegacyMirror(
                eq(null), eq("LEGACY_HOTEL_BOOKING"), any(), anyString());
    }

    @Test
    void adminStatusCancelMirrorsIntoUnifiedOrderCenter() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.CONFIRMED);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));
        when(hotelBookingRepository.save(booking)).thenReturn(booking);

        hotelBookingService.updateStatus(admin, 99L, "cancelled");

        assertEquals(HotelBooking.Status.CANCELLED, booking.getStatus());
        verify(orderCenterService).cancelLegacyMirror(
                eq(admin), eq("LEGACY_HOTEL_BOOKING"), eq(99L), anyString());
    }

    @Test
    void adminCanDeleteActiveHotelBooking() {
        HotelBooking booking = booking(99L, user, HotelBooking.Status.PENDING);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        hotelBookingService.deleteBooking(admin, 99L);

        assertEquals(HotelBooking.Status.CANCELLED, booking.getStatus());
        assertNotNull(booking.getDeletedAt());
        verify(hotelBookingRepository).save(booking);
        verify(orderCenterService).cancelLegacyMirror(eq(admin), eq("LEGACY_HOTEL_BOOKING"), eq(99L), anyString());
    }

    @Test
    void adminBookingListMasksGuestNameAndPhoneByDefault() {
        HotelBooking booking = bookingWithPii(99L, user);
        when(hotelBookingRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        Page<?> page = hotelBookingService.getAllBookings(admin, PageRequest.of(0, 20));
        var response = (com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse) page.getContent().get(0);

        assertNotEquals("Alice Zhang", response.guestName());
        assertEquals("A***g", response.guestName());
        assertEquals("138****8000", response.phone());
    }

    @Test
    void userBookingListKeepsOwnGuestNameButMasksPhone() throws Exception {
        HotelBooking booking = bookingWithPii(99L, user);
        when(hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        Page<?> page = hotelBookingService.getUserBookings(user, PageRequest.of(0, 20));
        var response = (com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse) page.getContent().get(0);

        assertEquals("Alice Zhang", response.guestName());
        assertEquals("138****8000", response.phone());
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(response);
        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("\"username\""));
        assertFalse(json.contains("traveler"));
    }

    @Test
    void revealBookingPiiReturnsFullGuestNameAndPhoneForAdmin() {
        HotelBooking booking = bookingWithPii(99L, user);
        when(piiAccessGuard.requireReveal(adminPiiAuthentication, 99L)).thenReturn(admin);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        var response = hotelBookingService.revealBookingPii(adminPiiAuthentication, 99L);

        assertEquals("Alice Zhang", response.guestName());
        assertEquals("13800138000", response.phone());
        verify(piiAuditService).recordRevealAllowed(admin, booking);
    }

    @Test
    void internalRevealWithoutPiiAuthorityDoesNotReturnFullPii() {
        Authentication adminWithoutPiiAuthority = authentication("admin", "ROLE_ADMIN");
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        HotelBookingPiiAccessGuard realGuard = new HotelBookingPiiAccessGuard(userRepository, piiAuditService);
        HotelBookingService guardedService = new HotelBookingService(
                hotelBookingRepository,
                hotelRepository,
                roomTypeRepository,
                orderCenterService,
                piiAuditService,
                realGuard);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        SecurityException error = assertThrows(
                SecurityException.class,
                () -> guardedService.revealBookingPii(adminWithoutPiiAuthority, 99L));

        assertEquals("Hotel booking PII read authority required", error.getMessage());
        verify(piiAuditService).recordRevealRejected(admin, 99L, "missing_authority");
        verify(hotelBookingRepository, never()).findById(99L);
    }

    @Test
    void adminRevealBookingPiiAuditsMissingBookingWithoutLeakingExistenceToNonAdmins() {
        when(piiAccessGuard.requireReveal(adminPiiAuthentication, 99L)).thenReturn(admin);
        when(hotelBookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> hotelBookingService.revealBookingPii(adminPiiAuthentication, 99L));

        verify(piiAuditService).recordRevealRejected(admin, 99L, "not_found");
    }

    @Test
    void publicHotelListMapsEntitiesToPublicDtos() {
        Hotel hotel = publicHotelEntity();
        when(hotelRepository.findAll(PageRequest.of(0, 200, Sort.by("id"))))
                .thenReturn(new PageImpl<>(List.of(hotel)));

        var response = hotelBookingService.getAllHotels().get(0);

        assertEquals(5L, response.id());
        assertEquals("Lhasa Hotel", response.name());
        assertEquals("Lhasa", response.location());
        assertEquals("800-1200", response.priceRange());
        assertEquals(new BigDecimal("4.8"), response.rating());
        assertEquals("/images/hotel.jpg", response.imageUrl());
        assertEquals("wifi, oxygen", response.facilities());
    }

    @Test
    void publicHotelDetailMapsEntityToPublicDto() {
        Hotel hotel = publicHotelEntity();
        when(hotelRepository.findById(5L)).thenReturn(Optional.of(hotel));

        var response = hotelBookingService.getHotel(5L).orElseThrow();

        assertEquals(5L, response.id());
        assertEquals("Lhasa Hotel", response.name());
        assertEquals("Lhasa", response.location());
    }

    @Test
    void publicRoomTypesMapEntitiesToPublicDtos() {
        RoomType roomType = new RoomType();
        roomType.setId(8L);
        roomType.setName("Deluxe King");
        roomType.setPrice(new BigDecimal("880.00"));
        roomType.setCapacity(2);
        roomType.setImageUrl("/images/room.jpg");
        roomType.setAmenities("oxygen, breakfast");
        roomType.setSortOrder(99);
        when(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(5L)).thenReturn(List.of(roomType));

        var response = hotelBookingService.getRoomTypes(5L).get(0);

        assertEquals(8L, response.id());
        assertEquals("Deluxe King", response.name());
        assertEquals(new BigDecimal("880.00"), response.price());
        assertEquals(2, response.capacity());
        assertEquals("/images/room.jpg", response.imageUrl());
        assertEquals("oxygen, breakfast", response.amenities());
    }

    private HotelBooking booking(Long id, User owner, HotelBooking.Status status) {
        HotelBooking booking = new HotelBooking();
        booking.setId(id);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }

    private HotelBooking bookingWithPii(Long id, User owner) {
        Hotel hotel = new Hotel();
        hotel.setId(5L);
        hotel.setName("Lhasa Hotel");

        HotelBooking booking = booking(id, owner, HotelBooking.Status.CONFIRMED);
        booking.setHotel(hotel);
        booking.setRoomName("Deluxe");
        booking.setRoomTypeId(6L);
        booking.setRoomPrice(new BigDecimal("880"));
        booking.setNights(2);
        booking.setCheckInDate(LocalDate.parse("2026-08-01"));
        booking.setCheckOutDate(LocalDate.parse("2026-08-03"));
        booking.setGuests(2);
        booking.setGuestName("Alice Zhang");
        booking.setPhone("13800138000");
        booking.setTotalPrice(new BigDecimal("1848"));
        booking.setCreatedAt(LocalDateTime.parse("2026-06-01T12:00:00"));
        return booking;
    }

    private Hotel publicHotelEntity() {
        Hotel hotel = new Hotel();
        hotel.setId(5L);
        hotel.setName("Lhasa Hotel");
        hotel.setLocation("Lhasa");
        hotel.setPhone("13900139000");
        hotel.setPriceRange("800-1200");
        hotel.setRating(new BigDecimal("4.8"));
        hotel.setImageUrl("/images/hotel.jpg");
        hotel.setFacilities("wifi, oxygen");
        hotel.setCreatedAt(LocalDateTime.parse("2026-06-01T12:00:00"));
        return hotel;
    }

    private RoomType roomType(Hotel hotel) {
        RoomType roomType = new RoomType();
        roomType.setId(6L);
        roomType.setHotel(hotel);
        roomType.setName("Deluxe King");
        roomType.setPrice(new BigDecimal("880"));
        roomType.setCapacity(2);
        return roomType;
    }

    private HotelBookingRequest hotelBookingRequest(Long hotelId, Long roomId) {
        HotelBookingRequest request = new HotelBookingRequest();
        request.setHotelId(hotelId);
        request.setRoomId(roomId);
        request.setGuests(2);
        request.setGuestName("Alice Zhang");
        request.setPhone("13800138000");
        request.setCheckInDate(LocalDate.now().plusDays(10));
        request.setCheckOutDate(LocalDate.now().plusDays(12));
        return request;
    }

    private Authentication authentication(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "password", grantedAuthorities);
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
    }
}
