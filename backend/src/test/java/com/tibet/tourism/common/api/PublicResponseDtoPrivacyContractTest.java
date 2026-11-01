package com.tibet.tourism.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.admin.web.dto.SecurityPostureResponse;
import com.tibet.tourism.modules.community.web.dto.PublicUserResponse;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationDebugResponse;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationEvaluationResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublicResponseDtoPrivacyContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final List<String> PUBLIC_RESPONSE_PACKAGES = List.of(
            "com.tibet.tourism.modules.community.web.dto",
            "com.tibet.tourism.modules.content.web.dto",
            "com.tibet.tourism.modules.spot.web.dto");

    private static final List<String> INTERNAL_DIAGNOSTIC_EXCLUDED_PREFIXES = List.of(
            "com.tibet.tourism.modules.admin.",
            "com.tibet.tourism.modules.recommendation.");

    private static final Set<String> REQUEST_LIKE_DTO_TYPES = Set.of(
            "com.tibet.tourism.modules.spot.web.dto.UserPreferenceDTO");

    private static final Set<String> FORBIDDEN_PUBLIC_SENSITIVE_PROPERTIES = Set.of(
            "userId",
            "user_id",
            "authorId",
            "author_id",
            "ownerId",
            "owner_id",
            "createdBy",
            "updatedBy",
            "username",
            "userName",
            "phone",
            "mobile",
            "email",
            "password",
            "role",
            "sessionVersion",
            "ipAddress",
            "allowedLoginFingerprintHash");

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicResponseDtoClasses")
    void publicCommunityContentSpotResponsesDoNotSerializeSensitiveUserFields(Class<?> dtoType) {
        List<String> propertyNames = serializablePropertyNames(dtoType);

        assertThat(propertyNames)
                .describedAs("%s serializes public response properties %s", dtoType.getName(), propertyNames)
                .doesNotContainAnyElementsOf(FORBIDDEN_PUBLIC_SENSITIVE_PROPERTIES);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicResponseDtoClasses")
    void publicCommunityContentSpotResponsesDoNotSerializeDomainEntityFields(Class<?> dtoType) {
        List<String> domainEntityProperties = serializableProperties(dtoType).stream()
                .filter(PublicResponseDtoPrivacyContractTest::serializesDomainEntityType)
                .map(PublicResponseDtoPrivacyContractTest::propertyTypeLabel)
                .toList();

        assertThat(domainEntityProperties)
                .describedAs("%s must map domain entities into public response DTOs", dtoType.getName())
                .isEmpty();
    }

    @Test
    void publicUserResponseDoesNotSerializeStableInternalId() {
        assertThat(serializablePropertyNames(PublicUserResponse.class))
                .contains("nickname", "avatar", "owner")
                .doesNotContain("id", "username", "role", "phone", "sessionVersion");
    }

    @Test
    void internalDiagnosticDtosAreExcludedFromPublicResponseContract() {
        assertThat(isInternalDiagnosticExclusion(RecommendationDebugResponse.class)).isTrue();
        assertThat(isInternalDiagnosticExclusion(RecommendationDebugResponse.SimilarUserEntry.class)).isTrue();
        assertThat(isInternalDiagnosticExclusion(RecommendationEvaluationResponse.UserEvaluationEntry.class)).isTrue();
        assertThat(isInternalDiagnosticExclusion(SecurityPostureResponse.class)).isTrue();
    }

    static Stream<Class<?>> publicResponseDtoClasses() {
        return PUBLIC_RESPONSE_PACKAGES.stream()
                .flatMap(packageName -> classesInPackage(packageName).stream())
                .filter(PublicResponseDtoPrivacyContractTest::isPublicResponseDto)
                .sorted(Comparator.comparing(Class::getName));
    }

    private static boolean isPublicResponseDto(Class<?> type) {
        String simpleName = type.getSimpleName();
        return !type.isSynthetic()
                && !type.isAnonymousClass()
                && !type.isMemberClass()
                && !simpleName.endsWith("Request")
                && !simpleName.endsWith("UpdateRequest")
                && !REQUEST_LIKE_DTO_TYPES.contains(type.getName())
                && (simpleName.endsWith("DTO") || simpleName.endsWith("Response"));
    }

    private static boolean isInternalDiagnosticExclusion(Class<?> type) {
        return INTERNAL_DIAGNOSTIC_EXCLUDED_PREFIXES.stream()
                .anyMatch(prefix -> type.getName().startsWith(prefix));
    }

    private static List<String> serializablePropertyNames(Class<?> type) {
        return serializableProperties(type).stream()
                .map(BeanPropertyDefinition::getName)
                .toList();
    }

    private static List<BeanPropertyDefinition> serializableProperties(Class<?> type) {
        return OBJECT_MAPPER.getSerializationConfig()
                .introspect(OBJECT_MAPPER.constructType(type))
                .findProperties()
                .stream()
                .toList();
    }

    private static boolean serializesDomainEntityType(BeanPropertyDefinition property) {
        if (property.getPrimaryMember() == null) {
            return false;
        }
        Class<?> rawType = property.getPrimaryMember().getRawType();
        return rawType != null
                && !rawType.isEnum()
                && rawType.getName().contains(".domain.");
    }

    private static String propertyTypeLabel(BeanPropertyDefinition property) {
        Class<?> rawType = property.getPrimaryMember().getRawType();
        return property.getName() + ":" + rawType.getName();
    }

    private static List<Class<?>> classesInPackage(String packageName) {
        String resourcePath = packageName.replace('.', '/');
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);

        assertThat(resource)
                .describedAs("Compiled classes resource for %s", packageName)
                .isNotNull();

        try {
            Path packageDirectory = Path.of(resource.toURI());
            try (Stream<Path> paths = Files.walk(packageDirectory)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(path -> classNameFor(packageName, packageDirectory, path))
                        .filter(className -> !className.contains("$"))
                        .<Class<?>>map(PublicResponseDtoPrivacyContractTest::loadClass)
                        .toList();
            }
        } catch (IOException | URISyntaxException e) {
            throw new AssertionError("Failed to scan compiled DTO classes in " + packageName, e);
        }
    }

    private static String classNameFor(String packageName, Path packageDirectory, Path classFile) {
        String relativeClassName = packageDirectory.relativize(classFile).toString()
                .replace('\\', '.')
                .replace('/', '.')
                .replaceAll("\\.class$", "");
        return packageName + "." + relativeClassName;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Failed to load DTO class " + className, e);
        }
    }
}
