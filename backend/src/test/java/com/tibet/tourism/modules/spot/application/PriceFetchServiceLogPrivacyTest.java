package com.tibet.tourism.modules.spot.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PriceFetchServiceLogPrivacyTest {

    private static final Pattern RAW_THROWABLE_LOG_ARGUMENT = Pattern.compile(
            "\\blogger\\s*\\.\\s*(?:debug|warn|error)\\s*\\([^;]*,\\s*(?:e|ex|exception|throwable|error)\\s*\\)");

    @Test
    void externalContentSummaryDoesNotContainRawContent() {
        String rawContent = "{\"basePrice\":\"not-a-number\",\"token\":\"secret-value\"}";

        String summary = PriceFetchService.summarizeExternalContent(rawContent);

        assertThat(summary).contains("length=", "sha256=");
        assertThat(summary).doesNotContain("secret-value");
        assertThat(summary).doesNotContain("not-a-number");
    }

    @Test
    void aiPriceResponseLoggingUsesSummaryInsteadOfRawResponse() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/tibet/tourism/modules/spot/application/PriceFetchService.java"));

        assertThat(source).doesNotContain("Raw AI price response");
        assertThat(source).doesNotContain("logger.debug(\"{}\", response)");
        assertThat(source).contains("summarizeExternalContent(response)");
    }

    @Test
    void crawlerSupplierAndAiFailuresDoNotLogRawThrowableArguments() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/tibet/tourism/modules/spot/application/PriceFetchService.java"));

        assertThat(RAW_THROWABLE_LOG_ARGUMENT.matcher(source).results().toList())
                .describedAs("Price fetch failures must log sanitized exception summaries, not raw throwable arguments")
                .isEmpty();
        assertThat(source).contains("SensitiveLogSanitizer.exceptionSummary(e)");
    }
}
