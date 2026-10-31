package com.tibet.tourism.modules.hotel.application;

import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.hotel.web.dto.HotelBookingRequest;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.web.dto.CreateOrderItemRequest;
import com.tibet.tourism.modules.order.web.dto.CreateOrderRequest;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Runs only against the dedicated real MySQL context job, never substitutes H2 locking semantics. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.tibet.tourism.modules.hotel.application.HotelInventoryConcurrencyIntegrationTest$CurrentReadSqlInspector",
        // Keep the unrelated scheduled sweeper out of the controlled historical-state interleaving.
        // Its expiry behavior is exercised separately in HotelBookingServiceTest.
        "app.hotel-bookings.expiry-sweep-delay-ms=3600000",
        "app.seed.content.enabled=false",
        "app.seed.demo-users.enabled=false",
        "recommendation.item-similarity.preload-on-startup=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TESTS", matches = "true")
public class HotelInventoryConcurrencyIntegrationTest {
    @Autowired private HotelRepository hotelRepository;
    @Autowired private HotelBookingRepository hotelBookingRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HotelBookingService hotelBookingService;
    @Autowired private OrderCenterService orderCenterService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @org.junit.jupiter.api.BeforeEach
    void resetObservedSql() { CurrentReadSqlInspector.statements.remove(); }

    @org.junit.jupiter.api.AfterEach
    void releaseObservedSql() { CurrentReadSqlInspector.statements.remove(); }

