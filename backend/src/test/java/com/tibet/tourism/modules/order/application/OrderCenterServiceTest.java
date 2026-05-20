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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
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
        when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderCenterService.createOrder(user, scenicOrderRequest(), "idem-1");

        assertEquals("PENDING_PAYMENT", response.status());
        assertEquals("UNPAID", response.paymentStatus());
        assertEquals(BigDecimal.valueOf(600), response.payableAmount());
        assertEquals(1, response.items().size());
        assertEquals("SCENIC_SPOT", response.items().get(0).productType());
        verify(inventoryLockRepository).save(any(InventoryLock.class));
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
        when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
