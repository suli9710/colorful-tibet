package com.tibet.tourism.modules.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import com.tibet.tourism.modules.content.web.TibetanDictionaryController;
import com.tibet.tourism.modules.hotel.web.HotelBookingController;
import com.tibet.tourism.modules.order.web.OrderCenterController;
import com.tibet.tourism.modules.spot.web.PriceController;
import com.tibet.tourism.modules.spot.web.ScenicSpotController;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AdminAuditControllerContractTest {

    private static final Pattern MUTATION_MAPPING = Pattern.compile(
            "@(PostMapping|PutMapping|PatchMapping|DeleteMapping)\\b");
    private static final Pattern CAPTURE_CALL = Pattern.compile(
            "auditLogService\\.capture(?:Created)?\\(");
    private static final Pattern CREATED_CAPTURE_CALL = Pattern.compile(
            "auditLogService\\.captureCreated\\(");
    private static final Pattern CONDITIONAL_CAPTURE_CALL = Pattern.compile(
            "auditLogService\\.captureIfAdmin\\(");
    private static final Pattern BEGIN_CALL = Pattern.compile(
            "auditLogService\\.begin\\(");
    private static final Pattern COMPLETE_CALL = Pattern.compile(
            "auditLogService\\.complete\\(");

    private static final List<ControllerContract> CONTROLLERS = List.of(
            new ControllerContract(AdminCommunityController.class, 10),
            new ControllerContract(AdminHeritageController.class, 9),
            new ControllerContract(AdminNewsController.class, 3),
            new ControllerContract(AdminCarouselController.class, 3),
            new ControllerContract(AdminRouteController.class, 3),
            new ControllerContract(AdminHotelController.class, 6),
            new ControllerContract(AdminScenicSpotController.class, 3));

    @Test
    void everySelectedAdminMutationIsWrappedByTheAuditService() throws IOException {
        int mutationCount = 0;
        for (ControllerContract contract : CONTROLLERS) {
            String source = source(contract.type());
            int mappings = count(MUTATION_MAPPING, source);
            int captures = count(CAPTURE_CALL, source);

            assertThat(mappings)
                    .as("mutation mappings in %s", contract.type().getSimpleName())
                    .isEqualTo(contract.mutations());
            assertThat(captures)
                    .as("audit captures in %s", contract.type().getSimpleName())
                    .isEqualTo(mappings);
            mutationCount += mappings;
        }

        assertThat(mutationCount).isEqualTo(37);
    }

    @Test
    void adminImageUploadIsAuditedWithoutDuplicatingUserServiceAuditEvents() throws IOException {
        String source = source(AdminUserController.class);

        assertThat(count(MUTATION_MAPPING, source)).isEqualTo(4);
        assertThat(count(CAPTURE_CALL, source)).isEqualTo(1);
        assertThat(source).contains(
                "auditLogService.capture(\"admin_image\", null, \"admin_image_upload\"");
        assertThat(source)
                .doesNotContain("user_unlock", "user_role_update", "user_delete");
        assertThat(Arrays.stream(AdminUserController.class.getDeclaredConstructors()))
                .allMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
                        .contains(AdminAuditLogService.class));
    }

    @Test
    void auditedControllersRequireConstructorInjection() throws Exception {
        for (ControllerContract contract : CONTROLLERS) {
            assertThat(Arrays.stream(contract.type().getDeclaredConstructors()))
                    .as("constructors in %s", contract.type().getSimpleName())
                    .allMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
                            .contains(AdminAuditLogService.class));

            var field = contract.type().getDeclaredField("auditLogService");
            assertThat(field.getType()).isEqualTo(AdminAuditLogService.class);
            assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        }
    }

    @Test
    void remainingAdministratorCapabilitiesHaveExplicitAuditCoverage() throws Exception {
        String dictionary = source(
                "modules/content/web/TibetanDictionaryController.java");
        assertThat(count(MUTATION_MAPPING, dictionary)).isEqualTo(6);
        assertThat(count(CAPTURE_CALL, dictionary)).isEqualTo(5);
        assertThat(dictionary).contains(
                "tibetan_dictionary_create",
                "tibetan_dictionary_update",
                "tibetan_dictionary_delete",
                "tibetan_dictionary_batch_add",
                "tibetan_dictionary_initialize");

        String prices = source("modules/spot/web/PriceController.java");
        assertThat(count(MUTATION_MAPPING, prices)).isEqualTo(3);
        assertThat(count(CAPTURE_CALL, prices)).isEqualTo(3);
        assertThat(prices).contains(
                "scenic_spot_price_update",
                "scenic_spot_price_batch_update",
                "scenic_spot_price_job_start");

        String bookings = source("modules/hotel/web/HotelBookingController.java");
        assertThat(count(CAPTURE_CALL, bookings)).isEqualTo(1);
        assertThat(count(CONDITIONAL_CAPTURE_CALL, bookings)).isEqualTo(2);
        assertThat(bookings).contains(
                "hotel_booking_status_update",
                "hotel_booking_cancel",
                "hotel_booking_permanent_delete");

        String orders = source("modules/order/web/OrderCenterController.java");
        assertThat(count(CAPTURE_CALL, orders)).isEqualTo(1);
        assertThat(orders).contains("order_refund_review");

        String spots = source("modules/spot/web/ScenicSpotController.java");
        assertThat(count(BEGIN_CALL, spots)).isEqualTo(1);
        assertThat(count(COMPLETE_CALL, spots)).isEqualTo(2);
        assertThat(spots).contains(
                "item_similarity_precompute", "completed", "already_in_progress");

        String stats = source(AdminStatsController.class);
        assertThat(count(BEGIN_CALL, stats)).isEqualTo(1);
        assertThat(count(COMPLETE_CALL, stats)).isEqualTo(2);
        assertThat(stats).contains(
                "item_similarity_precompute", "completed", "already_in_progress");

        for (Class<?> controllerType : List.of(
                TibetanDictionaryController.class,
                PriceController.class,
                HotelBookingController.class,
                OrderCenterController.class,
                AdminStatsController.class)) {
            assertThat(Arrays.stream(controllerType.getDeclaredConstructors()))
                    .as("constructors in %s", controllerType.getSimpleName())
                    .allMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
                            .contains(AdminAuditLogService.class));
        }

        var scenicAuditField = ScenicSpotController.class.getDeclaredField("auditLogService");
        assertThat(scenicAuditField.getType()).isEqualTo(AdminAuditLogService.class);
        assertThat(scenicAuditField.getAnnotation(
                org.springframework.beans.factory.annotation.Autowired.class)).isNotNull();

        int coveredAdministratorMutations = CONTROLLERS.stream()
                .mapToInt(ControllerContract::mutations)
                .sum()
                + 1  // administrator image upload
                + 5  // dictionary mutations (translate is read-only)
                + 3  // price mutations
                + 1  // scenic-spot similarity precompute
                + 1  // booking status
                + 1  // refund review
                + 2  // mixed booking cancel/delete when invoked by an administrator
                + 1; // administrator dashboard precompute
        assertThat(coveredAdministratorMutations).isEqualTo(52);
    }

    @Test
    void singleEntityCreateMutationsBindThePersistedEntityIdentifier() throws IOException {
        String carousels = source(AdminCarouselController.class);
        String news = source(AdminNewsController.class);
        String hotels = source(AdminHotelController.class);
        String heritage = source(AdminHeritageController.class);
        String spots = source(AdminScenicSpotController.class);
        String routes = source(AdminRouteController.class);
        String dictionary = source("modules/content/web/TibetanDictionaryController.java");

        assertThat(count(CREATED_CAPTURE_CALL, carousels)).isEqualTo(1);
        assertThat(carousels).contains(
                "auditLogService.captureCreated(\"carousel\", \"carousel_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, news)).isEqualTo(1);
        assertThat(news).contains(
                "auditLogService.captureCreated(\"news\", \"news_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, hotels)).isEqualTo(2);
        assertThat(hotels).contains(
                "auditLogService.captureCreated(\"hotel\", \"hotel_create\"",
                "auditLogService.captureCreated(\"room_type\", \"room_type_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, heritage)).isEqualTo(3);
        assertThat(heritage).contains(
                "auditLogService.captureCreated(\"heritage_item\", \"heritage_item_create\"",
                "auditLogService.captureCreated(\"heritage_inheritor\", \"heritage_inheritor_create\"",
                "auditLogService.captureCreated(\"heritage_event\", \"heritage_event_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, spots)).isEqualTo(1);
        assertThat(spots).contains(
                "auditLogService.captureCreated(\"scenic_spot\", \"scenic_spot_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, routes)).isEqualTo(1);
        assertThat(routes).contains(
                "auditLogService.captureCreated(\"official_route\", \"official_route_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, dictionary)).isEqualTo(1);
        assertThat(dictionary).contains(
                "auditLogService.captureCreated(\"tibetan_dictionary\", \"tibetan_dictionary_create\"");

        assertThat(count(CREATED_CAPTURE_CALL, carousels)
                + count(CREATED_CAPTURE_CALL, news)
                + count(CREATED_CAPTURE_CALL, hotels)
                + count(CREATED_CAPTURE_CALL, heritage)
                + count(CREATED_CAPTURE_CALL, spots)
                + count(CREATED_CAPTURE_CALL, routes)
                + count(CREATED_CAPTURE_CALL, dictionary)).isEqualTo(10);
    }

    @Test
    void auditServiceCannotReceiveRawHttpRequestMaterial() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/tibet/tourism/modules/admin/application/AdminAuditLogService.java"),
                StandardCharsets.UTF_8);

        assertThat(source)
                .doesNotContain("HttpServletRequest")
                .doesNotContain("getRequestURI")
                .doesNotContain("getQueryString")
                .doesNotContain("getInputStream")
                .doesNotContain("getReader")
                .doesNotContain("getDeclaredMethod")
                .doesNotContain("getMethod(")
                .doesNotContain("getField(")
                .contains("CacheKeyHasher")
                .contains("targetIdExtractor.apply(response.getBody())")
                .contains("PROPAGATION_REQUIRES_NEW")
                .contains("SensitiveLogSanitizer.exceptionSummary(exception)");
    }

    private static String source(Class<?> controllerType) throws IOException {
        Path path = Path.of(
                "src/main/java/com/tibet/tourism/modules/admin/web/"
                        + controllerType.getSimpleName()
                        + ".java");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(
                Path.of("src/main/java/com/tibet/tourism/" + relativePath),
                StandardCharsets.UTF_8);
    }

    private static int count(Pattern pattern, String value) {
        int count = 0;
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private record ControllerContract(Class<?> type, int mutations) {
    }
}
