package com.tibet.tourism.modules.order.application;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.domain.InventoryLock;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.infra.CancellationPolicyRepository;
import com.tibet.tourism.modules.order.infra.InventoryLockRepository;
import com.tibet.tourism.modules.order.infra.PaymentTransactionRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import com.tibet.tourism.modules.order.web.dto.CreateOrderItemRequest;
import com.tibet.tourism.modules.order.web.dto.CreateOrderRequest;
import com.tibet.tourism.modules.order.web.dto.PaymentCallbackRequest;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCenterServiceTest {

    @Mock private PlatformOrderRepository orderRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock private InventoryLockRepository inventoryLockRepository;
    @Mock private ScenicSpotRepository scenicSpotRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;

    private OrderCenterService orderCenterService;
    private User user;
    private ScenicSpot spot;

    @BeforeEach
    void setUp() {
        orderCenterService = new OrderCenterService(
                orderRepository,
                paymentTransactionRepository,
                cancellationPolicyRepository,
                inventoryLockRepository,
                scenicSpotRepository,
                hotelRepository,
                roomTypeRepository
        );
        ReflectionTestUtils.setField(orderCenterService, "callbackSecret", "test-secret");

        user = new User();
        user.setId(1L);
        user.setUsername("traveler");

        spot = new ScenicSpot();
        spot.setId(10L);
        spot.setName("布达拉宫");
        spot.setTicketPrice(BigDecimal.valueOf(200));
        spot.setPeakSeasonPrice(BigDecimal.valueOf(300));
    }

    @Test
    void createOrderBuildsUnifiedScenicOrderAndInventoryLock() {
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(Optional.empty());
        when(scenicSpotRepository.findById(10L)).thenReturn(Optional.of(spot));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderCenterService.createOrder(user, scenicOrderRequest(), "idem-1");

        assertEquals("PENDING_PAYMENT", response.status());
        assertEquals("UNPAID", response.paymentStatus());
        assertEquals(BigDecimal.valueOf(600), response.payableAmount());
        assertEquals(1, response.items().size());
        assertEquals("SCENIC_SPOT", response.items().get(0).productType());
        verify(inventoryLockRepository).saveAndFlush(any(InventoryLock.class));
    }

    @Test
    void idempotencyReturnsExistingOrder() {
        PlatformOrder existing = new PlatformOrder();
        existing.setUser(user);
        existing.setOrderNo("ORD-EXISTING");
        existing.setProductSummary("已存在订单");
        existing.setPayableAmount(BigDecimal.TEN);
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(Optional.of(existing));

        var response = orderCenterService.createOrder(user, scenicOrderRequest(), "idem-1");

        assertEquals("ORD-EXISTING", response.orderNo());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validPaymentCallbackConfirmsOrderAndIssuesVoucher() {
        AtomicReference<PlatformOrder> savedOrder = new AtomicReference<>();
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(Optional.empty());
        when(scenicSpotRepository.findById(10L)).thenReturn(Optional.of(spot));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> {
            PlatformOrder order = invocation.getArgument(0);
            savedOrder.set(order);
            return order;
        });
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLockRepository.findByOrder(any(PlatformOrder.class))).thenReturn(List.of());

        var created = orderCenterService.createOrder(user, scenicOrderRequest(), "idem-1");
        when(orderRepository.findByOrderNo(created.orderNo())).thenReturn(Optional.of(savedOrder.get()));

        String transactionNo = "PAY-1";
        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        String signature = signature(created.orderNo(), transactionNo, amount, "SUCCESS");
        var response = orderCenterService.handlePaymentCallback(
                new PaymentCallbackRequest(created.orderNo(), transactionNo, "MOCK", amount, "SUCCESS", signature));

        assertEquals("CONFIRMED", response.status());
        assertEquals("PAID", response.paymentStatus());
        assertEquals(1, response.vouchers().size());
        assertEquals("SUCCESS", response.paymentTransactions().get(0).status());
    }

    @Test
    void hotelRoomOrderCreatesActiveLockForEachNight() {
        Hotel hotel = hotel();
        com.tibet.tourism.modules.hotel.domain.RoomType roomType = roomType(hotel);
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "hotel-1")).thenReturn(Optional.empty());
        when(hotelRepository.findById(20L)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(roomType));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLockRepository.findByActiveLockKey(anyString())).thenReturn(Optional.empty());
        when(inventoryLockRepository.existsByActiveLockKey(anyString())).thenReturn(false);
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderCenterService.createOrder(user, hotelOrderRequest(), "hotel-1");

        ArgumentCaptor<InventoryLock> captor = ArgumentCaptor.forClass(InventoryLock.class);
        verify(inventoryLockRepository, times(2)).saveAndFlush(captor.capture());
        assertEquals(
                List.of("HOTEL_ROOM:20:30:2026-06-01", "HOTEL_ROOM:20:30:2026-06-02"),
                captor.getAllValues().stream().map(InventoryLock::getActiveLockKey).toList());
    }

    @Test
    void hotelRoomOrderRejectsExistingActiveLock() {
        Hotel hotel = hotel();
        com.tibet.tourism.modules.hotel.domain.RoomType roomType = roomType(hotel);
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "hotel-1")).thenReturn(Optional.empty());
        when(hotelRepository.findById(20L)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(roomType));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLockRepository.findByActiveLockKey("HOTEL_ROOM:20:30:2026-06-01")).thenReturn(Optional.empty());
        when(inventoryLockRepository.existsByActiveLockKey("HOTEL_ROOM:20:30:2026-06-01")).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.createOrder(user, hotelOrderRequest(), "hotel-1"));

        assertEquals("Room type is unavailable for the selected dates", error.getMessage());
        verify(inventoryLockRepository, never()).saveAndFlush(any());
    }

    @Test
    void mockPaymentCallbackIsDisabledByDefault() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", false);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.handleMockPaymentCallback(user,
                        new PaymentCallbackRequest("ORD-1", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", "sig")));

        assertEquals("Mock payment callback is disabled", error.getMessage());
        verify(orderRepository, never()).findByOrderNo(anyString());
    }

    @Test
    void mockPaymentCallbackRejectsNonOwnerOrder() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", true);

        User anotherUser = new User();
        anotherUser.setId(2L);

        PlatformOrder order = new PlatformOrder();
        order.setUser(anotherUser);
        order.setOrderNo("ORD-OTHER");
        when(orderRepository.findByOrderNo("ORD-OTHER")).thenReturn(Optional.of(order));

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> orderCenterService.handleMockPaymentCallback(user,
                        new PaymentCallbackRequest("ORD-OTHER", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", "sig")));
    }

    @Test
    void strictConfigurationRejectsDevelopmentPaymentCallbackSecret() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", true);
        ReflectionTestUtils.setField(orderCenterService, "requireStrongSecrets", true);
        ReflectionTestUtils.setField(orderCenterService, "callbackSecret", "dev-payment-callback-secret");
        ReflectionTestUtils.setField(orderCenterService, "environment", new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.validatePaymentCallbackConfiguration());

        assertEquals("Production payment callback secret cannot use the development placeholder", error.getMessage());
    }

    @Test
    void deleteClosedOrderRemovesOrderAndInventoryLocks() {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setUser(user);
        order.setStatus(PlatformOrder.Status.EXPIRED);
        InventoryLock lock = new InventoryLock();

        when(orderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(order));
        when(inventoryLockRepository.findByOrder(order)).thenReturn(List.of(lock));

        orderCenterService.deleteClosedOrder(user, 99L);

        verify(inventoryLockRepository).deleteAll(List.of(lock));
        verify(orderRepository).delete(order);
    }

    @Test
    void deleteClosedOrderRejectsOpenOrder() {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setUser(user);
        order.setStatus(PlatformOrder.Status.PENDING_PAYMENT);

        when(orderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.deleteClosedOrder(user, 99L));

        assertEquals("仅已关闭订单可以删除", error.getMessage());
        verify(inventoryLockRepository, never()).deleteAll(anyList());
        verify(orderRepository, never()).delete(any());
    }

    private CreateOrderRequest scenicOrderRequest() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductType("SCENIC_SPOT");
        item.setProductId(10L);
        item.setServiceStartDate(LocalDate.of(2026, 6, 1));
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("idem-1");
        request.setCustomerName("Traveler");
        request.setItems(List.of(item));
        return request;
    }

    private CreateOrderRequest hotelOrderRequest() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductType("HOTEL_ROOM");
        item.setProductId(20L);
        item.setSkuId(30L);
        item.setServiceStartDate(LocalDate.of(2026, 6, 1));
        item.setServiceEndDate(LocalDate.of(2026, 6, 3));
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("hotel-1");
        request.setCustomerName("Traveler");
        request.setItems(List.of(item));
        return request;
    }

    private Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setId(20L);
        hotel.setName("Lhasa Hotel");
        return hotel;
    }

    private com.tibet.tourism.modules.hotel.domain.RoomType roomType(Hotel hotel) {
        com.tibet.tourism.modules.hotel.domain.RoomType roomType =
                new com.tibet.tourism.modules.hotel.domain.RoomType();
        roomType.setId(30L);
        roomType.setHotel(hotel);
        roomType.setName("Twin Room");
        roomType.setPrice(BigDecimal.valueOf(500));
        roomType.setCapacity(2);
        return roomType;
    }

    private String signature(String orderNo, String transactionNo, BigDecimal amount, String status) {
        try {
            String payload = orderNo + "|" + transactionNo + "|" + amount + "|" + status;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
