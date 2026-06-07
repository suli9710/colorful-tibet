package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.common.api.TestController;
import com.tibet.tourism.modules.admin.web.AdminCarouselController;
import com.tibet.tourism.modules.admin.web.AdminCommunityController;
import com.tibet.tourism.modules.admin.web.AdminHotelController;
import com.tibet.tourism.modules.admin.web.AdminNewsController;
import com.tibet.tourism.modules.admin.web.AdminRouteController;
import com.tibet.tourism.modules.admin.web.AdminScenicSpotController;
import com.tibet.tourism.modules.admin.web.AdminSecurityPostureController;
import com.tibet.tourism.modules.admin.web.AdminStatsController;
import com.tibet.tourism.modules.admin.web.AdminUserController;
import com.tibet.tourism.modules.ai.web.AiRouteController;
import com.tibet.tourism.modules.auth.web.AuthController;
import com.tibet.tourism.modules.community.web.CommentController;
import com.tibet.tourism.modules.community.web.FavoriteController;
import com.tibet.tourism.modules.community.web.SharedRouteController;
import com.tibet.tourism.modules.community.web.TravelQAController;
import com.tibet.tourism.modules.content.web.CarouselController;
import com.tibet.tourism.modules.content.web.HeritageController;
import com.tibet.tourism.modules.content.web.NewsController;
import com.tibet.tourism.modules.content.web.TibetanDictionaryController;
import com.tibet.tourism.modules.hotel.web.HotelBookingController;
import com.tibet.tourism.modules.order.web.BookingController;
import com.tibet.tourism.modules.order.web.OrderCenterController;
import com.tibet.tourism.modules.route.web.ItineraryController;
import com.tibet.tourism.modules.route.web.TibetSpecialtyController;
import com.tibet.tourism.modules.spot.web.PriceController;
import com.tibet.tourism.modules.spot.web.ScenicSpotController;
import com.tibet.tourism.modules.user.web.CurrentUserController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ControllerAuthorizationMatrixTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            TestController.class,
            AuthController.class,
            CurrentUserController.class,
            AiRouteController.class,
            OrderCenterController.class,
            BookingController.class,
            AdminCarouselController.class,
            AdminCommunityController.class,
            AdminHotelController.class,
            AdminNewsController.class,
            AdminRouteController.class,
            AdminScenicSpotController.class,
            AdminSecurityPostureController.class,
            AdminStatsController.class,
            AdminUserController.class,
            TibetSpecialtyController.class,
            ItineraryController.class,
            TibetanDictionaryController.class,
            NewsController.class,
            HeritageController.class,
            CarouselController.class,
            HotelBookingController.class,
            ScenicSpotController.class,
            PriceController.class,
            CommentController.class,
            TravelQAController.class,
            SharedRouteController.class,
            FavoriteController.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappedEndpoints")
    void nonPublicControllerEndpointsDeclareMethodSecurity(Endpoint endpoint) {
        if (ApiSecurityPaths.isPublicRequest(endpoint.httpMethod(), endpoint.samplePath())) {
            return;
        }

        assertThat(effectivePreAuthorize(endpoint.controller(), endpoint.method()))
                .as("%s %s handled by %s#%s must declare @PreAuthorize",
                        endpoint.httpMethod(),
                        endpoint.samplePath(),
                        endpoint.controller().getSimpleName(),
                        endpoint.method().getName())
                .isNotNull();
    }

    static Stream<Endpoint> mappedEndpoints() {
        return CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods())
                        .flatMap(method -> endpointsFor(controller, method)));
    }

    private static Stream<Endpoint> endpointsFor(Class<?> controller, Method method) {
        MethodMapping methodMapping = methodMapping(method);
        if (methodMapping == null) {
            return Stream.empty();
        }

        List<String> classPaths = requestMappingPaths(controller.getAnnotation(RequestMapping.class));
        return classPaths.stream()
                .flatMap(classPath -> methodMapping.paths().stream()
                        .map(methodPath -> combinePaths(classPath, methodPath)))
                .flatMap(path -> methodMapping.httpMethods().stream()
                        .map(httpMethod -> new Endpoint(controller, method, httpMethod, samplePath(path))));
    }

    private static PreAuthorize effectivePreAuthorize(Class<?> controller, Method method) {
        PreAuthorize methodAnnotation = method.getAnnotation(PreAuthorize.class);
        return methodAnnotation != null ? methodAnnotation : controller.getAnnotation(PreAuthorize.class);
    }

    private static List<String> requestMappingPaths(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return List.of("");
        }
        return paths(requestMapping.value(), requestMapping.path());
    }

    private static MethodMapping methodMapping(Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return new MethodMapping(List.of("GET"), paths(getMapping.value(), getMapping.path()));
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return new MethodMapping(List.of("POST"), paths(postMapping.value(), postMapping.path()));
        }
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return new MethodMapping(List.of("PUT"), paths(putMapping.value(), putMapping.path()));
        }
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return new MethodMapping(List.of("DELETE"), paths(deleteMapping.value(), deleteMapping.path()));
        }
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return new MethodMapping(List.of("PATCH"), paths(patchMapping.value(), patchMapping.path()));
        }
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping == null || requestMapping.method().length == 0) {
            return null;
        }
        return new MethodMapping(
                Arrays.stream(requestMapping.method()).map(Enum::name).toList(),
                paths(requestMapping.value(), requestMapping.path()));
    }

    private static List<String> paths(String[] values, String[] paths) {
        String[] selected = paths.length > 0 ? paths : values;
        if (selected.length == 0) {
            return List.of("");
        }
        return Arrays.asList(selected);
    }

    private static String combinePaths(String classPath, String methodPath) {
        if (classPath == null || classPath.isBlank()) {
            return normalizePath(methodPath);
        }
        if (methodPath == null || methodPath.isBlank()) {
            return normalizePath(classPath);
        }
        return normalizePath(trimTrailingSlash(classPath) + "/" + trimLeadingSlash(methodPath));
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String trimTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static String trimLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String samplePath(String path) {
        return path.replaceAll("\\{[^/]+}", "1");
    }

    private record MethodMapping(List<String> httpMethods, List<String> paths) {
    }

    private record Endpoint(Class<?> controller, Method method, String httpMethod, String samplePath) {

        @Override
        public String toString() {
            return httpMethod + " " + samplePath + " -> "
                    + controller.getSimpleName() + "#" + method.getName();
        }
    }
}
