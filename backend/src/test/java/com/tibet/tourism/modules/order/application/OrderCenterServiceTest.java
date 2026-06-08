package com.tibet.tourism.modules.order.application;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.domain.CancellationPolicy;
import com.tibet.tourism.modules.order.domain.InventoryLock;
import com.tibet.tourism.modules.order.domain.OrderItem;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.domain.RefundOrder;
import com.tibet.tourism.modules.order.infra.CancellationPolicyRepository;
import com.tibet.tourism.modules.order.infra.InventoryLockRepository;
import com.tibet.tourism.modules.order.infra.PaymentTransactionRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import com.tibet.tourism.modules.order.web.dto.CancelOrderRequest;
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
    @Mock private PaymentCallbackAuditService paymentCallbackAuditService;
    @Mock private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock private InventoryLockRepository inventoryLockRepository;
    @Mock private ScenicSpotRepository scenicSpotRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private HotelBookingRepository hotelBookingRepository;

    private OrderCenterService orderCenterService;
    private User user;
    private ScenicSpot spot;

    @BeforeEach
    void setUp() {
        orderCenterService = new OrderCenterService(
                orderRepository,
                paymentTransactionRepository,
                paymentCallbackAuditService,
                cancellationPolicyRepository,
                inventoryLockRepository,
                scenicSpotRepository,
                hotelRepository,
                roomTypeRepository,
                hotelBookingRepository
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
        when(scenicSpotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
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
    void emptyOrderUsesReadableDefaultSummary() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("empty-order");
        request.setCustomerName("Traveler");
        request.setItems(List.of());

        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "empty-order")).thenReturn(Optional.empty());
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderCenterService.createOrder(user, request, "empty-order");

        assertEquals("旅行订单", response.productSummary());
    }

    @Test
    void validPaymentCallbackConfirmsOrderAndIssuesVoucher() {
        AtomicReference<PlatformOrder> savedOrder = new AtomicReference<>();
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(Optional.empty());
        when(scenicSpotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> {
            PlatformOrder order = invocation.getArgument(0);
            savedOrder.set(order);
            return order;
        });
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLockRepository.findByOrder(any(PlatformOrder.class))).thenReturn(List.of());

        var created = orderCenterService.createOrder(user, scenicOrderRequest(), "idem-1");
        when(orderRepository.findByOrderNoForUpdate(created.orderNo())).thenReturn(Optional.of(savedOrder.get()));

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
    void failedPaymentCallbackUsesReadableAuditNote() {
        PlatformOrder order = payableOrder("ORD-FAILED", PlatformOrder.Status.PENDING_PAYMENT);
        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);

        when(orderRepository.findByOrderNoForUpdate("ORD-FAILED")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionNo("PAY-FAILED")).thenReturn(Optional.empty());

        var response = orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                "ORD-FAILED",
                "PAY-FAILED",
                "MOCK",
                amount,
                "FAILED",
                signature("ORD-FAILED", "PAY-FAILED", amount, "FAILED")));

        assertEquals("FAILED", response.paymentStatus());
        assertTrue(order.getAuditLogs().stream().anyMatch(auditLog ->
                "PAYMENT_FAILED".equals(auditLog.getAction())
                        && "支付失败回调".equals(auditLog.getNote())));
    }

    @Test
    void invalidPaymentCallbackRecordsRejectedAuditAndStillThrows() {
        PlatformOrder order = payableOrder("ORD-BAD-SIG", PlatformOrder.Status.PENDING_PAYMENT);
        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        when(orderRepository.findByOrderNoForUpdate("ORD-BAD-SIG")).thenReturn(Optional.of(order));

        SecurityException error = assertThrows(
                SecurityException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-BAD-SIG", "PAY-BAD-SIG", "MOCK", amount, "SUCCESS", "bad-signature")));

        assertEquals("Invalid payment signature", error.getMessage());
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                transactionCaptor.capture(),
                eq("PAYMENT_CALLBACK_REJECTED"),
                eq("Payment callback rejected: invalid signature"));
        PaymentTransaction rejectedTransaction = transactionCaptor.getValue();
        assertEquals("PAY-BAD-SIG", rejectedTransaction.getTransactionNo());
        assertEquals("MOCK", rejectedTransaction.getProvider());
        assertEquals(amount, rejectedTransaction.getAmount());
        assertEquals(PaymentTransaction.Status.FAILED, rejectedTransaction.getStatus());
        assertFalse(rejectedTransaction.getSignatureValid());
        assertFalse(rejectedTransaction.getCallbackPayload().contains("ORD-BAD-SIG"));
        assertFalse(rejectedTransaction.getCallbackPayload().contains("PAY-BAD-SIG"));
        assertTrue(rejectedTransaction.getCallbackPayload().contains("orderNoHash=order#"));
        assertTrue(rejectedTransaction.getCallbackPayload().contains("transactionNoHash=txn#"));
        assertTrue(order.getPaymentTransactions().isEmpty());
    }

    @Test
    void invalidPaymentCallbackStillThrowsOriginalRejectionWhenAuditFails() {
        PlatformOrder order = payableOrder("ORD-BAD-AUDIT", PlatformOrder.Status.PENDING_PAYMENT);
        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        when(orderRepository.findByOrderNoForUpdate("ORD-BAD-AUDIT")).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("database temporarily unavailable"))
                .when(paymentCallbackAuditService)
                .recordRejectedCallback(
                        eq(order),
                        any(PaymentTransaction.class),
                        eq("PAYMENT_CALLBACK_REJECTED"),
                        eq("Payment callback rejected: invalid signature"));

        SecurityException error = assertThrows(
                SecurityException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-BAD-AUDIT", "PAY-BAD-AUDIT", "MOCK", amount, "SUCCESS", "bad-signature")));

        assertEquals("Invalid payment signature", error.getMessage());
        assertTrue(order.getPaymentTransactions().isEmpty());
    }

    @Test
    void validPaymentCallbackCanReuseTransactionNumberAfterRejectedSignatureAudit() {
        PlatformOrder order = payableOrder("ORD-RETRY", PlatformOrder.Status.PENDING_PAYMENT);
        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        when(orderRepository.findByOrderNoForUpdate("ORD-RETRY")).thenReturn(Optional.of(order));

        assertThrows(
                SecurityException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-RETRY", "PAY-RETRY", "MOCK", amount, "SUCCESS", "bad-signature")));

        when(paymentTransactionRepository.findByTransactionNo("PAY-RETRY")).thenReturn(Optional.empty());
        when(inventoryLockRepository.findByOrder(order)).thenReturn(List.of());

        var response = orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                "ORD-RETRY", "PAY-RETRY", "MOCK", amount, "SUCCESS",
                signature("ORD-RETRY", "PAY-RETRY", amount, "SUCCESS")));

        assertEquals("CONFIRMED", response.status());
        assertEquals("PAID", response.paymentStatus());
        assertEquals(1, response.paymentTransactions().size());
        assertEquals("SUCCESS", response.paymentTransactions().get(0).status());
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                any(PaymentTransaction.class),
                eq("PAYMENT_CALLBACK_REJECTED"),
                eq("Payment callback rejected: invalid signature"));
    }

    @Test
    void amountMismatchPaymentCallbackRecordsIndependentAuditAndStillThrows() {
        PlatformOrder order = payableOrder("ORD-AMOUNT", PlatformOrder.Status.PENDING_PAYMENT);
        BigDecimal callbackAmount = BigDecimal.valueOf(601).setScale(2);
        when(orderRepository.findByOrderNoForUpdate("ORD-AMOUNT")).thenReturn(Optional.of(order));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-AMOUNT", "PAY-AMOUNT", "MOCK", callbackAmount, "SUCCESS",
                        signature("ORD-AMOUNT", "PAY-AMOUNT", callbackAmount, "SUCCESS"))));

        assertEquals("Payment amount mismatch", error.getMessage());
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                transactionCaptor.capture(),
                eq("PAYMENT_AMOUNT_MISMATCH"),
                eq("Payment callback rejected: amount mismatch"));
        PaymentTransaction rejectedTransaction = transactionCaptor.getValue();
        assertEquals("PAY-AMOUNT", rejectedTransaction.getTransactionNo());
        assertEquals(PaymentTransaction.Status.FAILED, rejectedTransaction.getStatus());
        assertTrue(rejectedTransaction.getSignatureValid());
        assertFalse(rejectedTransaction.getCallbackPayload().contains("ORD-AMOUNT"));
        assertFalse(rejectedTransaction.getCallbackPayload().contains("PAY-AMOUNT"));
        assertTrue(order.getPaymentTransactions().isEmpty());
    }

    @Test
    void latePaymentCallbackDoesNotReviveCancelledOrder() {
        PlatformOrder order = payableOrder("ORD-CANCELLED", PlatformOrder.Status.CANCELLED);
        when(orderRepository.findByOrderNoForUpdate("ORD-CANCELLED")).thenReturn(Optional.of(order));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        var response = orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                "ORD-CANCELLED", "PAY-LATE", "MOCK", amount, "SUCCESS",
                signature("ORD-CANCELLED", "PAY-LATE", amount, "SUCCESS")));

        assertEquals("CANCELLED", response.status());
        assertEquals("UNPAID", response.paymentStatus());
        assertEquals(0, response.vouchers().size());
        assertEquals(1, response.paymentTransactions().size());
        assertEquals("SUCCESS", response.paymentTransactions().get(0).status());
    }

    @Test
    void expiredOrderCannotBeConfirmedByLateCallbackAndReleasesLock() {
        PlatformOrder order = payableOrder("ORD-EXPIRED", PlatformOrder.Status.PENDING_PAYMENT);
        order.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        InventoryLock lock = new InventoryLock();
        lock.setStatus(InventoryLock.Status.LOCKED);
        lock.setActiveLockKey("HOTEL_ROOM:20:30:2026-06-01");

        when(orderRepository.findByOrderNoForUpdate("ORD-EXPIRED")).thenReturn(Optional.of(order));
        when(inventoryLockRepository.findByOrder(order)).thenReturn(List.of(lock));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        var response = orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                "ORD-EXPIRED", "PAY-EXPIRED", "MOCK", amount, "SUCCESS",
                signature("ORD-EXPIRED", "PAY-EXPIRED", amount, "SUCCESS")));

        assertEquals("EXPIRED", response.status());
        assertEquals("UNPAID", response.paymentStatus());
        assertEquals(InventoryLock.Status.EXPIRED, lock.getStatus());
        assertNull(lock.getActiveLockKey());
        assertEquals(0, response.vouchers().size());
    }

    @Test
    void duplicatePaymentTransactionReturnsExistingOrderWithoutReconfirming() {
        PlatformOrder order = payableOrder("ORD-PAID", PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        PaymentTransaction transaction = successfulTransaction(order, "PAY-1", BigDecimal.valueOf(600).setScale(2));

        when(orderRepository.findByOrderNoForUpdate("ORD-PAID")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionNo("PAY-1")).thenReturn(Optional.of(transaction));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        var response = orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                "ORD-PAID", "PAY-1", "MOCK", amount, "SUCCESS",
                signature("ORD-PAID", "PAY-1", amount, "SUCCESS")));

        assertEquals("CONFIRMED", response.status());
        assertEquals("PAID", response.paymentStatus());
        assertEquals(1, response.paymentTransactions().size());
        verify(inventoryLockRepository, never()).findByOrder(order);
    }

    @Test
    void duplicatePaymentTransactionForAnotherOrderIsRejectedAndAudited() {
        PlatformOrder order = payableOrder("ORD-CURRENT", PlatformOrder.Status.PENDING_PAYMENT);
        PlatformOrder otherOrder = payableOrder("ORD-OTHER", PlatformOrder.Status.CONFIRMED);
        PaymentTransaction existing = successfulTransaction(otherOrder, "PAY-DUP", BigDecimal.valueOf(600).setScale(2));

        when(orderRepository.findByOrderNoForUpdate("ORD-CURRENT")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionNo("PAY-DUP")).thenReturn(Optional.of(existing));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-CURRENT", "PAY-DUP", "MOCK", amount, "SUCCESS",
                        signature("ORD-CURRENT", "PAY-DUP", amount, "SUCCESS"))));

        assertEquals("Payment transaction number already belongs to another order", error.getMessage());
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                transactionCaptor.capture(),
                eq("PAYMENT_CALLBACK_DUPLICATE_REJECTED"),
                eq("Payment callback rejected: transaction belongs to another order"));
        assertEquals(PaymentTransaction.Status.FAILED, transactionCaptor.getValue().getStatus());
        assertTrue(transactionCaptor.getValue().getSignatureValid());
    }

    @Test
    void duplicatePaymentTransactionWithDifferentDataIsRejectedAndAudited() {
        PlatformOrder order = payableOrder("ORD-PAID", PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        successfulTransaction(order, "PAY-1", BigDecimal.valueOf(600).setScale(2));

        when(orderRepository.findByOrderNoForUpdate("ORD-PAID")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionNo("PAY-1"))
                .thenReturn(Optional.of(order.getPaymentTransactions().get(0)));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-PAID", "PAY-1", "MOCK", amount, "FAILED",
                        signature("ORD-PAID", "PAY-1", amount, "FAILED"))));

        assertEquals("Payment transaction number already exists with different callback data", error.getMessage());
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                any(PaymentTransaction.class),
                eq("PAYMENT_CALLBACK_DUPLICATE_REJECTED"),
                eq("Payment callback rejected: duplicate transaction payload mismatch"));
    }

    @Test
    void duplicatePaymentTransactionWithInvalidSignatureIsRejectedAndAudited() {
        PlatformOrder order = payableOrder("ORD-PAID", PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        successfulTransaction(order, "PAY-1", BigDecimal.valueOf(600).setScale(2));

        when(orderRepository.findByOrderNoForUpdate("ORD-PAID")).thenReturn(Optional.of(order));

        BigDecimal amount = BigDecimal.valueOf(600).setScale(2);
        SecurityException error = assertThrows(
                SecurityException.class,
                () -> orderCenterService.handlePaymentCallback(new PaymentCallbackRequest(
                        "ORD-PAID", "PAY-1", "MOCK", amount, "SUCCESS", "bad-signature")));

        assertEquals("Invalid payment signature", error.getMessage());
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentCallbackAuditService).recordRejectedCallback(
                eq(order),
                transactionCaptor.capture(),
                eq("PAYMENT_CALLBACK_REJECTED"),
                eq("Payment callback rejected: invalid signature"));
        PaymentTransaction rejectedTransaction = transactionCaptor.getValue();
        assertEquals("PAY-1", rejectedTransaction.getTransactionNo());
        assertEquals(PaymentTransaction.Status.FAILED, rejectedTransaction.getStatus());
        assertFalse(rejectedTransaction.getSignatureValid());
        verify(paymentTransactionRepository, never()).findByTransactionNo("PAY-1");
    }

    @Test
    void paymentCallbackWithMissingSignatureIsInvalid() {
        boolean valid = orderCenterService.verifyCallbackSignature(
                new PaymentCallbackRequest("ORD-1", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", null));

        assertFalse(valid);
    }

    @Test
    void paymentCallbackWithMissingSecretIsInvalid() {
        ReflectionTestUtils.setField(orderCenterService, "callbackSecret", "");

        boolean valid = orderCenterService.verifyCallbackSignature(
                new PaymentCallbackRequest("ORD-1", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", "sig"));

        assertFalse(valid);
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
    void hotelRoomOrderRejectsOverlappingLegacyBooking() {
        Hotel hotel = hotel();
        com.tibet.tourism.modules.hotel.domain.RoomType roomType = roomType(hotel);
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "hotel-1")).thenReturn(Optional.empty());
        when(hotelRepository.findById(20L)).thenReturn(Optional.of(hotel));
        when(roomTypeRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(roomType));
        when(hotelBookingRepository.findOverlappingActiveBookingsForUpdate(
                eq(30L), any(), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 3))))
                .thenReturn(List.of(new HotelBooking()));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.createOrder(user, hotelOrderRequest(), "hotel-1"));

        assertEquals("Room type is unavailable for the selected dates", error.getMessage());
        verify(inventoryLockRepository, never()).saveAndFlush(any());
    }

    @Test
    void legacySpotBookingWithNullUserMirrorsWithoutUserIdLookup() {
        Booking booking = new Booking();
        booking.setId(501L);
        booking.setUser(null);
        booking.setSpot(spot);
        booking.setStatus(Booking.Status.PENDING);
        booking.setVisitDate(LocalDate.of(2026, 6, 1));
        booking.setTicketCount(1);
        booking.setTotalPrice(BigDecimal.valueOf(200));

        when(orderRepository.findBySourceTypeAndSourceReferenceId("LEGACY_SPOT_BOOKING", 501L))
                .thenReturn(Optional.empty());
        when(scenicSpotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
        when(inventoryLockRepository.findActiveProductDateLocksForUpdate(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformOrder order = orderCenterService.createFromLegacySpotBooking(booking);

        assertNull(order.getUser());
        assertEquals("LEGACY_SPOT_BOOKING", order.getSourceType());
        assertEquals(501L, order.getSourceReferenceId());
        verify(orderRepository, never()).findByUserIdAndIdempotencyKey(any(), any());
    }

    @Test
    void legacyHotelBookingWithNullUserMirrorsWithoutUserIdLookup() {
        HotelBooking booking = new HotelBooking();
        booking.setId(502L);
        booking.setUser(null);
        booking.setHotel(hotel());
        booking.setRoomTypeId(30L);
        booking.setRoomName("Twin Room");
        booking.setRoomPrice(BigDecimal.valueOf(500));
        booking.setTotalPrice(BigDecimal.valueOf(1000));
        booking.setCheckInDate(LocalDate.of(2026, 6, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 6, 3));
        booking.setStatus(HotelBooking.Status.PENDING);

        when(orderRepository.findBySourceTypeAndSourceReferenceId("LEGACY_HOTEL_BOOKING", 502L))
                .thenReturn(Optional.empty());
        when(inventoryLockRepository.findByActiveLockKey(anyString())).thenReturn(Optional.empty());
        when(inventoryLockRepository.existsByActiveLockKey(anyString())).thenReturn(false);
        when(inventoryLockRepository.saveAndFlush(any(InventoryLock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(PlatformOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformOrder order = orderCenterService.createFromLegacyHotelBooking(booking);

        assertNull(order.getUser());
        assertEquals("LEGACY_HOTEL_BOOKING", order.getSourceType());
        assertEquals(502L, order.getSourceReferenceId());
        verify(orderRepository, never()).findByUserIdAndIdempotencyKey(any(), any());
    }

    @Test
    void mockPaymentCallbackIsDisabledByDefault() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", false);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.handleMockPaymentCallback(user,
                        new PaymentCallbackRequest("ORD-1", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", "sig")));

        assertEquals("Mock payment callback is disabled", error.getMessage());
        verify(orderRepository, never()).findByOrderNoForUpdate(anyString());
    }

    @Test
    void mockPaymentCallbackRejectsNonOwnerOrder() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", true);

        User anotherUser = new User();
        anotherUser.setId(2L);

        PlatformOrder order = new PlatformOrder();
        order.setUser(anotherUser);
        order.setOrderNo("ORD-OTHER");
        when(orderRepository.findByOrderNoForUpdate("ORD-OTHER")).thenReturn(Optional.of(order));

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

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));
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

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.deleteClosedOrder(user, 99L));

        assertEquals("Only closed orders can be deleted", error.getMessage());
        verify(inventoryLockRepository, never()).deleteAll(anyList());
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void getOrderExpiresPendingOrderAndReleasesInventoryLock() {
        PlatformOrder order = payableOrder("ORD-STALE", PlatformOrder.Status.PENDING_PAYMENT);
        order.setId(99L);
        order.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        InventoryLock lock = new InventoryLock();
        lock.setStatus(InventoryLock.Status.LOCKED);
        lock.setActiveLockKey("HOTEL_ROOM:20:30:2026-06-01");

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));
        when(inventoryLockRepository.findByOrder(order)).thenReturn(List.of(lock));

        var response = orderCenterService.getOrder(user, 99L);

        assertEquals("EXPIRED", response.status());
        assertEquals(InventoryLock.Status.EXPIRED, lock.getStatus());
        assertNull(lock.getActiveLockKey());
    }

    @Test
    void getOrderRejectsMissingAuthenticatedUserBeforeRepositoryLookup() {
        AuthenticationRequiredException error = assertThrows(
                AuthenticationRequiredException.class,
                () -> orderCenterService.getOrder(null, 99L));

        assertEquals("Authentication required", error.getMessage());
        verify(orderRepository, never()).findByIdAndUserIdForUpdate(any(), any());
    }

    @Test
    void mockPaymentCallbackRejectsMissingAuthenticatedUserBeforeOrderLookup() {
        ReflectionTestUtils.setField(orderCenterService, "mockCallbackEnabled", true);

        AuthenticationRequiredException error = assertThrows(
                AuthenticationRequiredException.class,
                () -> orderCenterService.handleMockPaymentCallback(null,
                        new PaymentCallbackRequest("ORD-1", "PAY-1", "MOCK", BigDecimal.TEN, "SUCCESS", "sig")));

        assertEquals("Authentication required", error.getMessage());
        verify(orderRepository, never()).findByOrderNoForUpdate(anyString());
    }

    @Test
    void orderResponseMasksCustomerPii() {
        PlatformOrder order = payableOrder("ORD-PII", PlatformOrder.Status.PENDING_PAYMENT);
        order.setId(99L);
        order.setCustomerName("Traveler");
        order.setCustomerPhone("13900000000");

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));

        var response = orderCenterService.getOrder(user, 99L);

        assertEquals("T***r", response.customerName());
        assertEquals("139****0000", response.customerPhone());
    }

    @Test
    void cancelConfirmedOrderUsesCancellationPolicyRefundRateAfterFreeWindow() {
        PlatformOrder order = paidOrderWithItem();
        OrderItem item = order.getItems().get(0);
        item.setServiceStartDate(LocalDate.now().plusDays(1));
        item.setCancellationPolicyId(5L);

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));
        when(cancellationPolicyRepository.findById(5L))
                .thenReturn(Optional.of(policy(72, new BigDecimal("0.50"))));

        var response = orderCenterService.cancelOrder(user, 99L, new CancelOrderRequest());

        assertEquals("REFUND_PENDING", response.status());
        assertEquals(new BigDecimal("300.00"), response.refunds().get(0).amount());
    }

    @Test
    void refundRequestUsesFullAmountInsideFreeCancellationWindow() {
        PlatformOrder order = paidOrderWithItem();
        OrderItem item = order.getItems().get(0);
        item.setServiceStartDate(LocalDate.now().plusDays(10));
        item.setCancellationPolicyId(5L);

        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));
        when(cancellationPolicyRepository.findById(5L))
                .thenReturn(Optional.of(policy(72, new BigDecimal("0.50"))));

        var response = orderCenterService.requestRefund(
                user,
                99L,
                new com.tibet.tourism.modules.order.web.dto.RefundRequest());

        assertEquals(new BigDecimal("600"), response.amount());
    }

    @Test
    void refundRequestRejectsDuplicatePendingRefund() {
        PlatformOrder order = paidOrderWithItem();
        RefundOrder pending = new RefundOrder();
        pending.setStatus(RefundOrder.Status.REQUESTED);
        pending.setAmount(BigDecimal.valueOf(100));
        order.addRefund(pending);
        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.requestRefund(user, 99L, new com.tibet.tourism.modules.order.web.dto.RefundRequest()));

        assertEquals("Refund already pending", error.getMessage());
        assertEquals(1, order.getRefunds().size());
    }

    @Test
    void refundRequestRejectsAmountAboveRemainingPaidAmount() {
        PlatformOrder order = paidOrderWithItem();
        RefundOrder completed = new RefundOrder();
        completed.setStatus(RefundOrder.Status.COMPLETED);
        completed.setAmount(BigDecimal.valueOf(600));
        order.addRefund(completed);
        when(orderRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> orderCenterService.requestRefund(user, 99L, new com.tibet.tourism.modules.order.web.dto.RefundRequest()));

        assertEquals("Refund amount exceeds paid amount", error.getMessage());
        assertEquals(1, order.getRefunds().size());
    }

    private PlatformOrder payableOrder(String orderNo, PlatformOrder.Status status) {
        PlatformOrder order = new PlatformOrder();
        order.setId(99L);
        order.setUser(user);
        order.setOrderNo(orderNo);
        order.setStatus(status);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.UNPAID);
        order.setProductSummary("Test order");
        order.setTotalAmount(BigDecimal.valueOf(600));
        order.setPayableAmount(BigDecimal.valueOf(600));

        OrderItem item = new OrderItem();
        item.setId(77L);
        item.setProductType(OrderItem.ProductType.SCENIC_SPOT);
        item.setProductId(10L);
        item.setProductName("Test spot");
        item.setServiceStartDate(LocalDate.of(2026, 6, 1));
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(300));
        item.setSubtotal(BigDecimal.valueOf(600));
        item.setStatus(status == PlatformOrder.Status.CONFIRMED ? OrderItem.Status.CONFIRMED : OrderItem.Status.LOCKED);
        order.addItem(item);
        return order;
    }

    private PlatformOrder paidOrderWithItem() {
        PlatformOrder order = payableOrder("ORD-REFUND", PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        order.setPaidAt(java.time.LocalDateTime.now());
        successfulTransaction(order, "PAY-REFUND", BigDecimal.valueOf(600));
        return order;
    }

    private PaymentTransaction successfulTransaction(PlatformOrder order, String transactionNo, BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setProvider("MOCK");
        transaction.setAmount(amount);
        transaction.setStatus(PaymentTransaction.Status.SUCCESS);
        transaction.setSignatureValid(true);
        transaction.setPaidAt(java.time.LocalDateTime.now());
        order.addPaymentTransaction(transaction);
        return transaction;
    }

    private CancellationPolicy policy(int freeCancelBeforeHours, BigDecimal refundRate) {
        CancellationPolicy policy = new CancellationPolicy();
        policy.setProductType(OrderItem.ProductType.SCENIC_SPOT);
        policy.setFreeCancelBeforeHours(freeCancelBeforeHours);
        policy.setRefundRate(refundRate);
        return policy;
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
