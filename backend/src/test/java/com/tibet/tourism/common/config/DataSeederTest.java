package com.tibet.tourism.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private ScenicSpotRepository spotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVisitHistoryRepository historyRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void seedHistoryCapsGeneratedRowsToUniqueUserSpotPairs() {
        List<ScenicSpot> spots = List.of(spot(10L), spot(11L));
        List<User> users = List.of(user(1L, "拉萨"), user(2L, "北京"));
        when(spotRepository.findAllWithoutTags(any(Pageable.class))).thenReturn(new PageImpl<>(spots));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(users));
        when(historyRepository.count()).thenReturn(0L, 4L);

        ReflectionTestUtils.invokeMethod(dataSeeder, "seedHistory");

        ArgumentCaptor<Iterable<UserVisitHistory>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(historyRepository).saveAll(captor.capture());
        List<UserVisitHistory> saved = StreamSupport
                .stream(captor.getValue().spliterator(), false)
                .toList();

        assertThat(saved).hasSize(4);
        assertThat(saved)
                .extracting(history -> history.getUser().getId() + ":" + history.getSpot().getId())
                .doesNotHaveDuplicates();
    }

    private static ScenicSpot spot(Long id) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        return spot;
    }

    private static User user(Long id, String city) {
        User user = new User();
        user.setId(id);
        user.setCity(city);
        return user;
    }
}
