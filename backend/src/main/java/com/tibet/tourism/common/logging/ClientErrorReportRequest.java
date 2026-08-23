package com.tibet.tourism.common.logging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ClientErrorReportRequest(
        @NotBlank
        @Pattern(regexp = "^(vue|window\\.error|unhandledrejection)$")
        String source,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9.$_-]{1,120}$")
        String name,
        @NotBlank
        @Size(max = 1000)
        String message,
        @Size(max = 4000)
        String stack,
        @Size(max = 500)
        String info,
        @NotBlank
        @Pattern(regexp = "^/[A-Za-z0-9._~!$&'()*+,;=:@%/\\-]{0,239}$")
        String path,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,100}$")
        String release,
        @Size(max = 500)
        String userAgent,
        @NotNull
        Instant timestamp) {
}
