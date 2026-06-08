package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.modules.admin.web.AdminHeritageController;
import com.tibet.tourism.modules.ai.web.AiGuideChatController;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ControllerAuthorizationMatrixTest {

    private static final List<Class<?>> CONTROLLERS = discoverRestControllers();

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
        assertThat(CONTROLLERS)
                .as("controller discovery must find the application @RestController classes")
                .contains(AiGuideChatController.class, AdminHeritageController.class);
        return CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods())
                        .flatMap(method -> endpointsFor(controller, method)));
    }

    private static List<Class<?>> discoverRestControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> controllers = new ArrayList<>();
        scanner.findCandidateComponents("com.tibet.tourism.modules").forEach(beanDefinition -> {
            Class<?> controller = loadControllerClass(beanDefinition.getBeanClassName());
            if (!controller.isSynthetic()) {
                controllers.add(controller);
            }
        });
        return List.copyOf(controllers);
    }

    private static Class<?> loadControllerClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load controller " + className, e);
        }
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
