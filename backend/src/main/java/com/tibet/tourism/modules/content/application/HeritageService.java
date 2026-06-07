package com.tibet.tourism.modules.content.application;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.infra.HeritageEventRepository;
import com.tibet.tourism.modules.content.infra.HeritageInheritorRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeritageService {

    @Autowired
    private HeritageItemRepository heritageItemRepository;

    @Autowired
    private HeritageInheritorRepository inheritorRepository;

    @Autowired
    private HeritageEventRepository eventRepository;

    public List<HeritageItem> getAllItems() {
        return heritageItemRepository.findAll(PageRequest.of(0, 500, Sort.by("id"))).getContent();
    }

    public Page<HeritageItem> getAllItems(Pageable pageable) {
        return heritageItemRepository.findAll(pageable);
    }

    public Optional<HeritageItem> getItemById(Long id) {
        return heritageItemRepository.findById(id);
    }

    @Transactional
    public Optional<HeritageItem> getItemByIdAndIncrementView(Long id) {
        Optional<HeritageItem> opt = heritageItemRepository.findById(id);
        opt.ifPresent(item -> heritageItemRepository.incrementViewCount(item.getId()));
        return opt;
    }

    public Page<HeritageItem> getItemsByCategory(String category, Pageable pageable) {
        return heritageItemRepository.findByCategory(category, pageable);
    }

    public Page<HeritageItem> searchItems(String keyword, Pageable pageable) {
        return heritageItemRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword, pageable);
    }

    public Page<HeritageItem> searchItemsByCategory(String category, String keyword, Pageable pageable) {
        return heritageItemRepository.searchByCategoryAndKeyword(category, keyword, pageable);
    }

    public List<HeritageInheritor> getInheritorsByItemId(Long itemId) {
        return inheritorRepository.findByHeritageItemId(itemId);
    }

    public List<HeritageEvent> getEventsByItemId(Long itemId) {
        return eventRepository.findByHeritageItemId(itemId);
    }

    public Page<HeritageEvent> getUpcomingEvents(Pageable pageable) {
        return eventRepository.findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate.now(), pageable);
    }
}
