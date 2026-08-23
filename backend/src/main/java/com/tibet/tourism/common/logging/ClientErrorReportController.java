package com.tibet.tourism.common.logging;

import com.tibet.tourism.common.security.PiiMasker;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-errors")
public class ClientErrorReportController {

    private static final Logger logger = LoggerFactory.getLogger(ClientErrorReportController.class);

    @PostMapping
    public ResponseEntity<Void> report(@Valid @RequestBody ClientErrorReportRequest report) {
        // Browser input is untrusted even after client-side redaction. Keep diagnostic labels, but
        // hash free-form fields so an attacker cannot inject secrets, PII, or forged log lines.
        logger.error(
                "frontend_error source={} name={} path={} release={} occurredAt={} "
                        + "messageHash={} stackHash={} infoHash={} userAgentHash={}",
                report.source(),
                report.name(),
                report.path(),
                report.release(),
                report.timestamp(),
                PiiMasker.shortHash(report.message()),
                PiiMasker.shortHash(report.stack()),
                PiiMasker.shortHash(report.info()),
                PiiMasker.shortHash(report.userAgent()));
        return ResponseEntity.accepted().build();
    }
}
