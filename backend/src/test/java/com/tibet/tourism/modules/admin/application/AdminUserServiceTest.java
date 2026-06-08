package com.tibet.tourism.modules.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query nativeQuery;

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
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
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
        deletes.verify(tibetTravelKitRepository).deleteByItineraryUserId(10L);
        deletes.verify(tibetTravelKitRepository).deleteByUserId(10L);
        deletes.verify(itineraryRepository).clearParentReferencesToUserItineraries(10L);
        deletes.verify(itineraryRepository).deleteByUserId(10L);
        deletes.verify(userVisitHistoryRepository).deleteByUserId(10L);
        deletes.verify(userRepository).delete(target);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(4)).createNativeQuery(sqlCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().get(0).contains("UPDATE invoices"));
        assertTrue(sqlCaptor.getAllValues().get(1).contains("UPDATE orders"));
        assertTrue(sqlCaptor.getAllValues().get(2).contains("UPDATE bookings"));
        assertTrue(sqlCaptor.getAllValues().get(3).contains("UPDATE hotel_bookings"));
        verify(nativeQuery, times(4)).setParameter("userId", 10L);
        verify(nativeQuery, times(3)).setParameter("anonymousLabel", "Deleted user");
        verify(nativeQuery, times(4)).executeUpdate();
        verify(platformOrderRepository, never()).deleteByUserId(10L);
        verify(bookingRepository, never()).deleteByUserId(10L);
        verify(hotelBookingRepository, never()).deleteByUserId(10L);
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

    @Test
    void actionPolicyUsesConfiguredSuperAdminUsername() {
        ReflectionTestUtils.setField(service, "superAdminUsername", "configured-root");
        User protectedAdmin = user(21L, "configured-root", User.Role.ADMIN);
        User ordinaryUser = user(22L, "traveler", User.Role.USER);
        User otherAdmin = user(23L, "admin2", User.Role.ADMIN);

        AdminUserService.UserActionPolicy protectedPolicy = service.actionPolicyFor(
                protectedAdmin, auth("configured-root"));
        AdminUserService.UserActionPolicy ordinaryPolicy = service.actionPolicyFor(
                ordinaryUser, auth("configured-root"));
        AdminUserService.UserActionPolicy adminPolicy = service.actionPolicyFor(
                otherAdmin, auth("configured-root"));
        AdminUserService.UserActionPolicy nonSuperPolicy = service.actionPolicyFor(
                ordinaryUser, auth("admin2"));

        assertTrue(protectedPolicy.protectedAccount());
        assertFalse(protectedPolicy.deletable());
        assertFalse(protectedPolicy.roleMutable());
        assertFalse(ordinaryPolicy.protectedAccount());
        assertTrue(ordinaryPolicy.deletable());
        assertTrue(ordinaryPolicy.roleMutable());
        assertFalse(adminPolicy.deletable());
        assertTrue(adminPolicy.roleMutable());
        assertFalse(nonSuperPolicy.deletable());
        assertFalse(nonSuperPolicy.roleMutable());
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
