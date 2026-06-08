package com.tibet.tourism.modules.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class HeritageServicePaginationTest {

    @Mock
    private HeritageItemRepository heritageItemRepository;

    @Mock
    private HeritageInheritorRepository inheritorRepository;

    @Mock
    private HeritageEventRepository eventRepository;

    @InjectMocks
    private HeritageService heritageService;

    @Test
    void getInheritorsByItemIdUsesPageableRepositoryQuery() {
        Pageable pageable = PageRequest.of(1, 20, Sort.by("id"));
        HeritageInheritor inheritor = new HeritageInheritor();
        when(inheritorRepository.findByHeritageItemId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(inheritor), pageable, 3));

        Page<HeritageInheritor> result = heritageService.getInheritorsByItemId(7L, pageable);

        assertThat(result.getContent()).containsExactly(inheritor);
        assertThat(result.getPageable()).isEqualTo(pageable);
        verify(inheritorRepository).findByHeritageItemId(7L, pageable);
        verify(inheritorRepository, never()).findByHeritageItemId(7L);
    }

    @Test
    void getEventsByItemIdUsesPageableRepositoryQuery() {
        Pageable pageable = PageRequest.of(2, 50, Sort.by("eventDate").ascending().and(Sort.by("id")));
        HeritageEvent event = new HeritageEvent();
        when(eventRepository.findByHeritageItemId(9L, pageable))
                .thenReturn(new PageImpl<>(List.of(event), pageable, 4));

        Page<HeritageEvent> result = heritageService.getEventsByItemId(9L, pageable);

        assertThat(result.getContent()).containsExactly(event);
        assertThat(result.getPageable()).isEqualTo(pageable);
        verify(eventRepository).findByHeritageItemId(9L, pageable);
        verify(eventRepository, never()).findByHeritageItemId(9L);
    }
}
