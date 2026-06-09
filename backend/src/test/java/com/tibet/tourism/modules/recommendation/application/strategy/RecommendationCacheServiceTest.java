package com.tibet.tourism.modules.recommendation.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.CacheKeyHasher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RecommendationCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RecommendationCacheService(
                redisTemplate,
                new ObjectMapper(),
                new CacheKeyHasher("test-cache-key-hmac-secret"));
    }

    @Test
    void tagProfileCacheHitAndInvalidateUseSameHmacUserKey() {
        Long userId = 1234567890123456789L;
        String rawUserId = userId.toString();
        Map<String, String> redisStore = new HashMap<>();
        Map<String, Double> tagProfile = Map.of("temple", 0.9, "lake", 0.4);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES));
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> redisStore.remove(invocation.getArgument(0)) != null);

        cacheService.cacheTagProfile(userId, tagProfile);
        assertThat(cacheService.getTagProfile(userId)).containsExactlyInAnyOrderEntriesOf(tagProfile);
        assertLocalCacheKeysDoNotExposeRawUserIds("localTagProfileCache", rawUserId);

        cacheService.invalidateUserCache(userId);
        assertThat(cacheService.getTagProfile(userId)).isNull();

        ArgumentCaptor<String> setKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> getKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(setKeyCaptor.capture(), anyString(), eq(60L), eq(TimeUnit.MINUTES));
        verify(valueOperations, org.mockito.Mockito.times(2)).get(getKeyCaptor.capture());
        verify(redisTemplate, org.mockito.Mockito.times(2)).delete(deleteKeyCaptor.capture());

        String tagProfileKey = setKeyCaptor.getValue();
        assertThat(tagProfileKey)
                .startsWith("recommend:tagprofile:user#")
                .doesNotContain(rawUserId);
        assertThat(getKeyCaptor.getAllValues()).containsOnly(tagProfileKey);
        assertThat(deleteKeyCaptor.getAllValues()).allSatisfy(key -> assertThat(key).doesNotContain(rawUserId));
        assertThat(deleteKeyCaptor.getAllValues()).contains(tagProfileKey);
    }

    @Test
    void differentUserIdsProduceDifferentRedisKeysWithoutRawUserIds() {
        Long firstUserId = 1234567890123456789L;
        Long secondUserId = 987654321987654321L;
        Long similarUserId = 100987654321L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.cacheSimilarity(firstUserId, Map.of(similarUserId, 0.8));
        cacheService.cacheSimilarity(secondUserId, Map.of(similarUserId, 0.8));
        assertLocalCacheKeysDoNotExposeRawUserIds(
                "localSimilarityCache",
                firstUserId.toString(),
                secondUserId.toString());
        assertLocalSimilarityValuesDoNotExposeRawUserIds(
                firstUserId.toString(),
                secondUserId.toString(),
                similarUserId.toString());

        ArgumentCaptor<String> setKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> setValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, org.mockito.Mockito.times(2))
                .set(setKeyCaptor.capture(), setValueCaptor.capture(), eq(30L), eq(TimeUnit.MINUTES));

        List<String> keys = setKeyCaptor.getAllValues();
        assertThat(keys).hasSize(2);
        assertThat(keys).allSatisfy(key -> assertThat(key)
                .startsWith("recommend:similarity:user#")
                .doesNotContain(firstUserId.toString())
                .doesNotContain(secondUserId.toString())
                .doesNotContain(similarUserId.toString()));
        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));

        assertThat(setValueCaptor.getAllValues()).allSatisfy(payload -> assertThat(payload)
                .contains("similar-user#")
                .doesNotContain(firstUserId.toString())
                .doesNotContain(secondUserId.toString())
                .doesNotContain(similarUserId.toString()));
        assertThat(cacheService.getSimilarity(firstUserId))
                .containsEntry(cacheService.similarUserCacheKey(similarUserId), 0.8);
    }

    @SuppressWarnings("unchecked")
    private void assertLocalCacheKeysDoNotExposeRawUserIds(String fieldName, String... rawUserIds) {
        Map<String, ?> localCache = (Map<String, ?>) ReflectionTestUtils.getField(cacheService, fieldName);

        assertThat(localCache).isNotNull();
        assertThat(localCache.keySet()).allSatisfy(key -> {
            assertThat(key).startsWith("user#");
            for (String rawUserId : rawUserIds) {
                assertThat(key).doesNotContain(rawUserId);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void assertLocalSimilarityValuesDoNotExposeRawUserIds(String... rawUserIds) {
        Map<String, Map<String, Double>> localCache =
                (Map<String, Map<String, Double>>) ReflectionTestUtils.getField(cacheService, "localSimilarityCache");

        assertThat(localCache).isNotNull();
        assertThat(localCache.values()).allSatisfy(similarities ->
                assertThat(similarities.keySet()).allSatisfy(key -> {
                    assertThat(key).startsWith("similar-user#");
                    for (String rawUserId : rawUserIds) {
                        assertThat(key).doesNotContain(rawUserId);
                    }
                }));
    }
}
