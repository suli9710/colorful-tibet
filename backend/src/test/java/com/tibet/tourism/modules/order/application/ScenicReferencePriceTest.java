package com.tibet.tourism.modules.order.application;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ScenicReferencePriceTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 11, 12})
    void offSeasonUsesConfiguredOrBasePrice(int month) {
        ScenicSpot spot = new ScenicSpot();
        spot.setTicketPrice(BigDecimal.TEN);
        LocalDate date = LocalDate.of(2030, month, 15);
        assertEquals(BigDecimal.TEN, ScenicReferencePrice.resolve(spot, date));
        spot.setOffSeasonPrice(BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, ScenicReferencePrice.resolve(spot, date));
    }

    @Test
    void explicitZeroIsAllowedButMissingOrNegativeIsNot() {
        ScenicSpot spot = new ScenicSpot();
        LocalDate date = LocalDate.of(2030, 1, 15);
        assertThrows(IllegalStateException.class, () -> ScenicReferencePrice.resolve(spot, date));
        spot.setTicketPrice(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, ScenicReferencePrice.resolve(spot, date));
        spot.setOffSeasonPrice(BigDecimal.valueOf(-1));
        assertThrows(IllegalStateException.class, () -> ScenicReferencePrice.resolve(spot, date));
        spot.setOffSeasonPrice(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, ScenicReferencePrice.resolve(spot, date));
    }

    @Test
    void peakBoundariesAndMissingDateAreExplicit() {
        ScenicSpot spot = new ScenicSpot();
        spot.setTicketPrice(BigDecimal.ONE);
        spot.setPeakSeasonPrice(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, ScenicReferencePrice.resolve(spot, LocalDate.of(2030, 5, 1)));
        assertEquals(BigDecimal.TEN, ScenicReferencePrice.resolve(spot, LocalDate.of(2030, 10, 31)));
        assertEquals(BigDecimal.ONE, ScenicReferencePrice.resolve(spot, LocalDate.of(2030, 11, 1)));
        assertThrows(IllegalArgumentException.class, () -> ScenicReferencePrice.resolve(spot, null));
    }
}
