package com.tibet.tourism.modules.upload.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.common.api.ApiErrorResponder;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.modules.admin.application.AdminUserService;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.admin.web.AdminUserController;
import com.tibet.tourism.modules.community.web.CommentController;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.application.CurrentUserApplicationService;
import com.tibet.tourism.modules.user.infra.UserRepository;
import com.tibet.tourism.modules.user.web.CurrentUserController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class UploadControllerSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private AdminAuditLogService auditLogService;

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    @Mock
    private CurrentUserApplicationService currentUserApplicationService;

    @Mock
    private ApiErrorResponder apiErrorResponder;

    @Test
    @SuppressWarnings("unchecked")
    void adminUploadReturnsBadRequestWhenStorageRejectsFile() throws Exception {
        MultipartFile file = multipart("payload.svg", "image/svg+xml");
        AdminUserController controller = new AdminUserController(
                userRepository, loginAttemptService, fileStorageService, adminUserService, auditLogService);

        when(auditLogService.capture(
                anyString(), nullable(Long.class), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<ResponseEntity<?>>) invocation.getArgument(3)).get());

        when(fileStorageService.storeAdminImage(file))
                .thenThrow(new IllegalArgumentException("Unsupported image content type"));

        ResponseEntity<?> response = controller.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response)).containsKey("error");
        verify(fileStorageService).storeAdminImage(file);
    }

    @Test
    void commentUploadReturnsBadRequestWhenStorageRejectsFile() throws Exception {
        MultipartFile file = multipart("payload.html", "text/html");
        CommentController controller = new CommentController();
        ReflectionTestUtils.setField(controller, "fileStorageService", fileStorageService);

        when(fileStorageService.storeCommentImage(file))
                .thenThrow(new IllegalArgumentException("Unsupported image content type"));

        ResponseEntity<?> response = controller.uploadCommentImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> responseBody = body(response);
        assertThat(responseBody).containsEntry("message", "Image upload failed");
        assertThat(responseBody.toString()).doesNotContain("Unsupported image content type");
        verify(fileStorageService).storeCommentImage(file);
    }

    @Test
    void avatarUploadReturnsBadRequestWhenStorageRejectsFile() {
        MultipartFile file = multipart("avatar.jpg", "image/jpeg");
        HttpServletRequest request = new MockHttpServletRequest();
        CurrentUserController controller = new CurrentUserController(
                jwtAuthSupport, currentUserApplicationService, apiErrorResponder);

        when(jwtAuthSupport.resolveCurrentUserId(request)).thenReturn(7L);
        when(currentUserApplicationService.uploadAvatar(7L, file))
                .thenThrow(new IllegalArgumentException("Image signature does not match extension"));

        ResponseEntity<?> response = controller.uploadAvatar(file, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> responseBody = body(response);
        assertThat(responseBody).containsEntry("error", "Invalid upload file");
        assertThat(responseBody.toString()).doesNotContain("Image signature does not match extension");
        verify(currentUserApplicationService).uploadAvatar(7L, file);
    }

    @Test
    void avatarUploadFailureDoesNotLeakStorageExceptionMessageToResponseOrLogs() {
        MultipartFile file = multipart("avatar.jpg", "image/jpeg");
        HttpServletRequest request = new MockHttpServletRequest();
        CurrentUserController controller = new CurrentUserController(
                jwtAuthSupport, currentUserApplicationService, apiErrorResponder);
        ListAppender<ILoggingEvent> appender = attachCurrentUserControllerAppender();

        when(jwtAuthSupport.resolveCurrentUserId(request)).thenReturn(7L);
        when(currentUserApplicationService.uploadAvatar(7L, file))
                .thenThrow(new IllegalStateException("s3://private-bucket/avatar-token-secret failed"));

        ResponseEntity<?> response;
        try {
            response = controller.uploadAvatar(file, request);
        } finally {
            detachCurrentUserControllerAppender(appender);
        }

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> responseBody = body(response);
        assertThat(responseBody).containsEntry("error", "Avatar upload failed");
        assertThat(responseBody.toString()).doesNotContain("private-bucket", "avatar-token-secret");
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getFormattedMessage()).contains("type=IllegalStateException");
            assertThat(event.getFormattedMessage()).contains("messageHash=");
        });
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("private-bucket")
                        || message.contains("avatar-token-secret")
                        || message.contains("s3://"));
        verify(currentUserApplicationService).uploadAvatar(7L, file);
    }

    private MultipartFile multipart(String originalName, String contentType) {
        return new MockMultipartFile("file", originalName, contentType, new byte[]{'x'});
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private ListAppender<ILoggingEvent> attachCurrentUserControllerAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(CurrentUserController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachCurrentUserControllerAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(CurrentUserController.class);
        logger.detachAppender(appender);
        appender.stop();
    }
}
