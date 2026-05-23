package com.tibet.tourism.modules.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.ai.infra.AiRouteRecordRepository;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import com.tibet.tourism.modules.route.infra.ItineraryRepository;
import com.tibet.tourism.modules.route.infra.TibetTravelKitRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private RouteLikeRepository routeLikeRepository;
    @Mock
    private RouteCommentRepository routeCommentRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private SharedRouteRepository sharedRouteRepository;
    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private QuestionLikeRepository questionLikeRepository;
    @Mock
    private TravelAnswerRepository travelAnswerRepository;
    @Mock
    private TravelQuestionRepository travelQuestionRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PlatformOrderRepository platformOrderRepository;
    @Mock
    private OrderAuditLogRepository orderAuditLogRepository;
    @Mock
    private HotelBookingRepository hotelBookingRepository;
    @Mock
    private AiRouteRecordRepository aiRouteRecordRepository;
    @Mock
    private TibetTravelKitRepository tibetTravelKitRepository;
    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private UserVisitHistoryRepository userVisitHistoryRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                userRepository,
                commentLikeRepository,
                routeLikeRepository,
                routeCommentRepository,
                commentRepository,
                sharedRouteRepository,
                favoriteRepository,
                questionLikeRepository,
                travelAnswerRepository,
                travelQuestionRepository,
                bookingRepository,
                platformOrderRepository,
                orderAuditLogRepository,
                hotelBookingRepository,
                aiRouteRecordRepository,
                tibetTravelKitRepository,
                itineraryRepository,
                userVisitHistoryRepository);
        ReflectionTestUtils.setField(service, "superAdminUsername", "lzh");
    }

    @Test
    void nonSuperAdminCannotDeleteOrdinaryUser() {
        User target = user(10L, "traveler", User.Role.USER);

        AdminUserService.DeleteUserResult result = service.deleteUser(target, auth("admin"));

        assertFalse(result.success());
        assertEquals(403, result.status());
        verify(userRepository, never()).delete(target);
    }

    @Test
    void superAdminCanDeleteOrdinaryUser() {
        User target = user(10L, "traveler", User.Role.USER);

        AdminUserService.DeleteUserResult result = service.deleteUser(target, auth("lzh"));

        assertTrue(result.success());
        assertEquals(200, result.status());
        InOrder deletes = inOrder(
                commentLikeRepository,
                routeLikeRepository,
                routeCommentRepository,
                favoriteRepository,
                questionLikeRepository,
                travelAnswerRepository,
                travelQuestionRepository,
                commentRepository,
                sharedRouteRepository,
                aiRouteRecordRepository,
                orderAuditLogRepository,
                platformOrderRepository,
                bookingRepository,
                hotelBookingRepository,
                tibetTravelKitRepository,
                itineraryRepository,
                userVisitHistoryRepository,
                userRepository);
        deletes.verify(commentLikeRepository).deleteByCommentUserId(10L);
        deletes.verify(commentLikeRepository).deleteByUserId(10L);
        deletes.verify(routeLikeRepository).deleteByRouteAuthorId(10L);
        deletes.verify(routeCommentRepository).deleteByRouteAuthorId(10L);
        deletes.verify(routeLikeRepository).deleteByUser(target);
        deletes.verify(routeCommentRepository).deleteByUser(target);
        deletes.verify(favoriteRepository).deleteByUser(target);
        deletes.verify(questionLikeRepository).deleteByQuestionAuthorId(10L);
        deletes.verify(questionLikeRepository).deleteByUser(target);
        deletes.verify(travelAnswerRepository).deleteByQuestionAuthorId(10L);
        deletes.verify(travelAnswerRepository).deleteByUser(target);
        deletes.verify(travelQuestionRepository).deleteByAuthor(target);
        deletes.verify(commentRepository).deleteByUser(target);
        deletes.verify(sharedRouteRepository).deleteByAuthor(target);
        deletes.verify(aiRouteRecordRepository).deleteByUserId(10L);
        deletes.verify(orderAuditLogRepository).clearActorUserByUserId(10L);
        deletes.verify(platformOrderRepository).deleteByUserId(10L);
        deletes.verify(bookingRepository).deleteByUserId(10L);
        deletes.verify(hotelBookingRepository).deleteByUserId(10L);
        deletes.verify(tibetTravelKitRepository).deleteByItineraryUserId(10L);
        deletes.verify(tibetTravelKitRepository).deleteByUserId(10L);
        deletes.verify(itineraryRepository).clearParentReferencesToUserItineraries(10L);
        deletes.verify(itineraryRepository).deleteByUserId(10L);
        deletes.verify(userVisitHistoryRepository).deleteByUserId(10L);
        deletes.verify(userRepository).delete(target);
    }

    @Test
    void deleteRejectsAdminsSuperAdminAndSelf() {
        User admin = user(11L, "admin2", User.Role.ADMIN);
        User superAdmin = user(12L, "lzh", User.Role.ADMIN);
        User self = user(13L, "admin", User.Role.USER);

        assertDeleteRejected(admin, auth("lzh"), 403);
        assertDeleteRejected(superAdmin, auth("lzh"), 400);
        assertDeleteRejected(self, auth("admin"), 403);
    }

    private void assertDeleteRejected(User target, Authentication auth, int status) {
        AdminUserService.DeleteUserResult result = service.deleteUser(target, auth);

        assertFalse(result.success());
        assertEquals(status, result.status());
        verify(userRepository, never()).delete(target);
    }

    private User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private Authentication auth(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
