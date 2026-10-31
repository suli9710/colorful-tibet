package com.tibet.tourism.modules.route.application;
import com.tibet.tourism.modules.route.domain.*;
import com.tibet.tourism.modules.hotel.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class ItineraryTrustPolicyTest {
    static IntStream sleepAltitudes() { return IntStream.rangeClosed(4480, 4520); }
    @ParameterizedTest @MethodSource("sleepAltitudes")
    void overnightNumericPolicyBoundary(int meters) {
        assertEquals(meters > 4500, ItineraryTrustPolicy.violatesOvernightPolicy("- 住宿：" + meters + "米客栈"));
    }
    @ParameterizedTest @ValueSource(strings={"住宿：珠峰大本营帐篷", "入住绒布寺招待所", "住宿：纳木错湖边客栈", "Overnight at 5200m", "hotel 5000 meters"})
    void rejectsExplicitUnacceptableSleepSuggestions(String text) { assertTrue(ItineraryTrustPolicy.violatesOvernightPolicy(text)); }
    @ParameterizedTest @ValueSource(strings={"- 上午：珠峰大本营 5200米", "- 不安排5000米住宿", "- 避免入住大本营", "", "- 住宿：拉萨市区待确认"})
    void doesNotConfuseVisitsOrWarningsWithOvernightPlans(String text) { assertFalse(ItineraryTrustPolicy.violatesOvernightPolicy(text)); }
    @Test void allDataStillRequiresExternalConfirmation() {
        var result = ItineraryTrustPolicy.evaluate(validDraft());
        assertEquals("NEEDS_CONFIRMATION", result.status());
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("SLEEP_ALTITUDE_UNKNOWN")));
        assertTrue(result.sources().stream().allMatch(s -> s.lastVerifiedAt() == null && s.status().equals("REFERENCE_ONLY")));
        assertTrue(result.checklist().stream().allMatch(c -> c.status().equals("TO_CONFIRM")));
    }
    @Test void missingCapacityIsBlocking() {
        var draft=validDraft(); draft.getItineraryDays().get(0).getItems().get(0).getRoomType().setCapacity(null);
        assertEquals("BLOCKED", ItineraryTrustPolicy.evaluate(draft).status());
    }
    @Test void missingHotelCannotBeValidated() {
        var draft=validDraft(); draft.getItineraryDays().get(0).getItems().get(0).setHotel(null);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("LODGING_UNRESOLVED")));
    }
    @Test void persistedPartyCapacityChecked() {
        var draft=validDraft(); draft.setTravelers(4);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("CAPACITY_UNKNOWN")));
    }
    @Test void dateMismatchIsBlocking() {
        var draft=validDraft(); draft.getItineraryDays().get(0).setTravelDate(LocalDate.now().plusDays(9));
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("DAY_DATE")));
    }
    @Test void legacyPartyUnknownIsBlocking() {
        var draft=validDraft(); draft.setTravelers(null);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("TRAVELERS_UNKNOWN")));
    }
    @Test void overnightNotAllowedAbovePolicy() {
        var draft=validDraft(); draft.getItineraryDays().get(0).getItems().get(0).setAltitudeMeters(4700);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("SLEEP_ALTITUDE_POLICY")));
    }
    @Test void overlappingItemsBlocked() {
        var draft=validDraft(); var day=draft.getItineraryDays().get(0);
        var item=new ItineraryItem(); item.setItemType(ItineraryItem.ItemType.MEAL);item.setTitle("meal");item.setStartTime("21:00");item.setDurationMinutes(60);day.addItem(item);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("TIME_OVERLAP")));
    }

    @Test void outOfRangeDayNumberBlocked() {
        var draft=validDraft(); draft.getItineraryDays().get(0).setDayNumber(2);draft.getItineraryDays().get(0).setTravelDate(draft.getStartDate().plusDays(1));
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("DAY_DATE")));
    }
    @ParameterizedTest @ValueSource(ints={-1,0,21,999})
    void invalidPersistedTravelersBlocked(int travelers) {
        var draft=validDraft();draft.setTravelers(travelers);
        assertEquals("BLOCKED",ItineraryTrustPolicy.evaluate(draft).status());
    }
    @Test void nonLodgingMidnightCrossingBlocked() {
        var draft=validDraft();var item=draft.getItineraryDays().get(0).getItems().get(0);item.setItemType(ItineraryItem.ItemType.TRANSPORT);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("CROSSES_MIDNIGHT")));
    }
    @Test void staleProvenanceNotSilentlyUpgraded() {
        var draft=validDraft();draft.setGenerationRuleVersion("old");
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("RULE_VERSION_CHANGED")));
        assertEquals("old",draft.getGenerationRuleVersion());
    }
    @Test void cautionInSameSuggestionDoesNotBypassSleepRule() {
        assertTrue(ItineraryTrustPolicy.violatesOvernightPolicy("住宿5200米营地，避免高反"));
    }


    @Test void presentationOrderDoesNotCreateOverlap() {
        var draft=validDraft();var day=draft.getItineraryDays().get(0);
        var item=new ItineraryItem();item.setItemType(ItineraryItem.ItemType.MEAL);item.setTitle("earlier meal");item.setStartTime("18:00");item.setDurationMinutes(60);day.addItem(item);
        assertFalse(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("TIME_OVERLAP")));
    }
    @Test void overnightCanConflictWithFollowingMorning() {
        var draft=validDraft();draft.setDays(2);
        var next=new ItineraryDay();next.setDayNumber(2);next.setTravelDate(draft.getStartDate().plusDays(1));
        var item=new ItineraryItem();item.setItemType(ItineraryItem.ItemType.TRANSPORT);item.setTitle("early transport");item.setStartTime("07:00");item.setDurationMinutes(60);next.addItem(item);draft.addDay(next);
        assertTrue(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("TIME_OVERLAP")));
    }
    @Test void adjacentIntervalsDoNotOverlap() {
        var draft=validDraft();var day=draft.getItineraryDays().get(0);
        var item=new ItineraryItem();item.setItemType(ItineraryItem.ItemType.MEAL);item.setTitle("meal");item.setStartTime("19:30");item.setDurationMinutes(60);day.addItem(item);
        assertFalse(ItineraryTrustPolicy.evaluate(draft).issues().stream().anyMatch(i -> i.code().equals("TIME_OVERLAP")));
    }

    private Itinerary validDraft() {
        var it=new Itinerary();it.setDays(1);it.setTravelers(2);it.setStartDate(LocalDate.now().plusDays(1));
        var day=new ItineraryDay();day.setDayNumber(1);day.setTravelDate(it.getStartDate());
        var hotel=new Hotel();hotel.setId(1L);var room=new RoomType();room.setCapacity(2);room.setInventoryUnits(2);room.setHotel(hotel);
        var item=new ItineraryItem();item.setItemType(ItineraryItem.ItemType.HOTEL);item.setHotel(hotel);item.setRoomType(room);item.setStartTime("20:30");item.setDurationMinutes(720);item.setEstimatedCost(BigDecimal.TEN);day.addItem(item);it.addDay(day);return it;
    }
}
