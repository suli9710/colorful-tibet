package com.tibet.tourism.modules.ai.application;

import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.infra.AiRouteRecordRepository;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiRouteRecordService {

    private static final Logger log = LoggerFactory.getLogger(AiRouteRecordService.class);
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int ERROR_MAX_LENGTH = 500;
    private static final String STALE_RUNNING_ERROR =
            "AI route generation did not finish before the recovery window expired";
    private static final Pattern MARKDOWN_TITLE_PATTERN = Pattern.compile("(?m)^\\s*#\\s+(.+?)\\s*$");

    private final AiRouteRecordRepository routeRecordRepository;
    private final UserRepository userRepository;

    public AiRouteRecordService(AiRouteRecordRepository routeRecordRepository,
                                UserRepository userRepository) {
        this.routeRecordRepository = routeRecordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AiRouteRecord createRunningRecord(User user, String jobId, int days,
                                             String budget, String preference, String locale) {
        AiRouteRecord record = findByJob(user.getId(), jobId).orElseGet(AiRouteRecord::new);
        record.setUser(userRepository.getReferenceById(user.getId()));
        record.setJobId(blankToNull(jobId));
        record.setTitle(AiRouteRecord.defaultTitle(days));
        record.setDays(days);
        record.setBudget(normalizeKey(budget));
        record.setPreference(normalizeKey(preference));
        record.setLocale(normalizeLocale(locale));
        record.setStatus(AiRouteRecord.Status.RUNNING);
        record.setErrorMessage(null);
        if (record.getContent() == null) {
            record.setContent("");
        }
        return routeRecordRepository.save(record);
    }

    @Transactional
    public AiRouteRecord recordCompletedRoute(User user, String jobId, int days,
                                              String budget, String preference, String locale,
                                              String content) {
        AiRouteRecord record = findByJob(user.getId(), jobId).orElseGet(AiRouteRecord::new);
        record.setUser(userRepository.getReferenceById(user.getId()));
        record.setJobId(null);
        record.setTitle(extractTitle(content, days));
        record.setContent(content == null ? "" : content.trim());
        record.setDays(days);
        record.setBudget(normalizeKey(budget));
        record.setPreference(normalizeKey(preference));
        record.setLocale(normalizeLocale(locale));
        record.setStatus(AiRouteRecord.Status.COMPLETED);
        record.setErrorMessage(null);
        return routeRecordRepository.save(record);
    }

    @Transactional
    public void updateRunningContent(Long userId, String jobId, String content) {
        if (userId == null || !StringUtils.hasText(jobId) || content == null || content.isBlank()) {
            return;
        }
        findByJob(userId, jobId).ifPresent(record -> {
            if (record.getStatus() == AiRouteRecord.Status.RUNNING) {
                record.setContent(content);
                record.setTitle(extractTitle(content, record.getDays()));
                routeRecordRepository.save(record);
            }
        });
    }

    @Transactional
    public void recordFailedRoute(User user, String jobId, int days, String budget,
                                  String preference, String locale, String errorMessage) {
        AiRouteRecord record = findByJob(user.getId(), jobId).orElseGet(AiRouteRecord::new);
        record.setUser(userRepository.getReferenceById(user.getId()));
        record.setJobId(null);
        record.setTitle(hasContent(record.getContent()) ? extractTitle(record.getContent(), days) : AiRouteRecord.defaultTitle(days));
        record.setDays(days);
        record.setBudget(normalizeKey(budget));
        record.setPreference(normalizeKey(preference));
        record.setLocale(normalizeLocale(locale));
        record.setStatus(AiRouteRecord.Status.FAILED);
        record.setErrorMessage(truncate(errorMessage, ERROR_MAX_LENGTH));
        routeRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<AiRouteRecordResponse> latestFor(User user) {
        return routeRecordRepository.findFirstByUserOrderByUpdatedAtDesc(user)
                .map(AiRouteRecordResponse::fromLatest);
    }

    @Transactional(readOnly = true)
    public List<AiRouteRecordResponse> savedFor(User user) {
        return routeRecordRepository.findByUserAndManuallySavedTrueOrderByUpdatedAtDesc(user)
                .stream()
                .map(AiRouteRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AiRouteRecord> findJobRecord(Long userId, String jobId) {
        return findByJob(userId, jobId);
    }

    @Transactional
    public int failStaleRunningRecords(Duration staleTtl) {
        Duration effectiveTtl = staleTtl == null || staleTtl.isNegative() ? Duration.ZERO : staleTtl;
        LocalDateTime cutoff = LocalDateTime.now().minus(effectiveTtl);
        List<AiRouteRecord> staleRecords = routeRecordRepository
                .findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(AiRouteRecord.Status.RUNNING, cutoff);
        if (staleRecords.isEmpty()) {
            return 0;
        }

        for (AiRouteRecord record : staleRecords) {
            record.setJobId(null);
            record.setStatus(AiRouteRecord.Status.FAILED);
            record.setErrorMessage(STALE_RUNNING_ERROR);
            record.setTitle(hasContent(record.getContent())
                    ? extractTitle(record.getContent(), record.getDays())
                    : AiRouteRecord.defaultTitle(record.getDays()));
        }
        routeRecordRepository.saveAll(staleRecords);
        log.warn("Marked {} stale AI route RUNNING records as FAILED during startup recovery; cutoff={}",
                staleRecords.size(), cutoff);
        return staleRecords.size();
    }

    @Transactional
    public AiRouteRecordResponse saveForUser(Long recordId, User user) {
        AiRouteRecord record = routeRecordRepository.findByIdAndUser(recordId, user)
                .orElseThrow(() -> new ResourceNotFoundException("AI route record not found"));
        if (!hasContent(record.getContent())) {
            throw new BusinessException("AI route content is empty");
        }
        record.setManuallySaved(true);
        return AiRouteRecordResponse.from(routeRecordRepository.save(record));
    }

    private Optional<AiRouteRecord> findByJob(Long userId, String jobId) {
        if (userId == null || !StringUtils.hasText(jobId)) {
            return Optional.empty();
        }
        return routeRecordRepository.findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(userId, jobId);
    }

    private static String extractTitle(String content, Integer days) {
        if (StringUtils.hasText(content)) {
            Matcher matcher = MARKDOWN_TITLE_PATTERN.matcher(content);
            if (matcher.find()) {
                String title = matcher.group(1).replaceAll("\\s+", " ").trim();
                if (StringUtils.hasText(title)) {
                    return truncate(title, TITLE_MAX_LENGTH);
                }
            }
        }
        return AiRouteRecord.defaultTitle(days);
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String normalizeLocale(String value) {
        return value == null || value.isBlank() ? "zh" : value.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static boolean hasContent(String value) {
        return value != null && !value.isBlank();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
