package com.tibet.tourism.modules.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotTagRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock private ScenicSpotRepository scenicSpotRepository;
    @Mock private SpotTagRepository tagRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserVisitHistoryRepository userVisitHistoryRepository;
    @Mock private TibetanTranslationService translationService;
    @Mock private NewsRepository newsRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private HeritageItemRepository itemRepository;
    @Mock private HeritageInheritorRepository inheritorRepository;
    @Mock private HeritageEventRepository eventRepository;
    @Mock private HeritageLikeRepository likeRepository;
    @Mock private HeritageCommentRepository heritageCommentRepository;

    @Test
    void adminScenicSpotListDoesNotExposeEntityGraphOrSensitiveFields() throws Exception {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(1L);
        spot.setName("Potala");
        spot.setDescription("Landmark");
        spot.setCategory(ScenicSpot.Category.CULTURAL);
        spot.setTicketPrice(new BigDecimal("200.00"));
        spot.setLatitude(new BigDecimal("29.6578"));
        spot.setLongitude(new BigDecimal("91.1169"));
        spot.setVisitCount(12);
        spot.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        when(scenicSpotRepository.findAllWithoutTags(any(Pageable.class)))
                .thenReturn(pageOf(spot));

        AdminScenicSpotController controller = new AdminScenicSpotController(
                scenicSpotRepository,
                tagRepository,
                commentRepository,
                commentLikeRepository,
                bookingRepository,
                userVisitHistoryRepository,
                translationService);

        String json = json(controller.getAllSpots(PageRequest.of(0, 10)).getBody());

        assertThat(json)
                .contains("\"name\":\"Potala\"")
                .contains("\"ticketPrice\":200.00")
                .doesNotContain("\"tags\"")
                .doesNotContain("\"hibernateLazyInitializer\"")
                .doesNotContain("\"handler\"");
        assertNoSensitiveAdminFields(json);
    }

    @Test
    void adminNewsListUsesExplicitContentContract() throws Exception {
        News news = new News();
        news.setId(2L);
        news.setTitle("Policy update");
        news.setContent("Public admin-editable content");
        news.setCategory(News.Category.NOTICE);
        news.setImageUrl("/uploads/news.jpg");
        news.setViewCount(9);
        news.setCreatedAt(LocalDateTime.parse("2026-02-03T04:05:06"));
        when(newsRepository.findAll(any(Pageable.class))).thenReturn(pageOf(news));

        AdminNewsController controller = new AdminNewsController(newsRepository, translationService);

        String json = json(controller.getAllNews(PageRequest.of(0, 10)).getBody());

        assertThat(json)
                .contains("\"title\":\"Policy update\"")
                .contains("\"viewCount\":9")
                .doesNotContain("\"author\"")
                .doesNotContain("\"hibernateLazyInitializer\"");
        assertNoSensitiveAdminFields(json);
    }

    @Test
    void adminHotelResponsesDoNotExposeRoomTypeEntityGraph() throws Exception {
        Hotel hotel = new Hotel();
        hotel.setId(3L);
        hotel.setName("Lhasa Stay");
        hotel.setLocation("Lhasa");
        hotel.setPhone("+86-0000");
        hotel.setPriceRange("300-500");
        hotel.setRating(new BigDecimal("4.8"));
        hotel.setFacilities("wifi,breakfast");
        hotel.setCreatedAt(LocalDateTime.parse("2026-03-04T05:06:07"));

        RoomType roomType = new RoomType();
        roomType.setId(4L);
        roomType.setHotel(hotel);
        roomType.setName("Standard");
        roomType.setPrice(new BigDecimal("320.00"));
        roomType.setCapacity(2);
        roomType.setSortOrder(1);
        hotel.setRoomTypes(List.of(roomType));

        when(hotelRepository.findAll(any(Pageable.class))).thenReturn(pageOf(hotel));
        when(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(3L)).thenReturn(List.of(roomType));

        AdminHotelController controller = new AdminHotelController(hotelRepository, roomTypeRepository);

        String hotelsJson = json(controller.getAllHotels(PageRequest.of(0, 10)).getBody());
        String roomTypesJson = json(controller.getRoomTypes(3L).getBody());

        assertThat(hotelsJson)
                .contains("\"name\":\"Lhasa Stay\"")
                .contains("\"phone\":\"+86-0000\"")
                .doesNotContain("\"roomTypes\"");
        assertThat(roomTypesJson)
                .contains("\"name\":\"Standard\"")
                .doesNotContain("\"hotel\"");
        assertNoSensitiveAdminFields(hotelsJson);
        assertNoSensitiveAdminFields(roomTypesJson);
    }

    @Test
    void adminHeritageResponsesDoNotExposeVersionOrEntityRelations() throws Exception {
        HeritageItem item = new HeritageItem();
        item.setId(5L);
        item.setName("Thangka");
        item.setDescription("Painted scroll");
        item.setCategory("craft");
        item.setRegion("Lhasa");
        item.setVersion(77L);
        item.setViewCount(20);
        item.setLikeCount(6);
        item.setCommentCount(2);
        item.setCreatedAt(LocalDateTime.parse("2026-04-05T06:07:08"));

        HeritageInheritor inheritor = new HeritageInheritor();
        inheritor.setId(6L);
        inheritor.setName("Tashi");
        inheritor.setLevel("national");
        inheritor.setHeritageItem(item);
        inheritor.setCreatedAt(LocalDateTime.parse("2026-04-06T06:07:08"));

        HeritageEvent event = new HeritageEvent();
        event.setId(7L);
        event.setTitle("Workshop");
        event.setEventDate(LocalDate.parse("2026-07-01"));
        event.setContactInfo("admin contact");
        event.setHeritageItem(item);
        event.setCreatedAt(LocalDateTime.parse("2026-04-07T06:07:08"));

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(pageOf(item));
        when(inheritorRepository.findByHeritageItemId(5L)).thenReturn(List.of(inheritor));
        when(eventRepository.findByHeritageItemId(5L)).thenReturn(List.of(event));

        AdminHeritageController controller = new AdminHeritageController(
                itemRepository,
                inheritorRepository,
                eventRepository,
                likeRepository,
                heritageCommentRepository);

        String itemsJson = json(controller.getAllItems(PageRequest.of(0, 10)).getBody());
        String inheritorsJson = json(controller.getInheritors(5L).getBody());
        String eventsJson = json(controller.getEvents(5L).getBody());

        assertThat(itemsJson)
                .contains("\"name\":\"Thangka\"")
                .doesNotContain("\"version\"");
        assertThat(inheritorsJson)
                .contains("\"heritageItemId\":5")
                .doesNotContain("\"heritageItem\":");
        assertThat(eventsJson)
                .contains("\"heritageItemId\":5")
                .doesNotContain("\"heritageItem\":");
        assertNoSensitiveAdminFields(itemsJson);
        assertNoSensitiveAdminFields(inheritorsJson);
        assertNoSensitiveAdminFields(eventsJson);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static <T> PageImpl<T> pageOf(T value) {
        return new PageImpl<>(List.of(value), PageRequest.of(0, 10), 1);
    }

    private static void assertNoSensitiveAdminFields(String json) {
        assertThat(json)
                .doesNotContain("\"password\"")
                .doesNotContain("\"token\"")
                .doesNotContain("\"session\"")
                .doesNotContain("\"secret\"")
                .doesNotContain("\"credential\"");
    }
}
