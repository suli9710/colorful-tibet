package com.tibet.tourism.modules.admin.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.modules.admin.application.AdminUserService;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.content.application.TibetanTranslationService;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotTagRepository;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.infra.UserRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminPageResponseContractTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private SharedRouteRepository sharedRouteRepository;

    @Mock
    private RouteLikeRepository routeLikeRepository;

    @Mock
    private RouteCommentRepository routeCommentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private TravelQuestionRepository travelQuestionRepository;

    @Mock
    private TravelAnswerRepository travelAnswerRepository;

    @Mock
    private QuestionLikeRepository questionLikeRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private TibetanTranslationService translationService;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private ScenicSpotRepository scenicSpotRepository;

    @Mock
    private SpotTagRepository spotTagRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserVisitHistoryRepository userVisitHistoryRepository;

    @Mock
    private HeritageItemRepository heritageItemRepository;

    @Mock
    private HeritageInheritorRepository heritageInheritorRepository;

    @Mock
    private HeritageEventRepository heritageEventRepository;

    @Mock
    private HeritageLikeRepository heritageLikeRepository;

    @Mock
    private HeritageCommentRepository heritageCommentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminUserController(
                                userRepository, loginAttemptService, fileStorageService, adminUserService),
                        new AdminCommunityController(
                                sharedRouteRepository,
                                routeLikeRepository,
                                routeCommentRepository,
                                commentRepository,
                                commentLikeRepository,
                                travelQuestionRepository,
                                travelAnswerRepository,
                                questionLikeRepository),
                        new AdminNewsController(newsRepository, translationService),
                        new AdminHotelController(hotelRepository, roomTypeRepository),
                        new AdminScenicSpotController(
                                scenicSpotRepository,
                                spotTagRepository,
                                commentRepository,
                                commentLikeRepository,
                                bookingRepository,
                                userVisitHistoryRepository,
                                translationService),
                        new AdminHeritageController(
                                heritageItemRepository,
                                heritageInheritorRepository,
                                heritageEventRepository,
                                heritageLikeRepository,
                                heritageCommentRepository),
                        new AdminRouteController(
                                sharedRouteRepository, routeLikeRepository, routeCommentRepository, userRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Test
    void usersListReturnsStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(page());

        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/users?page=1&size=2")));
    }

    @Test
    void communityListsReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        when(sharedRouteRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());
        when(routeCommentRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());
        when(commentRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());
        when(travelQuestionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());
        when(travelAnswerRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());

        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/community/routes?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/community/comments?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/community/spot-comments?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/community/questions?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/community/answers?page=1&size=2")));
    }

    @Test
    void adminContentListsReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        when(newsRepository.findAll(any(Pageable.class)))
                .thenReturn(page());
        when(hotelRepository.findAll(any(Pageable.class)))
                .thenReturn(page());
        when(scenicSpotRepository.findAllWithoutTags(any(Pageable.class)))
                .thenReturn(page());
        when(heritageItemRepository.findAll(any(Pageable.class)))
                .thenReturn(page());
        when(sharedRouteRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page());

        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/news?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/hotels?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/spots?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/heritage?page=1&size=2")));
        expectStablePageEnvelope(mockMvc.perform(get("/api/admin/routes?page=1&size=2")));
    }

    private static <T> PageImpl<T> page() {
        return new PageImpl<>(List.of(), PageRequest.of(1, 2), 5);
    }

    private static void expectStablePageEnvelope(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.first").doesNotExist())
                .andExpect(jsonPath("$.last").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist())
                .andExpect(content().string(not(containsString("\"pageable\""))))
                .andExpect(content().string(not(containsString("\"sort\""))))
                .andExpect(content().string(not(containsString("\"number\""))))
                .andExpect(content().string(not(containsString("\"numberOfElements\""))))
                .andExpect(content().string(not(containsString("\"first\""))))
                .andExpect(content().string(not(containsString("\"last\""))))
                .andExpect(content().string(not(containsString("\"empty\""))));
    }
}
