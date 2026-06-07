package com.tibet.tourism.modules.order.application;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.domain.CancellationPolicy;
import com.tibet.tourism.modules.order.domain.InventoryLock;
import com.tibet.tourism.modules.order.domain.Invoice;
import com.tibet.tourism.modules.order.domain.OrderAuditLog;
import com.tibet.tourism.modules.order.domain.OrderItem;
import com.tibet.tourism.modules.order.domain.PaymentTransaction;
import com.tibet.tourism.modules.order.domain.PlatformOrder;
import com.tibet.tourism.modules.order.domain.RefundOrder;
import com.tibet.tourism.modules.order.domain.Voucher;
import com.tibet.tourism.modules.order.infra.CancellationPolicyRepository;
import com.tibet.tourism.modules.order.infra.InventoryLockRepository;
import com.tibet.tourism.modules.order.infra.PaymentTransactionRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import com.tibet.tourism.modules.order.web.dto.CancelOrderRequest;
import com.tibet.tourism.modules.order.web.dto.CreateOrderItemRequest;
import com.tibet.tourism.modules.order.web.dto.CreateOrderRequest;
import com.tibet.tourism.modules.order.web.dto.InvoiceRequest;
import com.tibet.tourism.modules.order.web.dto.InvoiceResponse;
import com.tibet.tourism.modules.order.web.dto.OrderItemResponse;
import com.tibet.tourism.modules.order.web.dto.OrderResponse;
import com.tibet.tourism.modules.order.web.dto.PaymentCallbackRequest;
import com.tibet.tourism.modules.order.web.dto.PaymentTransactionResponse;
import com.tibet.tourism.modules.order.web.dto.RefundRequest;
import com.tibet.tourism.modules.order.web.dto.RefundResponse;
import com.tibet.tourism.modules.order.web.dto.VoucherResponse;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderCenterService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCenterService.class);
    private static final int LOCK_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MIN_CALLBACK_SECRET_LENGTH = 32;
    private static final String DEV_CALLBACK_SECRET = "dev-payment-callback-secret";
    private static final Set<InventoryLock.Status> ACTIVE_INVENTORY_STATUSES =
            Set.of(InventoryLock.Status.LOCKED, InventoryLock.Status.CONFIRMED);
    private static final Set<RefundOrder.Status> PENDING_REFUND_STATUSES =
            Set.of(RefundOrder.Status.REQUESTED, RefundOrder.Status.APPROVED);
    private static final Set<RefundOrder.Status> COUNTED_REFUND_STATUSES =
            Set.of(RefundOrder.Status.REQUESTED, RefundOrder.Status.APPROVED, RefundOrder.Status.COMPLETED);
    private static final Set<HotelBooking.Status> ACTIVE_HOTEL_BOOKING_STATUSES =
            Set.of(HotelBooking.Status.PENDING, HotelBooking.Status.CONFIRMED);

    private final PlatformOrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final ScenicSpotRepository scenicSpotRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelBookingRepository hotelBookingRepository;

    @Value("${app.payments.mock-callback-enabled:false}")
    private boolean mockCallbackEnabled;

    @Value("${app.payments.mock-callback-secret:}")
    private String callbackSecret;

    @Value("${app.security.require-strong-secrets:false}")
    private boolean requireStrongSecrets;

    @Autowired
    private Environment environment;

    public OrderCenterService(PlatformOrderRepository orderRepository,
                              PaymentTransactionRepository paymentTransactionRepository,
                              CancellationPolicyRepository cancellationPolicyRepository,
                              InventoryLockRepository inventoryLockRepository,
                              ScenicSpotRepository scenicSpotRepository,
                              HotelRepository hotelRepository,
                              RoomTypeRepository roomTypeRepository,
                              HotelBookingRepository hotelBookingRepository) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.inventoryLockRepository = inventoryLockRepository;
        this.scenicSpotRepository = scenicSpotRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.hotelBookingRepository = hotelBookingRepository;
    }

    @PostConstruct
    void validatePaymentCallbackConfiguration() {
        String secret = callbackSecret == null ? "" : callbackSecret.trim();
        callbackSecret = secret;

        if (!mockCallbackEnabled) {
            if (DEV_CALLBACK_SECRET.equals(secret)) {
                logger.warn("Mock payment callback is disabled, but the development callback secret is configured");
            }
            return;
        }

        boolean prodProfile = environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        boolean strictMode = requireStrongSecrets || prodProfile;

        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Payment callback secret must be configured when mock callbacks are enabled");
        }
        if (strictMode && DEV_CALLBACK_SECRET.equals(secret)) {
            throw new IllegalStateException("Production payment callback secret cannot use the development placeholder");
        }
        if (secret.length() < MIN_CALLBACK_SECRET_LENGTH) {
            throw new IllegalStateException("Payment callback secret must be at least "
                    + MIN_CALLBACK_SECRET_LENGTH + " characters when mock callbacks are enabled");
        }
        if (DEV_CALLBACK_SECRET.equals(secret)) {
            logger.warn("Using development payment callback secret; keep mock callbacks disabled outside local development");
        }
    }

    @Transactional
    public OrderResponse createOrder(User user, CreateOrderRequest request, String headerIdempotencyKey) {
        String idempotencyKey = normalizeIdempotencyKey(
                StringUtils.hasText(headerIdempotencyKey) ? headerIdempotencyKey : request.getIdempotencyKey());
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<PlatformOrder> existing = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(expireIfNeeded(existing.get()));
            }
        }

        PlatformOrder order = new PlatformOrder();
        order.setOrderNo(nextBusinessNo("ORD"));
        order.setUser(user);
        order.setIdempotencyKey(idempotencyKey);
        order.setCustomerName(InputSanitizer.optionalPlainText(request.getCustomerName(), 64, "customer name"));
        order.setCustomerPhone(InputSanitizer.optionalPlainText(request.getCustomerPhone(), 32, "customer phone"));
        order.setCustomerNote(InputSanitizer.optionalTextBlock(request.getCustomerNote(), 500, "澶囨敞"));
        order.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        order.setExpiresAt(order.getLockedUntil());

        BigDecimal total = BigDecimal.ZERO;
        int sortOrder = 0;
        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = buildOrderItem(itemRequest, sortOrder++);
            attachCancellationPolicy(item);
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayableAmount(total);
        order.setProductSummary(buildSummary(order.getItems()));
        order.addAuditLog(audit(user, "CREATE", null, order.getStatus().name(), "Unified order created and inventory locked"));

        PlatformOrder saved = orderRepository.save(order);
        createInventoryLocks(saved);
        return toResponse(saved);
    }

    @Transactional
    public List<OrderResponse> getMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::expireIfNeeded)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse getOrder(User user, Long id) {
        PlatformOrder order = orderRepository.findByIdAndUserIdForUpdate(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return toResponse(expireIfNeeded(order));
    }

    @Transactional
    public OrderResponse cancelOrder(User user, Long id, CancelOrderRequest request) {
        PlatformOrder order = orderRepository.findByIdAndUserIdForUpdate(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        expireIfNeeded(order);
        if (order.getStatus() == PlatformOrder.Status.CANCELLED || order.getStatus() == PlatformOrder.Status.EXPIRED) {
            return toResponse(order);
        }
        if (order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT) {
            transition(order, user, PlatformOrder.Status.CANCELLED,
                    InputSanitizer.optionalTextBlock(request == null ? null : request.getReason(), 500, "鍙栨秷鍘熷洜"));
            releaseLocks(order, InventoryLock.Status.RELEASED);
            order.setCancelledAt(LocalDateTime.now());
            order.getItems().forEach(item -> item.setStatus(OrderItem.Status.CANCELLED));
            return toResponse(order);
        }

        BigDecimal amount = refundAmountForOrder(order);
        ensureRefundAllowed(order, null, amount);
        RefundOrder refund = buildRefund(order, null, amount, request == null ? null : request.getReason());
        order.addRefund(refund);
        transition(order, user, PlatformOrder.Status.REFUND_PENDING, "Confirmed order cancelled, refund pending");
        order.setCancelledAt(LocalDateTime.now());
        order.getItems().forEach(item -> item.setStatus(OrderItem.Status.REFUND_PENDING));
        return toResponse(order);
    }

    @Transactional
    public RefundResponse requestRefund(User user, Long id, RefundRequest request) {
        PlatformOrder order = orderRepository.findByIdAndUserIdForUpdate(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        expireIfNeeded(order);
        if (order.getPaymentStatus() == PlatformOrder.PaymentStatus.UNPAID) {
            throw new IllegalStateException("Unpaid orders cannot be refunded");
        }
        OrderItem item = findOrderItem(order, request == null ? null : request.getOrderItemId());
        BigDecimal amount = item == null ? refundAmountForOrder(order) : refundAmountForItem(item);
        ensureRefundAllowed(order, item, amount);
        RefundOrder refund = buildRefund(order, item, amount, request == null ? null : request.getReason());
        order.addRefund(refund);
        transition(order, user, PlatformOrder.Status.REFUND_PENDING, "Refund requested by user");
        if (item != null) {
            item.setStatus(OrderItem.Status.REFUND_PENDING);
        }
        return toRefundResponse(refund);
    }

    @Transactional
    public InvoiceResponse requestInvoice(User user, Long id, InvoiceRequest request) {
        PlatformOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (order.getPaymentStatus() == PlatformOrder.PaymentStatus.UNPAID) {
            throw new IllegalStateException("Unpaid orders cannot request invoices");
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(nextBusinessNo("INV"));
        invoice.setInvoiceTitle(InputSanitizer.requiredPlainText(request.getInvoiceTitle(), 160, "鍙戠エ鎶ご"));
        invoice.setTaxNo(InputSanitizer.optionalPlainText(request.getTaxNo(), 64, "绋庡彿"));
        invoice.setAmount(order.getPayableAmount());
        invoice.setStatus(Invoice.Status.REQUESTED);
        order.addInvoice(invoice);
        order.addAuditLog(audit(user, "INVOICE_REQUESTED", order.getStatus().name(), order.getStatus().name(), "Invoice requested by user"));
        return toInvoiceResponse(invoice);
    }

    @Transactional
    public void deleteClosedOrder(User user, Long id) {
        PlatformOrder order = orderRepository.findByIdAndUserIdForUpdate(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        expireIfNeeded(order);
        if (!isClosedOrder(order)) {
            throw new IllegalStateException("Only closed orders can be deleted");
        }

        inventoryLockRepository.deleteAll(inventoryLockRepository.findByOrder(order));
        orderRepository.delete(order);
    }

    @Transactional
    public OrderResponse handleMockPaymentCallback(User actor, PaymentCallbackRequest request) {
        if (!mockCallbackEnabled) {
            throw new IllegalStateException("Mock payment callback is disabled");
        }
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Authenticated user is required for mock payment callback");
        }
        if (!"MOCK".equalsIgnoreCase(request.provider())) {
            throw new IllegalArgumentException("Mock payment callback only accepts MOCK provider");
        }

        PlatformOrder order = orderRepository.findByOrderNoForUpdate(request.orderNo())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (order.getUser() == null || !actor.getId().equals(order.getUser().getId())) {
            throw new NoSuchElementException("Order not found");
        }
        return handlePaymentCallback(order, request);
    }

    @Transactional
    public OrderResponse handlePaymentCallback(PaymentCallbackRequest request) {
        PlatformOrder order = orderRepository.findByOrderNoForUpdate(request.orderNo())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return handlePaymentCallback(order, request);
    }

    private OrderResponse handlePaymentCallback(PlatformOrder order, PaymentCallbackRequest request) {
        String transactionNo = InputSanitizer.requiredPlainText(request.transactionNo(), 64, "transactionNo");
        Optional<PaymentTransaction> duplicate = paymentTransactionRepository.findByTransactionNo(transactionNo);
        if (duplicate.isPresent()) {
            PaymentTransaction existing = duplicate.get();
            PlatformOrder existingOrder = existing.getOrder();
            if (existingOrder == null || !order.getOrderNo().equals(existingOrder.getOrderNo())) {
                throw new IllegalArgumentException("Payment transaction number already belongs to another order");
            }
            ensureSamePaymentCallback(existing, request);
            return toResponse(expireIfNeeded(existingOrder));
        }

        boolean signatureValid = verifyCallbackSignature(request);
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setProvider(InputSanitizer.requiredPlainText(request.provider(), 32, "payment provider"));
        transaction.setAmount(request.amount());
        transaction.setSignatureValid(signatureValid);
        transaction.setCallbackPayload(callbackPayloadSummary(request));

        if (!signatureValid) {
            transaction.setStatus(PaymentTransaction.Status.FAILED);
            order.addPaymentTransaction(transaction);
            order.addAuditLog(audit(null, "PAYMENT_CALLBACK_REJECTED", order.getStatus().name(), order.getStatus().name(), "鏀粯鍥炶皟绛惧悕鏃犳晥"));
            throw new SecurityException("Invalid payment signature");
        }

        expireIfNeeded(order);
        if (!sameAmount(order.getPayableAmount(), request.amount())) {
            transaction.setStatus(PaymentTransaction.Status.FAILED);
            order.addPaymentTransaction(transaction);
            order.addAuditLog(audit(null, "PAYMENT_AMOUNT_MISMATCH", order.getStatus().name(), order.getStatus().name(), "Payment amount mismatch"));
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        transaction.setStatus(paymentStatusFromCallback(request));
        transaction.setPaidAt(LocalDateTime.now());
        order.addPaymentTransaction(transaction);

        if (transaction.getStatus() == PaymentTransaction.Status.SUCCESS) {
            if (canConfirmPayment(order)) {
                confirmPaidOrder(order, "Payment callback verified");
            } else if (order.getStatus() == PlatformOrder.Status.CONFIRMED
                    && order.getPaymentStatus() == PlatformOrder.PaymentStatus.PAID) {
                order.addAuditLog(audit(null, "PAYMENT_CALLBACK_IDEMPOTENT",
                        order.getStatus().name(), order.getStatus().name(), "Order already paid"));
            } else {
                order.addAuditLog(audit(null, "PAYMENT_CALLBACK_IGNORED",
                        order.getStatus().name(), order.getStatus().name(), "Late payment callback ignored"));
            }
        } else {
            if (order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT) {
                order.setPaymentStatus(PlatformOrder.PaymentStatus.FAILED);
            }
            order.addAuditLog(audit(null, "PAYMENT_FAILED", order.getStatus().name(), order.getStatus().name(), "鏀粯澶辫触鍥炶皟"));
        }
        return toResponse(order);
    }

    @Transactional
    public PlatformOrder createFromLegacySpotBooking(Booking booking) {
        String idempotencyKey = "LEGACY_SPOT_" + booking.getId();
        Optional<PlatformOrder> existing = orderRepository.findByUserIdAndIdempotencyKey(booking.getUser().getId(), idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean confirmed = booking.getStatus() == Booking.Status.CONFIRMED;
        PlatformOrder order = legacyOrder(booking.getUser(), idempotencyKey,
                "LEGACY_SPOT_BOOKING", booking.getId(), confirmed);
        order.setProductSummary(booking.getSpot().getName());
        order.setTotalAmount(defaultMoney(booking.getTotalPrice()));
        order.setPayableAmount(order.getTotalAmount());

        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.SCENIC_SPOT);
        item.setProductId(booking.getSpot().getId());
        item.setProductName(booking.getSpot().getName());
        item.setServiceStartDate(booking.getVisitDate());
        item.setQuantity(booking.getTicketCount());
        item.setUnitPrice(booking.getTicketCount() == null || booking.getTicketCount() == 0
                ? BigDecimal.ZERO
                : defaultMoney(booking.getTotalPrice()).divide(BigDecimal.valueOf(booking.getTicketCount()), 2, RoundingMode.HALF_UP));
        item.setSubtotal(defaultMoney(booking.getTotalPrice()));
        item.setStatus(confirmed ? OrderItem.Status.CONFIRMED : OrderItem.Status.LOCKED);
        item.setLegacyReferenceType("SPOT_BOOKING");
        item.setLegacyReferenceId(booking.getId());
        order.addItem(item);
        if (confirmed) {
            addLegacyPaymentAndVoucher(order, item);
        }
        PlatformOrder saved = orderRepository.save(order);
        createInventoryLocks(saved);
        if (confirmed) {
            releaseLocks(saved, InventoryLock.Status.CONFIRMED);
        }
        return saved;
    }

    @Transactional
    public PlatformOrder createFromLegacyHotelBooking(HotelBooking booking) {
        String idempotencyKey = "LEGACY_HOTEL_" + booking.getId();
        Optional<PlatformOrder> existing = orderRepository.findByUserIdAndIdempotencyKey(booking.getUser().getId(), idempotencyKey);
        if (existing.isPresent()) {
            PlatformOrder order = existing.get();
            if (booking.getStatus() == HotelBooking.Status.CONFIRMED && canConfirmPayment(order)) {
                order.getItems().stream().findFirst()
                        .filter(item -> order.getPaymentTransactions().isEmpty())
                        .ifPresent(item -> addLegacyPaymentAndVoucher(order, item));
                confirmPaidOrder(order, "Legacy hotel booking confirmed");
            }
            return order;
        }

        boolean confirmed = booking.getStatus() == HotelBooking.Status.CONFIRMED;
        PlatformOrder order = legacyOrder(booking.getUser(), idempotencyKey,
                "LEGACY_HOTEL_BOOKING", booking.getId(), confirmed);
        order.setCustomerName(booking.getGuestName());
        order.setCustomerPhone(booking.getPhone());
        order.setCustomerNote(booking.getNote());
        order.setProductSummary(booking.getHotel().getName() + " 路 " + booking.getRoomName());
        order.setTotalAmount(defaultMoney(booking.getTotalPrice()));
        order.setPayableAmount(order.getTotalAmount());

        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.HOTEL_ROOM);
        item.setProductId(booking.getHotel().getId());
        item.setSkuId(booking.getRoomTypeId());
        item.setProductName(booking.getHotel().getName());
        item.setSkuName(booking.getRoomName());
        item.setServiceStartDate(booking.getCheckInDate());
        item.setServiceEndDate(booking.getCheckOutDate());
        item.setQuantity(1);
        item.setUnitPrice(defaultMoney(booking.getRoomPrice()));
        item.setSubtotal(defaultMoney(booking.getTotalPrice()));
        item.setStatus(confirmed ? OrderItem.Status.CONFIRMED : OrderItem.Status.LOCKED);
        item.setLegacyReferenceType("HOTEL_BOOKING");
        item.setLegacyReferenceId(booking.getId());
        order.addItem(item);
        if (confirmed) {
            addLegacyPaymentAndVoucher(order, item);
        }
        PlatformOrder saved = orderRepository.save(order);
        createInventoryLocks(saved);
        if (confirmed) {
            releaseLocks(saved, InventoryLock.Status.CONFIRMED);
        }
        return saved;
    }

    @Transactional
    public void cancelLegacyMirror(User actor, String sourceType, Long sourceReferenceId, String reason) {
        orderRepository.findBySourceTypeAndSourceReferenceId(sourceType, sourceReferenceId).ifPresent(order -> {
            if (order.getStatus() == PlatformOrder.Status.CANCELLED || order.getStatus() == PlatformOrder.Status.EXPIRED) {
                return;
            }
            PlatformOrder.Status from = order.getStatus();
            order.setStatus(PlatformOrder.Status.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.getItems().forEach(item -> item.setStatus(OrderItem.Status.CANCELLED));
            order.getVouchers().forEach(voucher -> voucher.setStatus(Voucher.Status.CANCELLED));
            releaseLocks(order, InventoryLock.Status.RELEASED);
            order.addAuditLog(audit(actor, "LEGACY_CANCELLED", from.name(), order.getStatus().name(), reason));
        });
    }

    public void ensureHotelRoomAvailable(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        if (hotelId == null || roomTypeId == null || checkIn == null || checkOut == null) {
            return;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        for (long i = 0; i < nights; i++) {
            String activeLockKey = OrderItem.ProductType.HOTEL_ROOM + ":" + hotelId + ":" + roomTypeId + ":" + checkIn.plusDays(i);
            releaseExpiredActiveLock(activeLockKey);
            if (inventoryLockRepository.existsByActiveLockKey(activeLockKey)) {
                throw new IllegalStateException("Room type is unavailable for the selected dates");
            }
        }
    }

    public boolean verifyCallbackSignature(PaymentCallbackRequest request) {
        if (request == null || !StringUtils.hasText(request.signature())) {
            return false;
        }
        String payload = request.orderNo() + "|" + request.transactionNo() + "|" + request.amount().setScale(2, RoundingMode.HALF_UP) + "|" + request.status();
        String expected = hmacSha256(payload, callbackSecret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8));
    }

    private void ensureSamePaymentCallback(PaymentTransaction existing, PaymentCallbackRequest request) {
        if (!StringUtils.hasText(existing.getProvider())
                || !existing.getProvider().equalsIgnoreCase(request.provider())
                || !sameAmount(existing.getAmount(), request.amount())
                || existing.getStatus() != paymentStatusFromCallback(request)) {
            throw new IllegalArgumentException("Payment transaction number already exists with different callback data");
        }
    }

    private PaymentTransaction.Status paymentStatusFromCallback(PaymentCallbackRequest request) {
        return "SUCCESS".equals(request.status()) ? PaymentTransaction.Status.SUCCESS : PaymentTransaction.Status.FAILED;
    }

    private OrderItem buildOrderItem(CreateOrderItemRequest request, int sortOrder) {
        OrderItem.ProductType productType = OrderItem.ProductType.valueOf(request.getProductType());
        return switch (productType) {
            case SCENIC_SPOT -> scenicSpotOrderItem(request, sortOrder);
            case HOTEL_ROOM -> hotelRoomOrderItem(request, sortOrder);
            default -> throw new IllegalArgumentException("褰撳墠鍟嗗搧绫诲瀷鏆傛湭鎺ュ叆缁熶竴涓嬪崟");
        };
    }

    private OrderItem scenicSpotOrderItem(CreateOrderItemRequest request, int sortOrder) {
        ScenicSpot spot = scenicSpotRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Spot not found"));
        int quantity = defaultQuantity(request.getQuantity());
        BigDecimal unitPrice = scenicSpotPrice(spot, request.getServiceStartDate());
        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.SCENIC_SPOT);
        item.setProductId(spot.getId());
        item.setProductName(spot.getName());
        item.setServiceStartDate(request.getServiceStartDate());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setSortOrder(sortOrder);
        return item;
    }

    private OrderItem hotelRoomOrderItem(CreateOrderItemRequest request, int sortOrder) {
        if (request.getSkuId() == null) {
            throw new IllegalArgumentException("閰掑簵鎴垮瀷涓嶈兘涓虹┖");
        }
        Hotel hotel = hotelRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Hotel not found"));
        RoomType roomType = roomTypeRepository.findByIdForUpdate(request.getSkuId())
                .orElseThrow(() -> new NoSuchElementException("Room type not found"));
        if (roomType.getHotel() == null || !hotel.getId().equals(roomType.getHotel().getId())) {
            throw new IllegalArgumentException("鎴垮瀷涓嶅睘浜庤閰掑簵");
        }
        LocalDate checkIn = request.getServiceStartDate();
        LocalDate checkOut = request.getServiceEndDate();
        long nights = checkOut == null ? 0 : ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1 || nights > 30) {
            throw new IllegalArgumentException("Invalid hotel stay dates");
        }
        ensureNoLegacyHotelBookingOverlap(roomType, checkIn, checkOut);
        BigDecimal roomPrice = defaultMoney(roomType.getPrice());
        int quantity = defaultQuantity(request.getQuantity());
        BigDecimal subtotal = roomPrice.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(quantity));

        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.HOTEL_ROOM);
        item.setProductId(hotel.getId());
        item.setSkuId(roomType.getId());
        item.setProductName(hotel.getName());
        item.setSkuName(roomType.getName());
        item.setServiceStartDate(checkIn);
        item.setServiceEndDate(checkOut);
        item.setQuantity(quantity);
        item.setUnitPrice(roomPrice);
        item.setSubtotal(subtotal);
        item.setSortOrder(sortOrder);
        return item;
    }

    private void ensureNoLegacyHotelBookingOverlap(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        List<HotelBooking> overlapping = hotelBookingRepository.findOverlappingActiveBookingsForUpdate(
                roomType.getId(), ACTIVE_HOTEL_BOOKING_STATUSES, checkIn, checkOut);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Room type is unavailable for the selected dates");
        }
    }

    private void attachCancellationPolicy(OrderItem item) {
        cancellationPolicyRepository.findFirstByProductTypeAndActiveTrueOrderByPriorityDesc(item.getProductType())
                .map(CancellationPolicy::getId)
                .ifPresent(item::setCancellationPolicyId);
    }

    private void createInventoryLocks(PlatformOrder order) {
        for (OrderItem item : order.getItems()) {
            for (LocalDate serviceDate : lockDates(item)) {
                createInventoryLock(order, item, serviceDate);
            }
        }
    }

    private void createInventoryLock(PlatformOrder order, OrderItem item, LocalDate serviceDate) {
        InventoryLock lock = new InventoryLock();
        lock.setOrder(order);
        lock.setOrderItem(item);
        lock.setProductType(item.getProductType());
        lock.setProductId(item.getProductId());
        lock.setSkuId(item.getSkuId());
        lock.setServiceDate(serviceDate);
        lock.setQuantity(item.getQuantity());
        lock.setExpiresAt(order.getExpiresAt());
        String activeLockKey = activeLockKey(item, serviceDate);
        lock.setActiveLockKey(activeLockKey);
        if (StringUtils.hasText(activeLockKey)) {
            releaseExpiredActiveLock(activeLockKey);
            if (inventoryLockRepository.existsByActiveLockKey(activeLockKey)) {
                throw new IllegalStateException("Room type is unavailable for the selected dates");
            }
        } else if (item.getProductType() == OrderItem.ProductType.SCENIC_SPOT) {
            ensureScenicCapacityAvailable(item, serviceDate);
        }
        try {
            inventoryLockRepository.saveAndFlush(lock);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Room type is unavailable for the selected dates", exception);
        }
    }

    private List<LocalDate> lockDates(OrderItem item) {
        if (item.getServiceStartDate() == null) {
            return List.of();
        }
        if (item.getProductType() == OrderItem.ProductType.HOTEL_ROOM && item.getServiceEndDate() != null) {
            long nights = ChronoUnit.DAYS.between(item.getServiceStartDate(), item.getServiceEndDate());
            return java.util.stream.LongStream.range(0, nights)
                    .mapToObj(item.getServiceStartDate()::plusDays)
                    .toList();
        }
        return List.of(item.getServiceStartDate());
    }

    private String activeLockKey(OrderItem item, LocalDate serviceDate) {
        if (item.getProductType() != OrderItem.ProductType.HOTEL_ROOM || serviceDate == null || item.getSkuId() == null) {
            return null;
        }
        return item.getProductType() + ":" + item.getProductId() + ":" + item.getSkuId() + ":" + serviceDate;
    }

    private void releaseExpiredActiveLock(String activeLockKey) {
        inventoryLockRepository.findByActiveLockKey(activeLockKey).ifPresent(existing -> {
            if (existing.getStatus() == InventoryLock.Status.LOCKED
                    && existing.getExpiresAt() != null
                    && existing.getExpiresAt().isBefore(LocalDateTime.now())) {
                existing.setStatus(InventoryLock.Status.EXPIRED);
                existing.setActiveLockKey(null);
                inventoryLockRepository.saveAndFlush(existing);
            }
        });
    }

    private void ensureScenicCapacityAvailable(OrderItem item, LocalDate serviceDate) {
        if (serviceDate == null || item.getProductId() == null) {
            return;
        }
        releaseExpiredProductDateLocks(item.getProductType(), item.getProductId(), serviceDate);
        ScenicSpot spot = scenicSpotRepository.findByIdForUpdate(item.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Spot not found"));
        Integer capacity = spot.getNum();
        if (capacity == null || capacity <= 0) {
            return;
        }
        int reserved = inventoryLockRepository.findActiveProductDateLocksForUpdate(
                        item.getProductType(),
                        item.getProductId(),
                        serviceDate,
                        ACTIVE_INVENTORY_STATUSES)
                .stream()
                .map(InventoryLock::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
        int requested = defaultQuantity(item.getQuantity());
        if (reserved + requested > capacity) {
            throw new IllegalStateException("Scenic spot capacity is unavailable for the selected date");
        }
    }

    private void releaseExpiredProductDateLocks(OrderItem.ProductType productType, Long productId, LocalDate serviceDate) {
        inventoryLockRepository.findActiveProductDateLocksForUpdate(
                        productType,
                        productId,
                        serviceDate,
                        Set.of(InventoryLock.Status.LOCKED))
                .forEach(lock -> {
                    if (lock.getExpiresAt() != null && lock.getExpiresAt().isBefore(LocalDateTime.now())) {
                        lock.setStatus(InventoryLock.Status.EXPIRED);
                        lock.setActiveLockKey(null);
                        inventoryLockRepository.save(lock);
                    }
                });
    }

    private PlatformOrder expireIfNeeded(PlatformOrder order) {
        if (order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT
                && order.getExpiresAt() != null
                && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            transition(order, null, PlatformOrder.Status.EXPIRED, "Inventory lock expired");
            order.getItems().forEach(item -> item.setStatus(OrderItem.Status.EXPIRED));
            releaseLocks(order, InventoryLock.Status.EXPIRED);
        }
        return order;
    }

    @Scheduled(fixedDelayString = "${app.orders.expiry-sweep-delay-ms:60000}")
    @Transactional
    public void expirePendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.findByStatusAndExpiresAtBeforeForUpdate(PlatformOrder.Status.PENDING_PAYMENT, now)
                .forEach(this::expireIfNeeded);
        inventoryLockRepository.findExpiredLocks(InventoryLock.Status.LOCKED, now)
                .forEach(lock -> {
                    lock.setStatus(InventoryLock.Status.EXPIRED);
                    lock.setActiveLockKey(null);
                    inventoryLockRepository.save(lock);
                });
    }

    private void confirmPaidOrder(PlatformOrder order, String note) {
        if (!canConfirmPayment(order)) {
            order.addAuditLog(audit(null, "PAYMENT_CONFIRM_SKIPPED",
                    order.getStatus().name(), order.getStatus().name(), "Order is not payable"));
            return;
        }
        PlatformOrder.Status from = order.getStatus();
        order.setStatus(PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setConfirmedAt(LocalDateTime.now());
        order.getItems().forEach(item -> item.setStatus(OrderItem.Status.CONFIRMED));
        releaseLocks(order, InventoryLock.Status.CONFIRMED);
        issueVouchers(order);
        order.addAuditLog(audit(null, "PAYMENT_CONFIRMED", from.name(), order.getStatus().name(), note));
    }

    private boolean canConfirmPayment(PlatformOrder order) {
        return order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT
                && order.getPaymentStatus() != PlatformOrder.PaymentStatus.PAID;
    }

    private void issueVouchers(PlatformOrder order) {
        if (!order.getVouchers().isEmpty()) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            Voucher voucher = new Voucher();
            voucher.setOrderItem(item);
            voucher.setVoucherCode(nextBusinessNo("VCH"));
            voucher.setValidFrom(item.getServiceStartDate());
            voucher.setValidUntil(item.getServiceEndDate() == null ? item.getServiceStartDate() : item.getServiceEndDate());
            order.addVoucher(voucher);
        }
    }

    private void releaseLocks(PlatformOrder order, InventoryLock.Status status) {
        inventoryLockRepository.findByOrder(order).forEach(lock -> {
            lock.setStatus(status);
            if (status == InventoryLock.Status.RELEASED || status == InventoryLock.Status.EXPIRED) {
                lock.setActiveLockKey(null);
            }
            inventoryLockRepository.save(lock);
        });
    }

    private RefundOrder buildRefund(PlatformOrder order, OrderItem item, BigDecimal amount, String reason) {
        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(nextBusinessNo("RFD"));
        refund.setOrderItem(item);
        refund.setAmount(amount);
        refund.setReason(InputSanitizer.optionalTextBlock(reason, 500, "refund reason"));
        return refund;
    }

    private void ensureRefundAllowed(PlatformOrder order, OrderItem item, BigDecimal requestedAmount) {
        if (!isRefundableOrder(order)) {
            throw new IllegalStateException("Order is not refundable");
        }
        if (hasPendingRefund(order, item)) {
            throw new IllegalStateException("Refund already pending");
        }
        BigDecimal amount = defaultMoney(requestedAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Refund amount must be greater than zero");
        }
        BigDecimal remaining = refundableAmount(order).subtract(countedRefundAmount(order));
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalStateException("Refund amount exceeds paid amount");
        }
    }

    private boolean isRefundableOrder(PlatformOrder order) {
        boolean paid = order.getPaymentStatus() == PlatformOrder.PaymentStatus.PAID
                || order.getPaymentStatus() == PlatformOrder.PaymentStatus.PARTIALLY_REFUNDED;
        boolean active = order.getStatus() == PlatformOrder.Status.CONFIRMED
                || order.getStatus() == PlatformOrder.Status.PAID;
        return paid && active;
    }

    private boolean hasPendingRefund(PlatformOrder order, OrderItem item) {
        return order.getRefunds().stream()
                .filter(refund -> PENDING_REFUND_STATUSES.contains(refund.getStatus()))
                .anyMatch(refund -> item == null
                        || refund.getOrderItem() == null
                        || refund.getOrderItem() == item
                        || (refund.getOrderItem().getId() != null && refund.getOrderItem().getId().equals(item.getId())));
    }

    private BigDecimal countedRefundAmount(PlatformOrder order) {
        return order.getRefunds().stream()
                .filter(refund -> COUNTED_REFUND_STATUSES.contains(refund.getStatus()))
                .map(refund -> defaultMoney(refund.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal refundableAmount(PlatformOrder order) {
        BigDecimal orderAmount = defaultMoney(order.getPayableAmount());
        BigDecimal paidAmount = order.getPaymentTransactions().stream()
                .filter(transaction -> transaction.getStatus() == PaymentTransaction.Status.SUCCESS)
                .map(transaction -> defaultMoney(transaction.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0 && order.getPaidAt() != null) {
            paidAmount = orderAmount;
        }
        if (orderAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(orderAmount) > 0) {
            return orderAmount;
        }
        return paidAmount;
    }

    private BigDecimal refundAmountForOrder(PlatformOrder order) {
        return order.getPayableAmount();
    }

    private BigDecimal refundAmountForItem(OrderItem item) {
        return item.getSubtotal();
    }

    private OrderItem findOrderItem(PlatformOrder order, Long itemId) {
        if (itemId == null) {
            return null;
        }
        return order.getItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Order item not found"));
    }

    private boolean isClosedOrder(PlatformOrder order) {
        return order.getStatus() == PlatformOrder.Status.CANCELLED
                || order.getStatus() == PlatformOrder.Status.EXPIRED
                || order.getStatus() == PlatformOrder.Status.REFUNDED;
    }

    private PlatformOrder confirmedLegacyOrder(User user, String idempotencyKey, String sourceType, Long sourceReferenceId) {
        PlatformOrder order = new PlatformOrder();
        order.setOrderNo(nextBusinessNo("ORD"));
        order.setUser(user);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(PlatformOrder.Status.CONFIRMED);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setConfirmedAt(LocalDateTime.now());
        order.setSourceType(sourceType);
        order.setSourceReferenceId(sourceReferenceId);
        order.addAuditLog(audit(user, "LEGACY_MIRROR_CREATED", null, order.getStatus().name(), "鏃ч璁㈠啓鍏ョ粺涓€璁㈠崟涓績"));
        return order;
    }

    private PlatformOrder legacyOrder(User user, String idempotencyKey,
                                      String sourceType, Long sourceReferenceId,
                                      boolean confirmed) {
        return confirmed
                ? confirmedLegacyOrder(user, idempotencyKey, sourceType, sourceReferenceId)
                : pendingLegacyOrder(user, idempotencyKey, sourceType, sourceReferenceId);
    }

    private PlatformOrder pendingLegacyOrder(User user, String idempotencyKey, String sourceType, Long sourceReferenceId) {
        PlatformOrder order = new PlatformOrder();
        order.setOrderNo(nextBusinessNo("ORD"));
        order.setUser(user);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(PlatformOrder.Status.PENDING_PAYMENT);
        order.setPaymentStatus(PlatformOrder.PaymentStatus.UNPAID);
        order.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        order.setExpiresAt(order.getLockedUntil());
        order.setSourceType(sourceType);
        order.setSourceReferenceId(sourceReferenceId);
        order.addAuditLog(audit(user, "LEGACY_MIRROR_CREATED", null, order.getStatus().name(), "Legacy booking mirrored into order center, pending payment"));
        return order;
    }

    private void addLegacyPaymentAndVoucher(PlatformOrder order, OrderItem item) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo(nextBusinessNo("LEGACY_PAY"));
        transaction.setProvider("LEGACY");
        transaction.setAmount(order.getPayableAmount());
        transaction.setStatus(PaymentTransaction.Status.SUCCESS);
        transaction.setSignatureValid(true);
        transaction.setPaidAt(LocalDateTime.now());
        order.addPaymentTransaction(transaction);

        Voucher voucher = new Voucher();
        voucher.setOrderItem(item);
        voucher.setVoucherCode(nextBusinessNo("VCH"));
        voucher.setValidFrom(item.getServiceStartDate());
        voucher.setValidUntil(item.getServiceEndDate() == null ? item.getServiceStartDate() : item.getServiceEndDate());
        order.addVoucher(voucher);
    }

    private void transition(PlatformOrder order, User actor, PlatformOrder.Status toStatus, String note) {
        PlatformOrder.Status from = order.getStatus();
        order.setStatus(toStatus);
        order.addAuditLog(audit(actor, "STATUS_CHANGED", from == null ? null : from.name(), toStatus.name(), note));
    }

    private OrderAuditLog audit(User actor, String action, String fromStatus, String toStatus, String note) {
        OrderAuditLog auditLog = new OrderAuditLog();
        auditLog.setActorUser(actor);
        auditLog.setAction(action);
        auditLog.setFromStatus(fromStatus);
        auditLog.setToStatus(toStatus);
        auditLog.setNote(note);
        return auditLog;
    }

    private BigDecimal scenicSpotPrice(ScenicSpot spot, LocalDate visitDate) {
        BigDecimal unitPrice = defaultMoney(spot.getTicketPrice());
        int month = visitDate.getMonthValue();
        if (month >= 1 && month <= 3) {
            return BigDecimal.ZERO;
        }
        MonthDay md = MonthDay.from(visitDate);
        boolean isPeakSeason = !md.isBefore(MonthDay.of(5, 1)) && !md.isAfter(MonthDay.of(10, 31));
        if (isPeakSeason && spot.getPeakSeasonPrice() != null) {
            return spot.getPeakSeasonPrice();
        }
        if (!isPeakSeason && spot.getOffSeasonPrice() != null) {
            return spot.getOffSeasonPrice();
        }
        return unitPrice;
    }

    private String buildSummary(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "鏃呰璁㈠崟";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " and " + items.size() + " items";
    }

    private boolean sameAmount(BigDecimal expected, BigDecimal actual) {
        return expected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(actual.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private String callbackPayloadSummary(PaymentCallbackRequest request) {
        return "orderNo=" + request.orderNo() + ",transactionNo=" + request.transactionNo()
                + ",amount=" + request.amount() + ",status=" + request.status();
    }

    private String normalizeIdempotencyKey(String value) {
        return InputSanitizer.optionalPlainText(value, 96, "idempotency key");
    }

    private int defaultQuantity(Integer quantity) {
        return quantity == null ? 1 : quantity;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextBusinessNo(String prefix) {
        byte[] randomBytes = new byte[11];
        SECURE_RANDOM.nextBytes(randomBytes);
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + HexFormat.of().formatHex(randomBytes).toUpperCase();
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign payment payload", e);
        }
    }

    private OrderResponse toResponse(PlatformOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getCurrency(),
                order.getProductSummary(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPayableAmount(),
                order.getCustomerName(),
                PiiMasker.maskPhone(order.getCustomerPhone()),
                order.getCustomerNote(),
                order.getSourceType(),
                order.getSourceReferenceId(),
                order.getLockedUntil(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getConfirmedAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getPaymentTransactions().stream().map(this::toPaymentResponse).toList(),
                order.getRefunds().stream().map(this::toRefundResponse).toList(),
                order.getVouchers().stream().map(this::toVoucherResponse).toList(),
                order.getInvoices().stream().map(this::toInvoiceResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductType().name(),
                item.getProductId(),
                item.getSkuId(),
                item.getProductName(),
                item.getSkuName(),
                item.getServiceStartDate(),
                item.getServiceEndDate(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getStatus().name(),
                item.getLegacyReferenceType(),
                item.getLegacyReferenceId()
        );
    }

    private PaymentTransactionResponse toPaymentResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getTransactionNo(),
                transaction.getProvider(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getSignatureValid(),
                transaction.getPaidAt(),
                transaction.getCreatedAt()
        );
    }

    private RefundResponse toRefundResponse(RefundOrder refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getOrderItem() == null ? null : refund.getOrderItem().getId(),
                refund.getRefundNo(),
                refund.getAmount(),
                refund.getStatus().name(),
                refund.getReason(),
                refund.getRequestedAt(),
                refund.getProcessedAt()
        );
    }

    private VoucherResponse toVoucherResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getOrderItem() == null ? null : voucher.getOrderItem().getId(),
                voucher.getVoucherCode(),
                voucher.getStatus().name(),
                voucher.getValidFrom(),
                voucher.getValidUntil(),
                voucher.getIssuedAt()
        );
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getInvoiceTitle(),
                invoice.getTaxNo(),
                invoice.getAmount(),
                invoice.getStatus().name(),
                invoice.getRequestedAt(),
                invoice.getIssuedAt()
        );
    }
}
