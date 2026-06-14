package com.tibet.tourism.modules.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.security.PiiCryptoConverter;
import com.tibet.tourism.modules.admin.domain.AdminAuditLog;
import com.tibet.tourism.modules.admin.infra.AdminAuditLogRepository;
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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminUserServiceTest {

    private static final String KEY_32_BYTES_BASE64 =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginAttemptService loginAttemptService;
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
    private AdminAuditLogRepository adminAuditLogRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query nativeQuery;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                userRepository,
                loginAttemptService,
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
                userVisitHistoryRepository,
                adminAuditLogRepository);
        ReflectionTestUtils.setField(service, "superAdminUsername", "lzh");
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
    }

    @AfterEach
    void resetPiiKeys() {
        configurePii("", "", "");
    }

    @Test
    void nonSuperAdminCannotDeleteOrdinaryUser() {
        User actor = user(1L, "manager", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(actor));

        AdminUserService.DeleteUserResult result = service.deleteUser(target, auth("manager"));

        assertFalse(result.success());
        assertEquals(403, result.status());
        verify(userRepository, never()).delete(target);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_delete", audit.getAction());
        assertEquals("denied", audit.getResult());
        assertEquals("requires_super_admin", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(10L, audit.getTargetId());
        assertEquals("USER", audit.getBeforeRole());
        assertNull(audit.getAfterRole());
        assertAuditDoesNotContain(audit, "manager", "traveler");
    }

    @Test
    void unauthenticatedDeletePersistsFailureAuditWithoutRawUsername() {
        User target = user(10L, "traveler", User.Role.USER);

        AdminUserService.DeleteUserResult result = service.deleteUser(target, null);

        assertFalse(result.success());
        assertEquals(401, result.status());
        verify(userRepository, never()).delete(target);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_delete", audit.getAction());
        assertEquals("failure", audit.getResult());
        assertEquals("unauthenticated", audit.getReason());
        assertNull(audit.getActorId());
        assertEquals("user#anonymous", audit.getActorRef());
        assertEquals(10L, audit.getTargetId());
        assertEquals("USER", audit.getBeforeRole());
        assertAuditDoesNotContain(audit, "traveler");
    }

    @Test
    void superAdminCanDeleteOrdinaryUser() {
        configurePii("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        User actor = user(1L, "lzh", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));

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
        ArgumentCaptor<Object> anonymousLabelCaptor = ArgumentCaptor.forClass(Object.class);
        verify(nativeQuery, times(3)).setParameter(eq("anonymousLabel"), anonymousLabelCaptor.capture());
        PiiCryptoConverter converter = new PiiCryptoConverter();
        anonymousLabelCaptor.getAllValues().forEach(value -> {
            assertTrue(value instanceof String);
            String encryptedLabel = (String) value;
            assertTrue(encryptedLabel.startsWith("enc:v2:kid1:"));
            assertEquals("Deleted user", converter.convertToEntityAttribute(encryptedLabel));
        });
        verify(nativeQuery, times(4)).executeUpdate();
        verify(platformOrderRepository, never()).deleteByUserId(10L);
        verify(bookingRepository, never()).deleteByUserId(10L);
        verify(hotelBookingRepository, never()).deleteByUserId(10L);

        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_delete", audit.getAction());
        assertEquals("success", audit.getResult());
        assertEquals("authorized", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(10L, audit.getTargetId());
        assertEquals("USER", audit.getBeforeRole());
        assertNull(audit.getAfterRole());
        assertAuditDoesNotContain(audit, "lzh", "traveler");
    }

    private static void configurePii(String rawKeys, String activeKid, String legacyRawKey) {
        try {
            Method configure = PiiCryptoConverter.class.getDeclaredMethod(
                    "configure", String.class, String.class, String.class);
            configure.setAccessible(true);
            configure.invoke(null, rawKeys, activeKid, legacyRawKey);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to configure PII test keys", exception);
        }
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
    void cannotDeleteReservedOfficialSystemAccount() {
        User official = user(30L, "official", User.Role.USER);

        AdminUserService.DeleteUserResult result = service.deleteUser(official, auth("lzh"));

        assertFalse(result.success());
        assertEquals(400, result.status());
        verify(userRepository, never()).delete(official);
        verify(sharedRouteRepository, never()).deleteByAuthor(official);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_delete", audit.getAction());
        assertEquals("denied", audit.getResult());
        assertEquals("protected_account", audit.getReason());
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

    @Test
    void superAdminRoleUpdatePersistsAuditWithoutRawUsernames() {
        User actor = user(1L, "lzh", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));

        AdminUserService.RoleUpdateResult result = service.updateRole(target, "ADMIN", auth("lzh"));

        assertTrue(result.success());
        assertEquals(200, result.status());
        assertEquals(User.Role.ADMIN, target.getRole());
        assertEquals(1L, target.getSessionVersion());
        verify(userRepository).save(target);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_role_update", audit.getAction());
        assertEquals("success", audit.getResult());
        assertEquals("authorized", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(10L, audit.getTargetId());
        assertEquals("USER", audit.getBeforeRole());
        assertEquals("ADMIN", audit.getAfterRole());
        assertAuditDoesNotContain(audit, "lzh", "traveler");
    }

    @Test
    void deniedRoleUpdatePersistsAuditWithoutRawUsernames() {
        User actor = user(1L, "manager", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(actor));

        AdminUserService.RoleUpdateResult result = service.updateRole(target, "ADMIN", auth("manager"));

        assertFalse(result.success());
        assertEquals(403, result.status());
        verify(userRepository, never()).save(target);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_role_update", audit.getAction());
        assertEquals("denied", audit.getResult());
        assertEquals("requires_super_admin", audit.getReason());
        assertEquals("USER", audit.getBeforeRole());
        assertEquals("ADMIN", audit.getAfterRole());
        assertAuditDoesNotContain(audit, "manager", "traveler");
    }

    @Test
    void invalidRolePersistsFailureAuditWithoutRawUsernames() {
        User actor = user(1L, "lzh", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));

        AdminUserService.RoleUpdateResult result = service.updateRole(target, "OWNER", auth("lzh"));

        assertFalse(result.success());
        assertEquals(400, result.status());
        verify(userRepository, never()).save(target);
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_role_update", audit.getAction());
        assertEquals("failure", audit.getResult());
        assertEquals("invalid_role", audit.getReason());
        assertEquals("USER", audit.getBeforeRole());
        assertEquals("USER", audit.getAfterRole());
        assertAuditDoesNotContain(audit, "lzh", "traveler", "OWNER");
    }

    @Test
    void roleUpdateMissingTargetPersistsFailureAudit() {
        User actor = user(1L, "lzh", User.Role.ADMIN);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        AdminUserService.RoleUpdateResult result = service.updateRole(404L, "ADMIN", auth("lzh"));

        assertFalse(result.success());
        assertEquals(404, result.status());
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_role_update", audit.getAction());
        assertEquals("failure", audit.getResult());
        assertEquals("target_not_found", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(404L, audit.getTargetId());
        assertEquals("user#missing", audit.getTargetRef());
        assertAuditDoesNotContain(audit, "lzh");
    }

    @Test
    void deleteMissingTargetPersistsFailureAudit() {
        User actor = user(1L, "lzh", User.Role.ADMIN);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        AdminUserService.DeleteUserResult result = service.deleteUser(404L, auth("lzh"));

        assertFalse(result.success());
        assertEquals(404, result.status());
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_delete", audit.getAction());
        assertEquals("failure", audit.getResult());
        assertEquals("target_not_found", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(404L, audit.getTargetId());
        assertEquals("user#missing", audit.getTargetRef());
        assertAuditDoesNotContain(audit, "lzh");
    }

    @Test
    void ordinaryAdminCanUnlockOrdinaryUser(CapturedOutput output) {
        User actor = user(1L, "manager", User.Role.ADMIN);
        User target = user(10L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(actor));
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        AdminUserService.UnlockUserResult result = service.unlockLogin(10L, auth("manager"));

        assertTrue(result.success());
        assertEquals(200, result.status());
        verify(loginAttemptService).reset("traveler");
        String logs = output.toString();
        assertTrue(logs.contains("action=admin_user_unlock_login"));
        assertTrue(logs.contains("result=success"));
        assertTrue(logs.contains("reason=authorized"));
        assertTrue(logs.contains("actorId=1"));
        assertTrue(logs.contains("targetId=10"));
        assertFalse(logs.contains("manager"));
        assertFalse(logs.contains("traveler"));
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_unlock_login", audit.getAction());
        assertEquals("success", audit.getResult());
        assertEquals("authorized", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(10L, audit.getTargetId());
        assertNull(audit.getBeforeRole());
        assertNull(audit.getAfterRole());
        assertAuditDoesNotContain(audit, "manager", "traveler");
    }

    @Test
    void ordinaryAdminCannotUnlockAdminOrProtectedAccount(CapturedOutput output) {
        User actor = user(1L, "manager", User.Role.ADMIN);
        User adminTarget = user(11L, "admin2", User.Role.ADMIN);
        User protectedTarget = user(12L, "lzh", User.Role.ADMIN);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(actor));
        when(userRepository.findById(11L)).thenReturn(Optional.of(adminTarget));
        when(userRepository.findById(12L)).thenReturn(Optional.of(protectedTarget));

        AdminUserService.UnlockUserResult adminResult = service.unlockLogin(11L, auth("manager"));
        AdminUserService.UnlockUserResult protectedResult = service.unlockLogin(12L, auth("manager"));

        assertFalse(adminResult.success());
        assertEquals(403, adminResult.status());
        assertFalse(protectedResult.success());
        assertEquals(403, protectedResult.status());
        verify(loginAttemptService, never()).reset(anyString());
        String logs = output.toString();
        assertTrue(logs.contains("reason=requires_super_admin"));
        assertTrue(logs.contains("targetId=11"));
        assertTrue(logs.contains("targetId=12"));
        assertFalse(logs.contains("manager"));
        assertFalse(logs.contains("admin2"));
        assertFalse(logs.contains("lzh"));
        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository, times(2)).save(auditCaptor.capture());
        assertTrue(auditCaptor.getAllValues().stream()
                .allMatch(audit -> "admin_user_unlock_login".equals(audit.getAction())
                        && "denied".equals(audit.getResult())
                        && "requires_super_admin".equals(audit.getReason())));
        auditCaptor.getAllValues().forEach(audit -> assertAuditDoesNotContain(audit, "manager", "admin2", "lzh"));
    }

    @Test
    void superAdminCanUnlockManagementAndProtectedAccounts(CapturedOutput output) {
        User actor = user(1L, "lzh", User.Role.ADMIN);
        User adminTarget = user(11L, "admin2", User.Role.ADMIN);
        User protectedTarget = user(12L, "lzh", User.Role.ADMIN);
        when(userRepository.findByUsername("lzh")).thenReturn(Optional.of(actor));
        when(userRepository.findById(11L)).thenReturn(Optional.of(adminTarget));
        when(userRepository.findById(12L)).thenReturn(Optional.of(protectedTarget));

        AdminUserService.UnlockUserResult adminResult = service.unlockLogin(11L, auth("lzh"));
        AdminUserService.UnlockUserResult protectedResult = service.unlockLogin(12L, auth("lzh"));

        assertTrue(adminResult.success());
        assertEquals(200, adminResult.status());
        assertTrue(protectedResult.success());
        assertEquals(200, protectedResult.status());
        verify(loginAttemptService).reset("admin2");
        verify(loginAttemptService).reset("lzh");
        String logs = output.toString();
        assertTrue(logs.contains("result=success"));
        assertTrue(logs.contains("targetId=11"));
        assertTrue(logs.contains("targetId=12"));
        assertFalse(logs.contains("admin2"));
        assertFalse(logs.contains("lzh"));
        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository, times(2)).save(auditCaptor.capture());
        assertTrue(auditCaptor.getAllValues().stream()
                .allMatch(audit -> "admin_user_unlock_login".equals(audit.getAction())
                        && "success".equals(audit.getResult())
                        && "authorized".equals(audit.getReason())));
        auditCaptor.getAllValues().forEach(audit -> assertAuditDoesNotContain(audit, "lzh", "admin2"));
    }

    @Test
    void unlockAuditsTargetNotFoundWithoutReset(CapturedOutput output) {
        User actor = user(1L, "manager", User.Role.ADMIN);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(actor));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        AdminUserService.UnlockUserResult result = service.unlockLogin(404L, auth("manager"));

        assertFalse(result.success());
        assertEquals(404, result.status());
        verify(loginAttemptService, never()).reset(anyString());
        String logs = output.toString();
        assertTrue(logs.contains("result=failure"));
        assertTrue(logs.contains("reason=target_not_found"));
        assertTrue(logs.contains("targetId=404"));
        assertTrue(logs.contains("targetRef=user#missing"));
        assertFalse(logs.contains("manager"));
        AdminAuditLog audit = captureOnlyAudit();
        assertEquals("admin_user_unlock_login", audit.getAction());
        assertEquals("failure", audit.getResult());
        assertEquals("target_not_found", audit.getReason());
        assertEquals(1L, audit.getActorId());
        assertEquals(404L, audit.getTargetId());
        assertEquals("user#missing", audit.getTargetRef());
        assertAuditDoesNotContain(audit, "manager");
    }

    private void assertDeleteRejected(User target, Authentication auth, int status) {
        AdminUserService.DeleteUserResult result = service.deleteUser(target, auth);

        assertFalse(result.success());
        assertEquals(status, result.status());
        verify(userRepository, never()).delete(target);
    }

    private AdminAuditLog captureOnlyAudit() {
        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());
        return auditCaptor.getValue();
    }

    private void assertAuditDoesNotContain(AdminAuditLog audit, String... forbiddenValues) {
        String serialized = String.join("|",
                String.valueOf(audit.getActorId()),
                audit.getActorRef(),
                String.valueOf(audit.getTargetId()),
                audit.getTargetRef(),
                audit.getAction(),
                audit.getResult(),
                audit.getReason(),
                String.valueOf(audit.getBeforeRole()),
                String.valueOf(audit.getAfterRole()));
        for (String forbiddenValue : forbiddenValues) {
            assertFalse(serialized.contains(forbiddenValue));
        }
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
