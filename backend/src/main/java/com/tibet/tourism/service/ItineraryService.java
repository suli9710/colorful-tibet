package com.tibet.tourism.service;

import com.tibet.tourism.dto.HotelBookingRequest;
import com.tibet.tourism.dto.itinerary.*;
import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import com.tibet.tourism.security.InputSanitizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ItineraryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Pattern ALTITUDE_PATTERN = Pattern.compile("(\\d{3,5})");
    private static final int ITINERARY_SPOT_LIMIT = 500;
    private static final int ITINERARY_HOTEL_LIMIT = 200;

    private final ItineraryRepository itineraryRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ScenicSpotRepository scenicSpotRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;
    private final HotelBookingService hotelBookingService;
    private final OrderCenterService orderCenterService;

    public ItineraryService(ItineraryRepository itineraryRepository,
                            ItineraryItemRepository itineraryItemRepository,
                            ScenicSpotRepository scenicSpotRepository,
                            HotelRepository hotelRepository,
                            RoomTypeRepository roomTypeRepository,
                            BookingRepository bookingRepository,
                            HotelBookingService hotelBookingService,
                            OrderCenterService orderCenterService) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.scenicSpotRepository = scenicSpotRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
        this.hotelBookingService = hotelBookingService;
        this.orderCenterService = orderCenterService;
    }

    @Transactional
    public ItineraryResponse generateItinerary(User user, GenerateItineraryRequest request) {
        String versionType = normalizeVersionType(request.getVersionType());
        Itinerary itinerary = buildItinerary(
                user,
                null,
                safeDays(request.getDays()),
                request.getStartDate() == null ? LocalDate.now().plusDays(1) : request.getStartDate(),
                normalizeBudget(request.getBudget(), versionType),
                normalizePreference(request.getPreference()),
                versionType,
                Math.max(1, request.getTravelers() == null ? 2 : request.getTravelers())
        );

        Itinerary saved = itineraryRepository.save(itinerary);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponse> getMyItineraries(User user) {
        return itineraryRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(User user, Long itineraryId) {
        return toResponse(loadItinerary(user, itineraryId));
    }

    @Transactional(readOnly = true)
    public ItineraryQuoteResponse quoteItinerary(User user, Long itineraryId) {
        Itinerary itinerary = loadItinerary(user, itineraryId);
        List<ItineraryQuoteItemResponse> items = itinerary.getItineraryDays().stream()
                .flatMap(day -> day.getItems().stream()
                        .map(item -> toQuoteItem(day, item)))
                .toList();

        BigDecimal bookableTotal = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.bookable()))
                .map(ItineraryQuoteItemResponse::estimatedCost)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal total = items.stream()
                .map(ItineraryQuoteItemResponse::estimatedCost)
                .reduce(ZERO, BigDecimal::add);

        return new ItineraryQuoteResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                money(total),
                money(bookableTotal),
                money(total.subtract(bookableTotal)),
                "CNY",
                items
        );
    }

    @Transactional
    public BookItineraryItemResponse bookItem(User user, Long itineraryId, Long itemId, BookItineraryItemRequest request) {
        ItineraryItem item = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("行程节点不存在"));
        Itinerary itinerary = item.getDay().getItinerary();
        if (!itinerary.getId().equals(itineraryId) || !itinerary.getUser().getId().equals(user.getId())) {
            throw new SecurityException("无权操作该行程节点");
        }
        if (item.getBookingStatus() == ItineraryItem.BookingStatus.BOOKED && item.getBookingReferenceId() != null) {
            return new BookItineraryItemResponse(item.getId(), item.getBookingReferenceType(),
                    item.getBookingReferenceId(), money(item.getEstimatedCost()), "该节点已预订");
        }

        int travelers = Math.max(1, request.getTravelers() == null ? 1 : request.getTravelers());
        if (item.getItemType() == ItineraryItem.ItemType.SCENIC_SPOT) {
            return bookScenicSpotItem(user, item, travelers);
        }
        if (item.getItemType() == ItineraryItem.ItemType.HOTEL) {
            return bookHotelItem(user, item, travelers, request);
        }
        throw new IllegalArgumentException("该行程节点暂不支持直接预订");
    }

    @Transactional
    public ItineraryResponse createVersion(User user, Long itineraryId, CreateItineraryVersionRequest request) {
        Itinerary parent = loadItinerary(user, itineraryId);
        String versionType = normalizeVersionType(request.getVersionType());
        Itinerary version = buildItinerary(
                user,
                parent,
                parent.getDays(),
                parent.getStartDate(),
                normalizeBudget(parent.getBudget(), versionType),
                parent.getPreference(),
                versionType,
                2
        );
        return toResponse(itineraryRepository.save(version));
    }

    private Itinerary buildItinerary(User user,
                                     Itinerary parent,
                                     int days,
                                     LocalDate startDate,
                                     String budget,
                                     String preference,
                                     String versionType,
                                     int travelers) {
        List<ScenicSpot> spots = prioritizeSpots(
                scenicSpotRepository.findAllWithoutTags(PageRequest.of(0, ITINERARY_SPOT_LIMIT,
                        Sort.by(Sort.Direction.DESC, "visitCount").and(Sort.by("id")))).getContent(),
                preference,
                versionType);
        List<Hotel> hotels = hotelRepository.findAll(PageRequest.of(0, ITINERARY_HOTEL_LIMIT, Sort.by("id"))).getContent();

        Itinerary itinerary = new Itinerary();
        itinerary.setUser(user);
        itinerary.setParentItinerary(parent);
        itinerary.setDays(days);
        itinerary.setStartDate(startDate);
        itinerary.setBudget(budget);
        itinerary.setPreference(preference);
        itinerary.setVersionType(versionType);
        itinerary.setVersionLabel(versionLabel(versionType));
        itinerary.setTitle("西藏" + days + "天" + versionLabel(versionType) + "可预订行程");

        for (int dayNumber = 1; dayNumber <= days; dayNumber++) {
            ScenicSpot spot = pickSpot(spots, dayNumber, versionType);
            ItineraryDay day = buildDay(dayNumber, startDate.plusDays(dayNumber - 1L), spot, budget, versionType);
            addTransportItem(day, dayNumber, budget, versionType);
            addSpotItem(day, spot, dayNumber, travelers, spots, versionType);
            addMealItem(day, travelers, budget);
            addHotelItem(day, hotels, spot, travelers, dayNumber, budget, versionType);
            day.setEstimatedCost(sumDay(day));
            itinerary.addDay(day);
        }

        itinerary.setTotalEstimatedCost(sumItinerary(itinerary));
        itinerary.setSourceContent(buildSourceContent(itinerary));
        return itinerary;
    }

    private ItineraryDay buildDay(int dayNumber, LocalDate date, ScenicSpot spot, String budget, String versionType) {
        String region = spot == null ? "拉萨" : fallback(spot.getLocation(), "西藏");
        String spotName = spot == null ? "高原适应" : spot.getName();
        String risk = altitudeRisk(parseAltitude(spot));

        ItineraryDay day = new ItineraryDay();
        day.setDayNumber(dayNumber);
        day.setTravelDate(date);
        day.setRegion(region);
        day.setTitle("第" + dayNumber + "天：" + spotName + " · " + region);
        day.setAltitudeRisk(risk);
        day.setSummary(daySummary(spotName, risk, budget, versionType));
        return day;
    }

    private void addTransportItem(ItineraryDay day, int dayNumber, String budget, String versionType) {
        BigDecimal cost = switch (budget) {
            case "luxury" -> BigDecimal.valueOf(650);
            case "economy" -> BigDecimal.valueOf(180);
            default -> BigDecimal.valueOf(360);
        };
        if ("relaxed".equals(versionType)) {
            cost = cost.add(BigDecimal.valueOf(80));
        }

        ItineraryItem item = baseItem(ItineraryItem.ItemType.TRANSPORT, "09:00", "当日交通", 120, 1);
        item.setDescription(dayNumber == 1 ? "抵达后以低强度市区交通为主，优先留出高原适应时间。" : "根据当日路线安排包车/拼车接驳，行程中预留休息点。");
        item.setEstimatedCost(money(cost));
        item.setBookingAction(ItineraryItem.BookingAction.ADD_TO_TRIP);
        item.setBookingStatus(ItineraryItem.BookingStatus.NOT_BOOKABLE);
        day.addItem(item);
    }

    private void addSpotItem(ItineraryDay day,
                             ScenicSpot spot,
                             int dayNumber,
                             int travelers,
                             List<ScenicSpot> alternatives,
                             String versionType) {
        if (spot == null) {
            ItineraryItem note = baseItem(ItineraryItem.ItemType.NOTE, "10:30", "高原适应与自由活动", 180, 2);
            note.setDescription("当天以适应海拔和轻量城市漫步为主，不安排强制景点。");
            note.setEstimatedCost(ZERO);
            day.addItem(note);
            return;
        }

        int altitude = parseAltitude(spot);
        BigDecimal price = estimateSpotPrice(spot, day.getTravelDate()).multiply(BigDecimal.valueOf(travelers));
        ItineraryItem item = baseItem(ItineraryItem.ItemType.SCENIC_SPOT, "10:30", spot.getName(), 180, 2);
        item.setDescription(buildSpotDescription(spot, versionType));
        item.setEstimatedCost(money(price));
        item.setAltitudeMeters(altitude == 0 ? null : altitude);
        item.setRiskLevel(altitudeRisk(altitude));
        item.setAlternatives(alternativeNames(alternatives, spot.getId()));
        item.setBookingAction(ItineraryItem.BookingAction.BOOK_SPOT);
        item.setBookingStatus(ItineraryItem.BookingStatus.BOOKABLE);
        item.setScenicSpot(spot);
        day.addItem(item);
    }

    private void addMealItem(ItineraryDay day, int travelers, String budget) {
        BigDecimal perPerson = switch (budget) {
            case "luxury" -> BigDecimal.valueOf(180);
            case "economy" -> BigDecimal.valueOf(55);
            default -> BigDecimal.valueOf(95);
        };

        ItineraryItem item = baseItem(ItineraryItem.ItemType.MEAL, "18:30", "本地餐饮", 90, 3);
        item.setDescription("优先选择当地藏餐/石锅鸡/甜茶馆，兼顾清淡饮食，避免刚到高原时饮酒。");
        item.setEstimatedCost(money(perPerson.multiply(BigDecimal.valueOf(travelers))));
        item.setBookingAction(ItineraryItem.BookingAction.ADD_TO_TRIP);
        item.setBookingStatus(ItineraryItem.BookingStatus.NOT_BOOKABLE);
        day.addItem(item);
    }

    private void addHotelItem(ItineraryDay day,
                              List<Hotel> hotels,
                              ScenicSpot spot,
                              int travelers,
                              int dayNumber,
                              String budget,
                              String versionType) {
        Hotel hotel = pickHotel(hotels, spot, dayNumber);
        RoomType roomType = pickRoomType(hotel, travelers, budget);
        ItineraryItem item = baseItem(ItineraryItem.ItemType.HOTEL, "20:30", hotel == null ? "住宿待确认" : hotel.getName(), 720, 4);
        item.setDescription(hotel == null ? "暂未匹配到可订酒店，可先保存行程后人工补充。" : "建议入住 " + fallback(hotel.getLocation(), day.getRegion()) + "，优先选择含氧/供暖/可停车的房型。");
        item.setEstimatedCost(money(roomType == null || roomType.getPrice() == null ? hotelFallbackPrice(budget) : roomType.getPrice()));
        item.setBookingAction(hotel == null || roomType == null ? ItineraryItem.BookingAction.ADD_TO_TRIP : ItineraryItem.BookingAction.BOOK_HOTEL);
        item.setBookingStatus(hotel == null || roomType == null ? ItineraryItem.BookingStatus.NOT_BOOKABLE : ItineraryItem.BookingStatus.BOOKABLE);
        item.setHotel(hotel);
        item.setRoomType(roomType);
        day.addItem(item);
    }

    private ItineraryItem baseItem(ItineraryItem.ItemType type, String startTime, String title, int durationMinutes, int sortOrder) {
        ItineraryItem item = new ItineraryItem();
        item.setItemType(type);
        item.setStartTime(startTime);
        item.setTitle(title);
        item.setDurationMinutes(durationMinutes);
        item.setSortOrder(sortOrder);
        return item;
    }

    private BookItineraryItemResponse bookScenicSpotItem(User user, ItineraryItem item, int travelers) {
        ScenicSpot spot = item.getScenicSpot();
        if (spot == null) {
            throw new IllegalArgumentException("该景点节点缺少可预订景点");
        }

        BigDecimal unitPrice = estimateSpotPrice(spot, item.getDay().getTravelDate());
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSpot(spot);
        booking.setVisitDate(item.getDay().getTravelDate());
        booking.setTicketCount(travelers);
        booking.setStatus(Booking.Status.PENDING);
        booking.setTotalPrice(money(unitPrice.multiply(BigDecimal.valueOf(travelers))));
        bookingRepository.save(booking);
        orderCenterService.createFromLegacySpotBooking(booking);

        item.setBookingStatus(ItineraryItem.BookingStatus.BOOKED);
        item.setBookingReferenceType("SPOT_BOOKING");
        item.setBookingReferenceId(booking.getId());
        itineraryItemRepository.save(item);
        return new BookItineraryItemResponse(item.getId(), "SPOT_BOOKING", booking.getId(), booking.getTotalPrice(), "景点门票预订已创建，等待支付确认");
    }

    private BookItineraryItemResponse bookHotelItem(User user, ItineraryItem item, int travelers, BookItineraryItemRequest request) {
        Hotel hotel = item.getHotel();
        RoomType roomType = item.getRoomType();
        if (hotel == null || roomType == null) {
            throw new IllegalArgumentException("该酒店节点缺少可预订房型");
        }

        String guestName = StringUtils.hasText(request.getGuestName()) ? request.getGuestName() : fallback(user.getNickname(), user.getUsername());
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone() : user.getPhone();
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("酒店预订需要手机号");
        }

        HotelBookingRequest bookingRequest = new HotelBookingRequest();
        bookingRequest.setHotelId(hotel.getId());
        bookingRequest.setRoomId(roomType.getId());
        bookingRequest.setGuests(travelers);
        bookingRequest.setGuestName(InputSanitizer.requiredPlainText(guestName, 64, "入住人姓名"));
        bookingRequest.setPhone(InputSanitizer.requiredPlainText(phone, 32, "手机号"));
        bookingRequest.setNote(InputSanitizer.optionalTextBlock(request.getNote(), 500, "备注"));
        bookingRequest.setCheckInDate(item.getDay().getTravelDate());
        bookingRequest.setCheckOutDate(item.getDay().getTravelDate().plusDays(1));

        HotelBooking saved = hotelBookingService.createBooking(user, bookingRequest);
        item.setBookingStatus(ItineraryItem.BookingStatus.BOOKED);
        item.setBookingReferenceType("HOTEL_BOOKING");
        item.setBookingReferenceId(saved.getId());
        itineraryItemRepository.save(item);
        return new BookItineraryItemResponse(item.getId(), "HOTEL_BOOKING", saved.getId(), saved.getTotalPrice(), "酒店预订成功");
    }

    private Itinerary loadItinerary(User user, Long itineraryId) {
        return itineraryRepository.findByIdAndUserId(itineraryId, user.getId())
                .orElseThrow(() -> new NoSuchElementException("行程不存在"));
    }

    private List<ScenicSpot> prioritizeSpots(List<ScenicSpot> spots, String preference, String versionType) {
        Comparator<ScenicSpot> visitComparator = Comparator.comparingInt(spot -> spot.getVisitCount() == null ? 0 : spot.getVisitCount());
        if (!"hidden".equals(versionType)) {
            visitComparator = visitComparator.reversed();
        }
        Comparator<ScenicSpot> comparator = Comparator
                .comparingInt((ScenicSpot spot) -> preferenceScore(spot, preference, versionType)).reversed()
                .thenComparing(visitComparator)
                .thenComparing(ScenicSpot::getId);
        return spots.stream()
                .filter(Objects::nonNull)
                .filter(spot -> !"family".equals(versionType) || parseAltitude(spot) < 4300)
                .filter(spot -> !"relaxed".equals(versionType) || parseAltitude(spot) < 4700)
                .sorted(comparator)
                .toList();
    }

    private int preferenceScore(ScenicSpot spot, String preference, String versionType) {
        if ("hidden".equals(versionType)) {
            return 1;
        }
        if (spot.getCategory() == null) {
            return 0;
        }
        return switch (preference) {
            case "cultural" -> Set.of(ScenicSpot.Category.CULTURAL, ScenicSpot.Category.HISTORICAL, ScenicSpot.Category.RELIGIOUS).contains(spot.getCategory()) ? 3 : 1;
            case "photography", "natural" -> spot.getCategory() == ScenicSpot.Category.NATURAL ? 3 : 1;
            case "relaxation" -> parseAltitude(spot) < 4200 ? 2 : 1;
            default -> 1;
        };
    }

    private ScenicSpot pickSpot(List<ScenicSpot> spots, int dayNumber, String versionType) {
        if (spots.isEmpty()) {
            return null;
        }
        if (dayNumber == 1) {
            return spots.stream()
                    .filter(spot -> containsAny(spot.getName(), "布达拉", "大昭寺", "八廓", "拉萨"))
                    .findFirst()
                    .orElse(spots.get(0));
        }
        int stride = "hidden".equals(versionType) ? 2 : 1;
        int index = Math.floorMod((dayNumber - 1) * stride, spots.size());
        return spots.get(index);
    }

    private Hotel pickHotel(List<Hotel> hotels, ScenicSpot spot, int dayNumber) {
        if (hotels.isEmpty()) {
            return null;
        }
        String location = spot == null ? "" : fallback(spot.getLocation(), spot.getName());
        return hotels.stream()
                .filter(hotel -> StringUtils.hasText(hotel.getLocation()) && containsAny(location, hotel.getLocation()))
                .findFirst()
                .orElse(hotels.get(Math.floorMod(dayNumber - 1, hotels.size())));
    }

    private RoomType pickRoomType(Hotel hotel, int travelers, String budget) {
        if (hotel == null || hotel.getId() == null) {
            return null;
        }
        List<RoomType> roomTypes = roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotel.getId());
        Comparator<RoomType> priceOrder = Comparator.comparing(room -> room.getPrice() == null ? BigDecimal.ZERO : room.getPrice());
        if ("luxury".equals(budget)) {
            priceOrder = priceOrder.reversed();
        }
        return roomTypes.stream()
                .filter(room -> room.getCapacity() == null || room.getCapacity() >= travelers)
                .sorted(priceOrder)
                .findFirst()
                .orElse(roomTypes.stream().sorted(priceOrder).findFirst().orElse(null));
    }

    private ItineraryQuoteItemResponse toQuoteItem(ItineraryDay day, ItineraryItem item) {
        boolean bookable = item.getBookingAction() == ItineraryItem.BookingAction.BOOK_SPOT
                || item.getBookingAction() == ItineraryItem.BookingAction.BOOK_HOTEL;
        return new ItineraryQuoteItemResponse(
                item.getId(),
                day.getDayNumber(),
                day.getTravelDate(),
                item.getItemType().name(),
                item.getTitle(),
                money(item.getEstimatedCost()),
                bookable,
                item.getBookingAction() == null ? ItineraryItem.BookingAction.NONE.name() : item.getBookingAction().name(),
                item.getBookingStatus() == null ? ItineraryItem.BookingStatus.NOT_BOOKABLE.name() : item.getBookingStatus().name(),
                bookable ? "实时/缓存价" : "预估价"
        );
    }

    private ItineraryResponse toResponse(Itinerary itinerary) {
        Long parentId = itinerary.getParentItinerary() == null ? null : itinerary.getParentItinerary().getId();
        return new ItineraryResponse(
                itinerary.getId(),
                parentId,
                itinerary.getTitle(),
                itinerary.getDays(),
                itinerary.getStartDate(),
                itinerary.getBudget(),
                itinerary.getPreference(),
                itinerary.getVersionType(),
                itinerary.getVersionLabel(),
                money(itinerary.getTotalEstimatedCost()),
                itinerary.getStatus().name(),
                itinerary.getSourceContent(),
                itinerary.getCreatedAt(),
                itinerary.getItineraryDays().stream().map(this::toDayResponse).toList()
        );
    }

    private ItineraryDayResponse toDayResponse(ItineraryDay day) {
        return new ItineraryDayResponse(
                day.getId(),
                day.getDayNumber(),
                day.getTravelDate(),
                day.getTitle(),
                day.getRegion(),
                day.getSummary(),
                money(day.getEstimatedCost()),
                day.getAltitudeRisk(),
                day.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private ItineraryItemResponse toItemResponse(ItineraryItem item) {
        ScenicSpot spot = item.getScenicSpot();
        Hotel hotel = item.getHotel();
        RoomType roomType = item.getRoomType();
        return new ItineraryItemResponse(
                item.getId(),
                item.getItemType().name(),
                item.getTitle(),
                item.getDescription(),
                item.getStartTime(),
                item.getDurationMinutes(),
                money(item.getEstimatedCost()),
                item.getAltitudeMeters(),
                item.getRiskLevel(),
                item.getAlternatives(),
                item.getBookingAction() == null ? ItineraryItem.BookingAction.NONE.name() : item.getBookingAction().name(),
                item.getBookingStatus() == null ? ItineraryItem.BookingStatus.NOT_BOOKABLE.name() : item.getBookingStatus().name(),
                item.getBookingReferenceType(),
                item.getBookingReferenceId(),
                spot == null ? null : spot.getId(),
                spot == null ? null : spot.getName(),
                hotel == null ? null : hotel.getId(),
                hotel == null ? null : hotel.getName(),
                roomType == null ? null : roomType.getId(),
                roomType == null ? null : roomType.getName(),
                item.getSortOrder()
        );
    }

    private BigDecimal estimateSpotPrice(ScenicSpot spot, LocalDate travelDate) {
        if (spot == null) {
            return ZERO;
        }
        BigDecimal unitPrice = spot.getTicketPrice() == null ? BigDecimal.ZERO : spot.getTicketPrice();
        int month = travelDate.getMonthValue();
        if (month >= 1 && month <= 3) {
            return ZERO;
        }

        MonthDay travelMonthDay = MonthDay.from(travelDate);
        MonthDay may1 = MonthDay.of(5, 1);
        MonthDay oct31 = MonthDay.of(10, 31);
        boolean peakSeason = !travelMonthDay.isBefore(may1) && !travelMonthDay.isAfter(oct31);
        if (peakSeason && spot.getPeakSeasonPrice() != null) {
            return money(spot.getPeakSeasonPrice());
        }
        if (!peakSeason && spot.getOffSeasonPrice() != null) {
            return money(spot.getOffSeasonPrice());
        }
        return money(unitPrice);
    }

    private int parseAltitude(ScenicSpot spot) {
        if (spot == null || !StringUtils.hasText(spot.getAltitude())) {
            return 0;
        }
        Matcher matcher = ALTITUDE_PATTERN.matcher(spot.getAltitude());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String altitudeRisk(int altitude) {
        if (altitude >= 4700) return "HIGH";
        if (altitude >= 3900) return "MEDIUM";
        return "LOW";
    }

    private BigDecimal hotelFallbackPrice(String budget) {
        return switch (budget) {
            case "luxury" -> BigDecimal.valueOf(980);
            case "economy" -> BigDecimal.valueOf(260);
            default -> BigDecimal.valueOf(520);
        };
    }

    private BigDecimal sumDay(ItineraryDay day) {
        return money(day.getItems().stream()
                .map(ItineraryItem::getEstimatedCost)
                .reduce(ZERO, BigDecimal::add));
    }

    private BigDecimal sumItinerary(Itinerary itinerary) {
        return money(itinerary.getItineraryDays().stream()
                .map(ItineraryDay::getEstimatedCost)
                .reduce(ZERO, BigDecimal::add));
    }

    private String buildSourceContent(Itinerary itinerary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(itinerary.getTitle()).append("\n\n");
        builder.append("预算：").append(itinerary.getBudget()).append("；偏好：").append(itinerary.getPreference())
                .append("；版本：").append(itinerary.getVersionLabel()).append("\n\n");
        for (ItineraryDay day : itinerary.getItineraryDays()) {
            builder.append("## 第").append(day.getDayNumber()).append("天：").append(day.getTitle()).append("\n");
            builder.append(day.getSummary()).append("\n");
            for (ItineraryItem item : day.getItems()) {
                builder.append("- ").append(item.getStartTime()).append(" ")
                        .append(item.getTitle()).append("（约 ¥").append(money(item.getEstimatedCost())).append("）\n");
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    private String buildSpotDescription(ScenicSpot spot, String versionType) {
        String base = StringUtils.hasText(spot.getDescription())
                ? truncateText(spot.getDescription(), 180)
                : "结合开放时间、门票和海拔信息安排游览。";
        if ("family".equals(versionType)) {
            return base + " 建议缩短单次步行，预留休息和补水时间。";
        }
        if ("relaxed".equals(versionType)) {
            return base + " 该版本降低赶路强度，优先保证高原适应。";
        }
        return base;
    }

    private String daySummary(String spotName, String risk, String budget, String versionType) {
        String intensity = "relaxed".equals(versionType) || "family".equals(versionType) ? "轻松节奏" : "标准节奏";
        return "围绕 " + spotName + " 安排 " + intensity + "，预算档位为 " + budget + "，当日海拔风险：" + risk + "。";
    }

    private String alternativeNames(List<ScenicSpot> spots, Long currentSpotId) {
        return spots.stream()
                .filter(spot -> !Objects.equals(spot.getId(), currentSpotId))
                .limit(3)
                .map(ScenicSpot::getName)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private boolean containsAny(String value, String... needles) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String needle : needles) {
            if (StringUtils.hasText(needle) && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeBudget(String budget, String versionType) {
        if ("cheaper".equals(versionType)) {
            return "economy";
        }
        String value = budget == null ? "" : budget.trim().toLowerCase();
        return Set.of("economy", "comfort", "luxury").contains(value) ? value : "comfort";
    }

    private String normalizePreference(String preference) {
        String value = preference == null ? "" : preference.trim().toLowerCase();
        return Set.of("natural", "cultural", "photography", "relaxation").contains(value) ? value : "natural";
    }

    private String normalizeVersionType(String versionType) {
        String value = versionType == null ? "" : versionType.trim().toLowerCase();
        return Set.of("cheaper", "relaxed", "hidden", "family").contains(value) ? value : "default";
    }

    private String versionLabel(String versionType) {
        return switch (versionType) {
            case "cheaper" -> "更省钱";
            case "relaxed" -> "更轻松";
            case "hidden" -> "更小众";
            case "family" -> "适合老人小孩";
            default -> "标准版";
        };
    }

    private int safeDays(Integer days) {
        return Math.min(15, Math.max(1, days == null ? 5 : days));
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncateText(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
