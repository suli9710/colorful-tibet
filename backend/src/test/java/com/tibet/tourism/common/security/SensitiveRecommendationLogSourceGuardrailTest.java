package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SensitiveRecommendationLogSourceGuardrailTest {

    private static final List<String> SENSITIVE_SOURCE_PATHS = List.of(
            "com/tibet/tourism/modules/ai/application",
            "com/tibet/tourism/modules/ai/web",
            "com/tibet/tourism/modules/recommendation/application",
            "com/tibet/tourism/modules/spot/application/PriceFetchService.java",
            "com/tibet/tourism/modules/spot/application/CompanionInferenceService.java");

    private static final Pattern LOGGER_CALL = Pattern.compile(
            "\\b(?:logger|log)\\s*\\.\\s*(?:trace|debug|info|warn|error)\\s*\\(");
    private static final Pattern AUTHORIZATION_OR_BEARER = Pattern.compile(
            "(?i)\\b(?:authorization|bearer)\\b");
    private static final Pattern RAW_PAYLOAD_FIELD_TEMPLATE = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])(?:request|response|body|requestBody|responseBody|errorBody)"
                    + "(?![A-Za-z0-9_])\\s*=");
    private static final Pattern RAW_USER_ID_TEMPLATE = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])userId(?![A-Za-z0-9_])\\s*[:=]");
    private static final Pattern RAW_USER_ID_REFERENCE = Pattern.compile(
            "(?<![A-Za-z0-9_])userId(?![A-Za-z0-9_])");
    private static final Pattern SAFE_USER_REF_CALL = Pattern.compile(
            "\\b(?:AiLogPrivacy|RecommendationLogPrivacy)\\s*\\.\\s*userRef\\s*\\([^)]*\\)");
    private static final Pattern GET_MESSAGE_CALL = Pattern.compile("\\.\\s*getMessage\\s*\\(");
    private static final Pattern HIGH_RISK_PAYLOAD_REFERENCE = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])(?:request|response|body|requestBody|responseBody|errorBody)"
                    + "(?![A-Za-z0-9_])");
    private static final Pattern RAW_PAYLOAD_ARGUMENT = Pattern.compile(
            "(?i)^\\s*(?:request|response|body|requestBody|responseBody|errorBody)\\s*$");
    private static final Pattern RAW_PAYLOAD_BODY_ACCESS = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])(?:request|response|body|requestBody|responseBody|errorBody)"
                    + "(?![A-Za-z0-9_])\\s*\\.\\s*"
                    + "(?:body|getBody|getInputStream|getReader|getContentAsString|toString)\\s*\\(");
    private static final Pattern SAFE_PAYLOAD_FUNCTION_CALL = Pattern.compile(
            "(?:textLength|redactForLog|safeErrorSummary|exceptionSummary|summarizeExternalContent"
                    + "|PiiMasker\\s*\\.\\s*shortHash)"
                    + "\\s*\\([^()]*\\)");
    private static final Pattern RAW_THROWABLE_ARGUMENT = Pattern.compile(
            "^\\s*(?:e|ex|exception|throwable|error)\\s*$");
    private static final Pattern DEBUG_WARN_ERROR_LOGGER_CALL = Pattern.compile(
            "\\b(?:logger|log)\\s*\\.\\s*(?:debug|warn|error)\\s*\\(");
    private static final Pattern SAFE_PAYLOAD_METHOD_CALL = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])(?:request|response|body|requestBody|responseBody|errorBody)"
                    + "(?![A-Za-z0-9_])\\s*\\.\\s*(?:keySet|size|length)\\s*\\(\\s*\\)");
    private static final Pattern NON_ASCII_LOG_TEMPLATE = Pattern.compile("[^\\x09\\x0A\\x0D\\x20-\\x7E]");

    @Test
    void sensitiveRecommendationLoggerCallsDoNotLogRawSensitiveValues() throws IOException {
        Path backendRoot = backendRoot();
        List<Violation> violations = sensitiveSourceFiles(backendRoot).stream()
                .flatMap(sourceFile -> loggerCalls(sourceFile, displayPath(backendRoot, sourceFile)).stream())
                .flatMap(loggerCall -> violationsFor(loggerCall).stream())
                .toList();

        assertThat(violations)
                .describedAs("Sensitive recommendation log guardrail violations:%n%s", formatViolations(violations))
                .isEmpty();
    }

    @Test
    void recommendationLoggerTemplatesUseStableAsciiText() throws IOException {
        Path backendRoot = backendRoot();
        Path recommendationRoot = backendRoot.resolve("src/main/java/com/tibet/tourism/modules/recommendation/application");
        List<Violation> violations = javaSourceFiles(recommendationRoot).stream()
                .flatMap(sourceFile -> loggerCalls(sourceFile, displayPath(backendRoot, sourceFile)).stream())
                .flatMap(loggerCall -> recommendationLogTextViolations(loggerCall).stream())
                .toList();

        assertThat(violations)
                .describedAs("Recommendation log text must avoid mojibake-prone text and emoji:%n%s",
                        formatViolations(violations))
                .isEmpty();
    }

    @Test
    void scannerIgnoresNonLoggerSensitiveCodeAndAllowsSafeSummaries() {
        String source = """
                class Example {
                    String getMessage(Throwable e) {
                        return e.getMessage();
                    }

                    void callApi(Long userId, Object requestBody, java.util.Map<?, ?> response) {
                        webClient.post()
                                .header("Authorization", "Bearer token")
                                .bodyValue(requestBody);
                        log.info("AI request prepared: user={}, bodyLength={}, bodyHash={}",
                                AiLogPrivacy.userRef(userId),
                                textLength(requestBody),
                                PiiMasker.shortHash(redactForLog(requestBody)));
                        log.info("AI response received, top-level keys={}", response.keySet());
                    }
                }
                """;

        List<Violation> violations = loggerCalls(source, "Example.java").stream()
                .flatMap(loggerCall -> violationsFor(loggerCall).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void scannerFlagsRawSensitiveLoggerArguments() {
        String source = """
                class Example {
                    void leak(Long userId, RuntimeException error, Object requestBody, String token) {
                        logger.warn("route failed: userId={}", userId);
                        logger.warn("route failed: {}", error.getMessage());
                        logger.debug("route failed", error);
                        logger.info("AI upstream body={}", requestBody);
                        logger.info("Authorization header present: {}", token);
                    }
                }
                """;

        List<String> rules = loggerCalls(source, "Example.java").stream()
                .flatMap(loggerCall -> violationsFor(loggerCall).stream())
                .map(Violation::rule)
                .toList();

        assertThat(rules)
                .contains(
                        "raw userId log field",
                        "raw userId log argument",
                        "direct exception getMessage() log argument",
                        "raw exception throwable log argument",
                        "raw request/response/body log field",
                        "Authorization/Bearer log text");
    }

    @Test
    void scannerFlagsNonAsciiRecommendationLogText() {
        String source = """
                class Example {
                    void logMatrix() {
                        logger.info("Item matrix ✅ 预计算完成");
                    }
                }
                """;

        List<String> rules = loggerCalls(source, "Example.java").stream()
                .flatMap(loggerCall -> recommendationLogTextViolations(loggerCall).stream())
                .map(Violation::rule)
                .toList();

        assertThat(rules).contains("non-ASCII recommendation log text");
    }

    private static List<Violation> violationsFor(LoggerCall loggerCall) {
        List<Violation> violations = new ArrayList<>();
        if (loggerCall.arguments().isEmpty()) {
            return violations;
        }

        String templateText = String.join(" ", stringLiterals(loggerCall.arguments().get(0)));
        if (AUTHORIZATION_OR_BEARER.matcher(templateText).find()) {
            violations.add(loggerCall.violation("Authorization/Bearer log text"));
        }
        if (RAW_PAYLOAD_FIELD_TEMPLATE.matcher(templateText).find()) {
            violations.add(loggerCall.violation("raw request/response/body log field"));
        }
        if (RAW_USER_ID_TEMPLATE.matcher(templateText).find()) {
            violations.add(loggerCall.violation("raw userId log field"));
        }

        for (String argument : loggerCall.arguments()) {
            if (GET_MESSAGE_CALL.matcher(withoutStringLiterals(argument)).find()) {
                violations.add(loggerCall.violation("direct exception getMessage() log argument"));
            }
        }

        if (DEBUG_WARN_ERROR_LOGGER_CALL.matcher(loggerCall.expression()).find()) {
            for (int index = 1; index < loggerCall.arguments().size(); index++) {
                String argument = loggerCall.arguments().get(index);
                if (RAW_THROWABLE_ARGUMENT.matcher(withoutStringLiterals(argument)).matches()) {
                    violations.add(loggerCall.violation("raw exception throwable log argument"));
                }
            }
        }

        for (int index = 1; index < loggerCall.arguments().size(); index++) {
            String argument = loggerCall.arguments().get(index);
            if (containsUnsafeUserIdReference(argument)) {
                violations.add(loggerCall.violation("raw userId log argument"));
            }
            if (containsUnsafePayloadReference(argument)) {
                violations.add(loggerCall.violation("raw request/response/body log argument"));
            }
        }

        return violations;
    }

    private static List<Violation> recommendationLogTextViolations(LoggerCall loggerCall) {
        if (loggerCall.arguments().isEmpty()) {
            return List.of();
        }
        String templateText = String.join(" ", stringLiterals(loggerCall.arguments().get(0)));
        if (NON_ASCII_LOG_TEMPLATE.matcher(templateText).find()) {
            return List.of(loggerCall.violation("non-ASCII recommendation log text"));
        }
        return List.of();
    }

    private static boolean containsUnsafeUserIdReference(String argument) {
        String code = withoutStringLiterals(argument);
        String withoutSafeUserRefs = SAFE_USER_REF_CALL.matcher(code).replaceAll("SAFE_USER_REF");
        return RAW_USER_ID_REFERENCE.matcher(withoutSafeUserRefs).find();
    }

    private static boolean containsUnsafePayloadReference(String argument) {
        String code = withoutStringLiterals(argument);
        String withoutSafeSummaries = replaceUntilStable(code, SAFE_PAYLOAD_METHOD_CALL, "SAFE_PAYLOAD");
        withoutSafeSummaries = replaceUntilStable(withoutSafeSummaries, SAFE_PAYLOAD_FUNCTION_CALL, "SAFE_PAYLOAD");
        return HIGH_RISK_PAYLOAD_REFERENCE.matcher(withoutSafeSummaries).find()
                && (RAW_PAYLOAD_ARGUMENT.matcher(withoutSafeSummaries).find()
                || RAW_PAYLOAD_BODY_ACCESS.matcher(withoutSafeSummaries).find());
    }

    private static String replaceUntilStable(String value, Pattern pattern, String replacement) {
        String previous;
        String current = value;
        do {
            previous = current;
            current = pattern.matcher(previous).replaceAll(replacement);
        } while (!current.equals(previous));
        return current;
    }

    private static List<Path> sensitiveSourceFiles(Path backendRoot) throws IOException {
        Path sourceRoot = backendRoot.resolve("src/main/java");
        List<Path> sourceFiles = new ArrayList<>();

        for (String sensitiveSourcePath : SENSITIVE_SOURCE_PATHS) {
            Path target = sourceRoot.resolve(sensitiveSourcePath);
            assertThat(target)
                    .describedAs("Sensitive source path must exist: %s", target)
                    .exists();

            if (Files.isRegularFile(target)) {
                sourceFiles.add(target);
                continue;
            }

            try (Stream<Path> paths = Files.walk(target)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .forEach(sourceFiles::add);
            }
        }

        return sourceFiles.stream()
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList();
    }

    private static List<Path> javaSourceFiles(Path target) throws IOException {
        assertThat(target)
                .describedAs("Java source path must exist: %s", target)
                .exists();

        if (Files.isRegularFile(target)) {
            return List.of(target);
        }

        try (Stream<Path> paths = Files.walk(target)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static List<LoggerCall> loggerCalls(Path sourceFile, String displayPath) {
        try {
            return loggerCalls(Files.readString(sourceFile, StandardCharsets.UTF_8), displayPath);
        } catch (IOException e) {
            throw new AssertionError("Failed to read source file " + sourceFile, e);
        }
    }

    private static List<LoggerCall> loggerCalls(String source, String displayPath) {
        Matcher matcher = LOGGER_CALL.matcher(source);
        List<LoggerCall> calls = new ArrayList<>();

        while (matcher.find()) {
            if (!isCodePosition(source, matcher.start())) {
                continue;
            }
            int openParen = matcher.end() - 1;
            int closeParen = matchingCloseParen(source, openParen);
            String expression = source.substring(matcher.start(), closeParen + 1);
            String arguments = source.substring(openParen + 1, closeParen);
            calls.add(new LoggerCall(
                    displayPath + ":" + lineNumber(source, matcher.start()),
                    expression,
                    splitTopLevelArguments(arguments)));
        }

        return calls;
    }

    private static List<String> splitTopLevelArguments(String arguments) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        ScanState state = new ScanState();

        for (int index = 0; index < arguments.length(); index++) {
            char current = arguments.charAt(index);
            char next = charAt(arguments, index + 1);
            char nextNext = charAt(arguments, index + 2);

            int skippedTo = state.consume(arguments, index, current, next, nextNext);
            if (skippedTo != index) {
                index = skippedTo;
                continue;
            }
            if (!state.inCode()) {
                continue;
            }

            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == ',' && parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                addArgument(result, arguments.substring(start, index));
                start = index + 1;
            }
        }

        addArgument(result, arguments.substring(start));
        return result;
    }

    private static void addArgument(List<String> result, String argument) {
        String trimmed = argument.trim();
        if (!trimmed.isEmpty()) {
            result.add(trimmed);
        }
    }

    private static int matchingCloseParen(String source, int openParen) {
        int depth = 0;
        ScanState state = new ScanState();

        for (int index = openParen; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = charAt(source, index + 1);
            char nextNext = charAt(source, index + 2);

            int skippedTo = state.consume(source, index, current, next, nextNext);
            if (skippedTo != index) {
                index = skippedTo;
                continue;
            }
            if (!state.inCode()) {
                continue;
            }

            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }

        throw new AssertionError("Unclosed logger call near offset " + openParen);
    }

    private static boolean isCodePosition(String source, int position) {
        ScanState state = new ScanState();
        for (int index = 0; index < position; index++) {
            char current = source.charAt(index);
            char next = charAt(source, index + 1);
            char nextNext = charAt(source, index + 2);
            int skippedTo = state.consume(source, index, current, next, nextNext);
            if (skippedTo != index) {
                index = skippedTo;
            }
        }
        return state.inCode();
    }

    private static List<String> stringLiterals(String source) {
        List<String> literals = new ArrayList<>();
        ScanState state = new ScanState();

        for (int index = 0; index < source.length(); index++) {
            if (!state.inCode()) {
                char current = source.charAt(index);
                char next = charAt(source, index + 1);
                char nextNext = charAt(source, index + 2);
                int skippedTo = state.consume(source, index, current, next, nextNext);
                if (skippedTo != index) {
                    index = skippedTo;
                }
                continue;
            }

            char current = source.charAt(index);
            char next = charAt(source, index + 1);
            char nextNext = charAt(source, index + 2);
            if (current == '"' && next == '"' && nextNext == '"') {
                int end = source.indexOf("\"\"\"", index + 3);
                if (end < 0) {
                    break;
                }
                literals.add(source.substring(index + 3, end));
                index = end + 2;
            } else if (current == '"') {
                StringBuilder literal = new StringBuilder();
                index++;
                while (index < source.length()) {
                    char value = source.charAt(index);
                    if (value == '\\' && index + 1 < source.length()) {
                        literal.append(value).append(source.charAt(index + 1));
                        index += 2;
                    } else if (value == '"') {
                        break;
                    } else {
                        literal.append(value);
                        index++;
                    }
                }
                literals.add(literal.toString());
            } else {
                int skippedTo = state.consume(source, index, current, next, nextNext);
                if (skippedTo != index) {
                    index = skippedTo;
                }
            }
        }

        return literals;
    }

    private static String withoutStringLiterals(String source) {
        StringBuilder result = new StringBuilder(source.length());
        ScanState state = new ScanState();

        for (int index = 0; index < source.length(); index++) {
            if (!state.inCode()) {
                char current = source.charAt(index);
                char next = charAt(source, index + 1);
                char nextNext = charAt(source, index + 2);
                int skippedTo = state.consume(source, index, current, next, nextNext);
                result.append(' ');
                if (skippedTo != index) {
                    index = skippedTo;
                }
                continue;
            }

            char current = source.charAt(index);
            char next = charAt(source, index + 1);
            char nextNext = charAt(source, index + 2);
            if (current == '"' && next == '"' && nextNext == '"') {
                int end = source.indexOf("\"\"\"", index + 3);
                result.append(' ');
                index = end < 0 ? source.length() : end + 2;
            } else if (current == '"') {
                result.append(' ');
                index++;
                while (index < source.length()) {
                    char value = source.charAt(index);
                    if (value == '\\' && index + 1 < source.length()) {
                        index += 2;
                    } else if (value == '"') {
                        break;
                    } else {
                        index++;
                    }
                }
            } else if (current == '\'') {
                result.append(' ');
                index++;
                while (index < source.length()) {
                    char value = source.charAt(index);
                    if (value == '\\' && index + 1 < source.length()) {
                        index += 2;
                    } else if (value == '\'') {
                        break;
                    } else {
                        index++;
                    }
                }
            } else {
                int skippedTo = state.consume(source, index, current, next, nextNext);
                if (skippedTo != index) {
                    result.append(' ');
                    index = skippedTo;
                } else {
                    result.append(current);
                }
            }
        }

        return result.toString();
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

    private static String displayPath(Path backendRoot, Path sourceFile) {
        return backendRoot.relativize(sourceFile).toString().replace('\\', '/');
    }

    private static String formatViolations(List<Violation> violations) {
        return violations.stream()
                .map(violation -> "- " + violation.location()
                        + " [" + violation.rule() + "] " + violation.snippet())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String singleLine(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
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

    private static char charAt(String value, int index) {
        return index >= 0 && index < value.length() ? value.charAt(index) : '\0';
    }

    private record LoggerCall(String location, String expression, List<String> arguments) {
        private Violation violation(String rule) {
            return new Violation(location, rule, singleLine(expression));
        }
    }

    private record Violation(String location, String rule, String snippet) {
    }

    private static final class ScanState {
        private boolean inLineComment;
        private boolean inBlockComment;
        private boolean inString;
        private boolean inChar;
        private boolean inTextBlock;

        private boolean inCode() {
            return !inLineComment && !inBlockComment && !inString && !inChar && !inTextBlock;
        }

        private int consume(String source, int index, char current, char next, char nextNext) {
            if (inLineComment) {
                if (current == '\n') {
                    inLineComment = false;
                }
                return index;
            }
            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    return index + 1;
                }
                return index;
            }
            if (inTextBlock) {
                if (current == '"' && next == '"' && nextNext == '"') {
                    inTextBlock = false;
                    return index + 2;
                }
                return index;
            }
            if (inString) {
                if (current == '\\') {
                    return index + 1;
                }
                if (current == '"') {
                    inString = false;
                }
                return index;
            }
            if (inChar) {
                if (current == '\\') {
                    return index + 1;
                }
                if (current == '\'') {
                    inChar = false;
                }
                return index;
            }

            if (current == '/' && next == '/') {
                inLineComment = true;
                return index + 1;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                return index + 1;
            }
            if (current == '"' && next == '"' && nextNext == '"') {
                inTextBlock = true;
                return index + 2;
            }
            if (current == '"') {
                inString = true;
                return index;
            }
            if (current == '\'') {
                inChar = true;
                return index;
            }
            return index;
        }
    }
}