    @org.junit.jupiter.api.RepeatedTest(10)
    void concurrentLegacyAndUnifiedReservationsCannotOversellTwoRooms() throws Exception {
        Fixture fixture = fixture(2);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < 4; index++) {
                final boolean legacy = index % 2 == 0;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    try {
                        if (legacy) hotelBookingService.createBooking(fixture.user(), legacyRequest(fixture));
                        else orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString());
                        return true;
                    } catch (IllegalStateException unavailable) {
                        assertTrue(unavailable.getMessage().contains("unavailable"), unavailable.getMessage());
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int successful = 0;
            for (Future<Boolean> result : results) if (result.get(45, TimeUnit.SECONDS)) successful++;
            assertEquals(2, successful, "Both channels must share the same per-night room capacity");
            for (int day = 0; day < 2; day++) {
                Long occupied = jdbc.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM inventory_locks "
                        + "WHERE product_type='HOTEL_ROOM' AND sku_id=? AND service_date=? "
                        + "AND status IN ('LOCKED','CONFIRMED')", Long.class,
                        fixture.room().getId(), fixture.date().plusDays(day));
                assertEquals(2L, occupied);
            }
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void currentReadIncludesOrderProvenanceAfterEarlierRepeatableReadSnapshot() throws Exception {
        Fixture fixture = fixture(2);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            transaction.executeWithoutResult(status -> {
                // Establish a consistent snapshot before another transaction commits its order.
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?",
                        Long.class, fixture.user().getId()));
                try {
                    pool.submit(() -> orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture),
                            UUID.randomUUID().toString())).get(20, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError("Competing reservation must commit before current-read check", failure);
                }
                // Prove this transaction still has the old snapshot, rather than changing isolation
                // or weakening the original failure to a sequential test.
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?",
                        Long.class, fixture.user().getId()));
                assertNotNull(hotelBookingService.createBooking(fixture.user(), legacyRequest(fixture)).getId());
            });
            for (int day = 0; day < 2; day++) {
                assertEquals(2L, jdbc.queryForObject("SELECT SUM(quantity) FROM inventory_locks "
                        + "WHERE product_type='HOTEL_ROOM' AND sku_id=? AND service_date=? "
                        + "AND status IN ('LOCKED','CONFIRMED')", Long.class,
                        fixture.room().getId(), fixture.date().plusDays(day)));
            }
            assertThrows(IllegalStateException.class,
                    () -> orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString()));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void fullCapacityCommittedAfterSnapshotIsRejectedAcrossBothChannels(boolean existingLegacy, boolean requestingLegacy)
            throws Exception {
        Fixture fixture = fixture(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            IllegalStateException unavailable = assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?",
                        Long.class, fixture.user().getId()));
                try {
                    pool.submit(() -> {
                        if (existingLegacy) hotelBookingService.createBooking(fixture.user(), legacyRequest(fixture));
                        else orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString());
                    }).get(20, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError("Competing reservation did not commit", failure);
                }
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?",
                        Long.class, fixture.user().getId()), "Requester must still hold its earlier consistent snapshot");
                if (requestingLegacy) hotelBookingService.createBooking(fixture.user(), legacyRequest(fixture));
                else orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString());
            }));
            assertTrue(unavailable.getMessage().contains("unavailable"), unavailable.getMessage());
            for (int day = 0; day < 2; day++) {
                assertEquals(1L, jdbc.queryForObject("SELECT SUM(quantity) FROM inventory_locks "
                        + "WHERE product_type='HOTEL_ROOM' AND sku_id=? AND service_date=? "
                        + "AND status IN ('LOCKED','CONFIRMED')", Long.class,
                        fixture.room().getId(), fixture.date().plusDays(day)));
            }
            assertTrue(CurrentReadSqlInspector.statements.get().stream().anyMatch(sql ->
                    sql.contains("select * from room_types") && sql.endsWith("for update")),
                    "Room mutex must be acquired by a single current locking SQL statement");
            assertTrue(CurrentReadSqlInspector.statements.get().stream().anyMatch(sql ->
                    sql.contains("select inventory_units from room_types") && sql.endsWith("for update")),
                    "Capacity must be scalar current data, not a cached managed entity");
            assertTrue(CurrentReadSqlInspector.statements.get().stream().anyMatch(sql ->
                    sql.contains("l.sku_id as skuid") && sql.contains("join orders") && sql.endsWith("for update")),
                    "Allocation quantity and provenance must come from the same current locking SQL statement");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void committedInventoryDecreaseOverridesPreviouslyManagedRoom() throws Exception {
        Fixture fixture = fixture(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            IllegalStateException unavailable = assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                RoomType previouslyManaged = roomTypeRepository.findById(fixture.room().getId()).orElseThrow();
                assertEquals(1, previouslyManaged.getInventoryUnits());
                try {
                    pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(other -> {
                        RoomType current = roomTypeRepository.findByIdForUpdate(fixture.room().getId()).orElseThrow();
                        current.setInventoryUnits(0);
                        roomTypeRepository.saveAndFlush(current);
                    })).get(20, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError("Operator inventory decrease must commit", failure);
                }
                assertEquals(1, previouslyManaged.getInventoryUnits(), "Persistence context still contains stale room state");
                orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString());
            }));
            assertTrue(unavailable.getMessage().contains("unavailable"), unavailable.getMessage());
            assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM inventory_locks WHERE sku_id=? AND product_type='HOTEL_ROOM'",
                    Long.class, fixture.room().getId()));
            assertEquals(0, jdbc.queryForObject("SELECT inventory_units FROM room_types WHERE id=?",
                    Integer.class, fixture.room().getId()));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void currentLegacyStatusOverridesPreviouslyManagedExpiredPendingState() throws Exception {
        Fixture fixture = fixture(1);
        HotelBooking historical = new HotelBooking();
        historical.setUser(fixture.user());
        historical.setHotel(fixture.hotel());
        historical.setRoomTypeId(fixture.room().getId());
        historical.setCheckInDate(fixture.date());
        historical.setCheckOutDate(fixture.date().plusDays(2));
        historical.setStatus(HotelBooking.Status.PENDING);
        Long legacyId = hotelBookingRepository.saveAndFlush(historical).getId();
        jdbc.update("UPDATE hotel_bookings SET created_at=? WHERE id=?",
                java.time.LocalDateTime.now().minusHours(1), legacyId);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try {
            IllegalStateException unavailable = assertThrows(IllegalStateException.class,
                    () -> transaction.executeWithoutResult(status -> {
                HotelBooking stale = hotelBookingRepository.findById(legacyId).orElseThrow();
                assertEquals(HotelBooking.Status.PENDING, stale.getStatus());
                try {
                    pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(other -> {
                        roomTypeRepository.findByIdForUpdate(fixture.room().getId()).orElseThrow();
                        // Model a historical unmirrored reservation imported/confirmed by an operator.
                        jdbc.update("UPDATE hotel_bookings SET status='CONFIRMED' WHERE id=?", legacyId);
                    })).get(20, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError("Legacy state change must commit", failure);
                }
                assertEquals(HotelBooking.Status.PENDING, stale.getStatus());
                orderCenterService.ensureHotelRoomAvailable(fixture.hotel().getId(), fixture.room().getId(),
                        fixture.date(), fixture.date().plusDays(2));
            }));
            assertTrue(unavailable.getMessage().contains("unavailable"), unavailable.getMessage());
            assertEquals("CONFIRMED", jdbc.queryForObject("SELECT status FROM hotel_bookings WHERE id=?", String.class, legacyId));
            assertTrue(CurrentReadSqlInspector.statements.get().stream().anyMatch(sql ->
                    sql.contains("hb.status as status") && sql.endsWith("for update")),
                    "Legacy occupancy state must come from a scalar current locking read");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    public static class CurrentReadSqlInspector implements org.hibernate.resource.jdbc.spi.StatementInspector {
        // Observe only the requesting test thread; competing worker SQL cannot satisfy its assertions.
        static final ThreadLocal<java.util.List<String>> statements = ThreadLocal.withInitial(java.util.ArrayList::new);
        @Override public String inspect(String sql) {
            String normalized = sql.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("room_types") || normalized.contains("inventory_locks") || normalized.contains("hotel_bookings")) statements.get().add(normalized);
            return sql;
        }
    }

    @Test
    void migratedUnknownInventoryCannotCreateEitherKindOfReservation() {
        Fixture fixture = fixture(null);
        assertThrows(IllegalStateException.class,
                () -> hotelBookingService.createBooking(fixture.user(), legacyRequest(fixture)));
        assertThrows(IllegalStateException.class,
                () -> orderCenterService.createOrder(fixture.user(), unifiedRequest(fixture), UUID.randomUUID().toString()));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM inventory_locks WHERE sku_id=? AND product_type='HOTEL_ROOM'",
                Long.class, fixture.room().getId()));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM hotel_bookings WHERE room_type_id=?",
                Long.class, fixture.room().getId()));
    }

    @Test
    void overCapacityCartRollsBackAllEarlierItemHolds() {
        Fixture fixture = fixture(2);
        CreateOrderRequest request = unifiedRequest(fixture);
        CreateOrderItemRequest first = request.getItems().get(0);
        first.setQuantity(2);
        CreateOrderItemRequest second = unifiedRequest(fixture).getItems().get(0);
        request.setItems(List.of(first, second));
        assertThrows(IllegalStateException.class,
                () -> orderCenterService.createOrder(fixture.user(), request, UUID.randomUUID().toString()));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM inventory_locks WHERE sku_id=? AND product_type='HOTEL_ROOM'",
                Long.class, fixture.room().getId()));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?", Long.class, fixture.user().getId()));
    }

    private Fixture fixture(Integer stock) {
        User user = new User();
        user.setUsername("stock-" + UUID.randomUUID());
        user.setPassword("test-only-not-a-login-hash");
        user = userRepository.saveAndFlush(user);
        Hotel hotel = new Hotel();
        hotel.setName("Inventory integration fixture " + UUID.randomUUID());
        hotel = hotelRepository.saveAndFlush(hotel);
        RoomType room = new RoomType();
        room.setHotel(hotel);
        room.setName("Twin");
        room.setPrice(BigDecimal.valueOf(500));
        room.setCapacity(2);
        room.setInventoryUnits(stock);
        room = roomTypeRepository.saveAndFlush(room);
        return new Fixture(user, hotel, room, LocalDate.now().plusDays(30));
    }

    private HotelBookingRequest legacyRequest(Fixture fixture) {
        HotelBookingRequest request = new HotelBookingRequest();
        request.setHotelId(fixture.hotel().getId());
        request.setRoomId(fixture.room().getId());
        request.setCheckInDate(fixture.date());
        request.setCheckOutDate(fixture.date().plusDays(2));
        request.setGuests(2);
        request.setGuestName("Integration Guest");
        request.setPhone("00000000000");
        return request;
    }

    private CreateOrderRequest unifiedRequest(Fixture fixture) {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductType("HOTEL_ROOM");
        item.setProductId(fixture.hotel().getId());
        item.setSkuId(fixture.room().getId());
        item.setServiceStartDate(fixture.date());
        item.setServiceEndDate(fixture.date().plusDays(2));
        item.setQuantity(1);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));
        return request;
    }

    private record Fixture(User user, Hotel hotel, RoomType room, LocalDate date) {}
}
