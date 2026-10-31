package com.tibet.tourism.modules.hotel.application;

import com.tibet.tourism.modules.hotel.domain.Hotel;
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
        "app.seed.content.enabled=false",
        "app.seed.demo-users.enabled=false",
        "recommendation.item-similarity.preload-on-startup=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TESTS", matches = "true")
class HotelInventoryConcurrencyIntegrationTest {
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HotelBookingService hotelBookingService;
    @Autowired private OrderCenterService orderCenterService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
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
