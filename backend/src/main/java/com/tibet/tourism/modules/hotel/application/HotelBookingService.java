package com.tibet.tourism.modules.hotel.application;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingRequest;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingResponse;
import com.tibet.tourism.modules.hotel.web.dto.PublicHotelResponse;
import com.tibet.tourism.modules.hotel.web.dto.PublicRoomTypeResponse;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HotelBookingService {

    private static final int MAX_NIGHTS = 30;
    private static final int PENDING_HOLD_MINUTES = 15;
    private static final Set<HotelBooking.Status> ACTIVE_BOOKING_STATUSES =
            Set.of(HotelBooking.Status.PENDING, HotelBooking.Status.CONFIRMED);
    private enum PiiView {
        OWNER,
        MASKED,
        FULL
    }

    private final HotelBookingRepository hotelBookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final OrderCenterService orderCenterService;

    public HotelBookingService(HotelBookingRepository hotelBookingRepository,
                               HotelRepository hotelRepository,
                               RoomTypeRepository roomTypeRepository,
                               OrderCenterService orderCenterService) {
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.orderCenterService = orderCenterService;
    }

    @Transactional(readOnly = true)
    public List<PublicRoomTypeResponse> getRoomTypes(Long hotelId) {
        return roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId).stream()
                .map(PublicRoomTypeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicHotelResponse> getAllHotels() {
        return hotelRepository.findAll(PageRequest.of(0, 200, Sort.by("id"))).getContent().stream()
                .map(PublicHotelResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PublicHotelResponse> getHotel(Long id) {
        return hotelRepository.findById(id)
                .map(PublicHotelResponse::from);
    }

    @Transactional
    public HotelBooking createBooking(User user, HotelBookingRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new NoSuchElementException("Hotel not found"));
        RoomType roomType = roomTypeRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("Room type not found"));

        validateBookingRequest(request, roomType);
        ensureRoomAvailable(roomType, request.getCheckInDate(), request.getCheckOutDate());

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal roomPrice = roomType.getPrice() == null ? BigDecimal.ZERO : roomType.getPrice();
        BigDecimal subtotal = roomPrice.multiply(BigDecimal.valueOf(nights));
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal totalPrice = subtotal.subtract(discount);

        HotelBooking booking = new HotelBooking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRoomName(roomType.getName());
        booking.setRoomTypeId(roomType.getId());
        booking.setRoomPrice(roomPrice);
        booking.setNights((int) nights);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuests(request.getGuests());
        booking.setGuestName(InputSanitizer.requiredPlainText(request.getGuestName(), 64, "入住人姓名"));
        booking.setPhone(InputSanitizer.requiredPlainText(request.getPhone(), 32, "手机号"));
        booking.setNote(InputSanitizer.optionalTextBlock(request.getNote(), 500, "备注"));
        booking.setSubtotal(subtotal);
        booking.setServiceFee(serviceFee);
        booking.setDiscount(discount);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(HotelBooking.Status.PENDING);

        HotelBooking saved = hotelBookingRepository.save(booking);
        orderCenterService.createFromLegacyHotelBooking(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<HotelBookingResponse> getUserBookings(User user, Pageable pageable) {
        return hotelBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(booking -> toResponse(booking, PiiView.OWNER));
    }

    @Transactional(readOnly = true)
    public Page<HotelBookingResponse> getAllBookings(User user, Pageable pageable) {
        requireAdmin(user);
        return hotelBookingRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(booking -> toResponse(booking, PiiView.MASKED));
    }

    @Transactional(readOnly = true)
    public HotelBookingResponse revealBookingPii(User user, Long id) {
        requireAdmin(user);
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));
        return toResponse(booking, PiiView.FULL);
    }

    @Transactional
    public HotelBooking updateStatus(User user, Long id, String status) {
        requireAdmin(user);
        HotelBooking booking = hotelBookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Invalid status");
        }
        transitionStatus(booking, HotelBooking.Status.valueOf(status.trim().toUpperCase()));
        HotelBooking saved = hotelBookingRepository.save(booking);
        if (saved.getStatus() == HotelBooking.Status.CONFIRMED) {
            orderCenterService.createFromLegacyHotelBooking(saved);
        }
        return saved;
    }

    @Transactional
    public HotelBooking cancelBooking(User user, Long id) {
        HotelBooking booking = findBookingForMutation(user, id);
        if (!isAdmin(user) && !isOwner(booking, user)) {
            throw new NoSuchElementException("Booking not found");
        }

        transitionStatus(booking, HotelBooking.Status.CANCELLED);
        HotelBooking saved = hotelBookingRepository.save(booking);
        orderCenterService.cancelLegacyMirror(user, "LEGACY_HOTEL_BOOKING", saved.getId(), "旧酒店预订取消");
        return saved;
    }

    @Transactional
    public void deleteBooking(User user, Long id) {
        HotelBooking booking = findBookingForMutation(user, id);
        if (booking.getDeletedAt() != null) {
            throw new NoSuchElementException("Booking not found");
        }
        boolean isAdmin = isAdmin(user);
        if (!isAdmin && !isOwner(booking, user)) {
            throw new NoSuchElementException("Booking not found");
        }
        if (!isAdmin && booking.getStatus() != HotelBooking.Status.CANCELLED) {
            throw new IllegalStateException("仅已取消酒店预订可以删除");
        }
        if (isAdmin && booking.getStatus() != HotelBooking.Status.CANCELLED) {
            transitionStatus(booking, HotelBooking.Status.CANCELLED);
        }
        booking.setDeletedAt(LocalDateTime.now());
        hotelBookingRepository.save(booking);
    }

    private HotelBooking findBookingForMutation(User user, Long id) {
        if (isAdmin(user)) {
            return hotelBookingRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Booking not found"));
        }
        return hotelBookingRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));
    }

    private boolean isOwner(HotelBooking booking, User user) {
        return booking.getUser() != null
                && user.getId() != null
                && user.getId().equals(booking.getUser().getId());
    }

    private boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    private void validateBookingRequest(HotelBookingRequest request, RoomType roomType) {
        if (roomType.getHotel() == null || !request.getHotelId().equals(roomType.getHotel().getId())) {
            throw new IllegalArgumentException("Room type does not belong to this hotel");
        }
        if (!StringUtils.hasText(request.getGuestName()) || !StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("Guest name and phone are required");
        }

        int capacity = Math.max(1, roomType.getCapacity() == null ? 1 : roomType.getCapacity());
        if (request.getGuests() == null || request.getGuests() < 1 || request.getGuests() > capacity) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (request.getCheckInDate().isBefore(LocalDate.now()) || nights < 1 || nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("Invalid check-in or check-out date");
        }
    }

    private void ensureRoomAvailable(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        expireStalePendingBookings();
        List<HotelBooking> overlapping = hotelBookingRepository.findOverlappingActiveBookingsForUpdate(
                roomType.getId(), ACTIVE_BOOKING_STATUSES, checkIn, checkOut);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Room type is unavailable for the selected dates");
        }
        Long hotelId = roomType.getHotel() == null ? null : roomType.getHotel().getId();
        orderCenterService.ensureHotelRoomAvailable(hotelId, roomType.getId(), checkIn, checkOut);
    }

    @Scheduled(fixedDelayString = "${app.hotel-bookings.expiry-sweep-delay-ms:60000}")
    @Transactional
    public void expireStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_HOLD_MINUTES);
        hotelBookingRepository.findByStatusAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                        HotelBooking.Status.PENDING, cutoff)
                .forEach(booking -> {
                    transitionStatus(booking, HotelBooking.Status.CANCELLED);
                    hotelBookingRepository.save(booking);
                    orderCenterService.cancelLegacyMirror(null, "LEGACY_HOTEL_BOOKING",
                            booking.getId(), "Legacy hotel booking hold expired");
                });
    }

    private void transitionStatus(HotelBooking booking, HotelBooking.Status nextStatus) {
        HotelBooking.Status currentStatus = booking.getStatus() == null ? HotelBooking.Status.PENDING : booking.getStatus();
        if (currentStatus == nextStatus) {
            return;
        }
        boolean allowed = switch (currentStatus) {
            case PENDING -> nextStatus == HotelBooking.Status.CONFIRMED || nextStatus == HotelBooking.Status.CANCELLED;
            case CONFIRMED -> nextStatus == HotelBooking.Status.CANCELLED;
            case CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Illegal booking status transition");
        }
        booking.setStatus(nextStatus);
    }

    private void requireAdmin(User user) {
        if (user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Admin access required");
        }
    }

    private HotelBookingResponse toResponse(HotelBooking booking, PiiView piiView) {
        Hotel hotel = booking.getHotel();
        boolean fullPii = piiView == PiiView.FULL;
        boolean ownerView = piiView == PiiView.OWNER;
        return new HotelBookingResponse(
                booking.getId(),
                hotel == null ? null : new HotelBookingResponse.HotelSummary(
                        hotel.getId(),
                        hotel.getName(),
                        hotel.getLocation(),
                        hotel.getImageUrl()),
                hotel == null ? null : hotel.getId(),
                booking.getRoomName(),
                booking.getRoomTypeId(),
                booking.getRoomPrice(),
                booking.getNights(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuests(),
                fullPii || ownerView ? booking.getGuestName() : PiiMasker.maskName(booking.getGuestName()),
                fullPii ? booking.getPhone() : PiiMasker.maskPhone(booking.getPhone()),
                noteFor(booking, piiView),
                booking.getSubtotal(),
                booking.getServiceFee(),
                booking.getDiscount(),
                booking.getTotalPrice(),
                booking.getStatus() == null ? null : booking.getStatus().name(),
                booking.getCreatedAt()
        );
    }

    private String noteFor(HotelBooking booking, PiiView piiView) {
        if (piiView == PiiView.MASKED) {
            return null;
        }
        return booking.getNote();
    }
}
