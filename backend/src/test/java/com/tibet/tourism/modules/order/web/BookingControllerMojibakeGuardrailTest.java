package com.tibet.tourism.modules.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BookingControllerMojibakeGuardrailTest {

    private static final List<String> BOOKING_CONTROLLER_SOURCES = List.of(
            "src/main/java/com/tibet/tourism/modules/order/web/BookingController.java",
            "src/main/java/com/tibet/tourism/modules/hotel/web/HotelBookingController.java");

    private static final Pattern KNOWN_MOJIBAKE_TEXT = Pattern.compile(
            "[\\uFFFD\\uE000-\\uF8FF]|[ÃÂâèæåç]|[璇琚畨缁嫤鎴獙閰簵棰暟娉鎵閫鏃犳潈鎿嶇綔祫婧鐘舵佸厑杩欐牱鍙樻洿鍏娣櫙粹湀]");

    @Test
    void bookingControllersDoNotContainKnownMojibakeText() throws IOException {
        Path backendRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (String sourcePath : BOOKING_CONTROLLER_SOURCES) {
            Path file = backendRoot.resolve(sourcePath);
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = KNOWN_MOJIBAKE_TEXT.matcher(source);
            while (matcher.find()) {
                violations.add(sourcePath + ":" + lineNumber(source, matcher.start())
                        + " contains suspicious mojibake text near `"
                        + snippet(source, matcher.start()) + "`");
            }
        }

        assertThat(violations)
                .describedAs("Booking controller response text must not regress to mojibake:%n%s",
                        String.join(System.lineSeparator(), violations))
                .isEmpty();
    }

    private static Path backendRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        for (Path current = cwd; current != null; current = current.getParent()) {
            if (isBackendRoot(current)) {
                return current;
            }
            Path backend = current.resolve("backend");
            if (isBackendRoot(backend)) {
                return backend;
            }
        }
        throw new AssertionError("Could not locate backend source root from " + cwd);
    }

    private static boolean isBackendRoot(Path path) {
        return Files.isDirectory(path.resolve("src/main/java"))
                && Files.isDirectory(path.resolve("src/test/java"));
    }

    private static int lineNumber(String source, int offset) {
        int line = 1;
        for (int index = 0; index < offset; index++) {
            if (source.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String snippet(String source, int offset) {
        int start = Math.max(0, offset - 24);
        int end = Math.min(source.length(), offset + 24);
        return source.substring(start, end).replaceAll("\\s+", " ").trim();
    }
}
