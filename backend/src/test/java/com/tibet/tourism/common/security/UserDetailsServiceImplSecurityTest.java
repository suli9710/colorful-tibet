package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void usernameNotFoundExceptionDoesNotExposeRawUsername() {
        UserDetailsServiceImpl service = service();
        String rawUsername = "traveler.secret@example.com";

        when(userRepository.findByUsername(rawUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(rawUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found")
                .hasMessageNotContaining("traveler.secret@example.com");
    }

    @Test
    void authorityDebugLogDoesNotExposeRawUsername() {
        UserDetailsServiceImpl service = service();
        User user = new User();
        user.setId(7L);
        user.setUsername("admin.secret@example.com");
        user.setPassword("hashed");
        user.setRole(User.Role.ADMIN);
        ListAppender<ILoggingEvent> appender = attachDebugAppender();

        when(userRepository.findByUsername("admin.secret@example.com")).thenReturn(Optional.of(user));

        try {
            service.loadUserByUsername("admin.secret@example.com");
        } finally {
            detachDebugAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage()).contains("user#"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("admin.secret@example.com"));
    }

    private UserDetailsServiceImpl service() {
        UserDetailsServiceImpl service = new UserDetailsServiceImpl();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "hotelBookingPiiReaderUsernames", "");
        return service;
    }

    private ListAppender<ILoggingEvent> attachDebugAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserDetailsServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setName("user-details-security-test");
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        return appender;
    }

    private void detachDebugAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(UserDetailsServiceImpl.class);
        logger.detachAppender(appender);
        logger.setLevel(null);
        appender.stop();
    }
}
