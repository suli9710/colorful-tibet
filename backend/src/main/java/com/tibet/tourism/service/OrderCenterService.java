package com.tibet.tourism.service;

import com.tibet.tourism.dto.order.*;
import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import com.tibet.tourism.security.InputSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderCenterService {

    private static final int LOCK_MINUTES = 15;

    private final PlatformOrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final ScenicSpotRepository scenicSpotRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Value("${app.payments.mock-callback-secret:dev-payment-callback-secret}")
    private String callbackSecret;

    public OrderCenterService(PlatformOrderRepository orderRepository,
                              PaymentTransactionRepository paymentTransactionRepository,
                              CancellationPolicyRepository cancellationPolicyRepository,
                              InventoryLockRepository inventoryLockRepository,
                              ScenicSpotRepository scenicSpotRepository,
                              HotelRepository hotelRepository,
                              RoomTypeRepository roomTypeRepository) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.inventoryLockRepository = inventoryLockRepository;
        this.scenicSpotRepository = scenicSpotRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
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
        order.setCustomerName(InputSanitizer.optionalPlainText(request.getCustomerName(), 64, "联系人"));
        order.setCustomerPhone(InputSanitizer.optionalPlainText(request.getCustomerPhone(), 32, "手机号"));
        order.setCustomerNote(InputSanitizer.optionalTextBlock(request.getCustomerNote(), 500, "备注"));
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
        order.addAuditLog(audit(user, "CREATE", null, order.getStatus().name(), "统一订单创建并锁定库存"));

        PlatformOrder saved = orderRepository.save(order);
        createInventoryLocks(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse getOrder(User user, Long id) {
        PlatformOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return toResponse(expireIfNeeded(order));
    }

    @Transactional
    public OrderResponse cancelOrder(User user, Long id, CancelOrderRequest request) {
        PlatformOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        expireIfNeeded(order);
        if (order.getStatus() == PlatformOrder.Status.CANCELLED || order.getStatus() == PlatformOrder.Status.EXPIRED) {
            return toResponse(order);
        }
        if (order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT) {
            transition(order, user, PlatformOrder.Status.CANCELLED,
                    InputSanitizer.optionalTextBlock(request == null ? null : request.getReason(), 500, "取消原因"));
            releaseLocks(order, InventoryLock.Status.RELEASED);
            order.setCancelledAt(LocalDateTime.now());
            order.getItems().forEach(item -> item.setStatus(OrderItem.Status.CANCELLED));
            return toResponse(order);
        }

        RefundOrder refund = buildRefund(order, null, refundAmountForOrder(order),
                request == null ? null : request.getReason());
        order.addRefund(refund);
        transition(order, user, PlatformOrder.Status.REFUND_PENDING, "已确认订单取消，等待退款处理");
        order.setCancelledAt(LocalDateTime.now());
        order.getItems().forEach(item -> item.setStatus(OrderItem.Status.REFUND_PENDING));
        return toResponse(order);
    }

    @Transactional
    public RefundResponse requestRefund(User user, Long id, RefundRequest request) {
        PlatformOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (order.getPaymentStatus() == PlatformOrder.PaymentStatus.UNPAID) {
            throw new IllegalStateException("未支付订单不能退款");
        }
        OrderItem item = findOrderItem(order, request == null ? null : request.getOrderItemId());
        BigDecimal amount = item == null ? refundAmountForOrder(order) : refundAmountForItem(item);
        RefundOrder refund = buildRefund(order, item, amount, request == null ? null : request.getReason());
        order.addRefund(refund);
        transition(order, user, PlatformOrder.Status.REFUND_PENDING, "用户发起退款");
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
            throw new IllegalStateException("未支付订单不能开票");
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(nextBusinessNo("INV"));
        invoice.setInvoiceTitle(InputSanitizer.requiredPlainText(request.getInvoiceTitle(), 160, "发票抬头"));
        invoice.setTaxNo(InputSanitizer.optionalPlainText(request.getTaxNo(), 64, "税号"));
        invoice.setAmount(order.getPayableAmount());
        invoice.setStatus(Invoice.Status.REQUESTED);
        order.addInvoice(invoice);
        order.addAuditLog(audit(user, "INVOICE_REQUESTED", order.getStatus().name(), order.getStatus().name(), "用户申请开票"));
        return toInvoiceResponse(invoice);
    }

    @Transactional
    public void deleteClosedOrder(User user, Long id) {
        PlatformOrder order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        expireIfNeeded(order);
        if (!isClosedOrder(order)) {
            throw new IllegalStateException("仅已关闭订单可以删除");
        }

        inventoryLockRepository.deleteAll(inventoryLockRepository.findByOrder(order));
        orderRepository.delete(order);
    }

    @Transactional
    public OrderResponse handlePaymentCallback(PaymentCallbackRequest request) {
        PlatformOrder order = orderRepository.findByOrderNo(request.orderNo())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        boolean signatureValid = verifyCallbackSignature(request);
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo(InputSanitizer.requiredPlainText(request.transactionNo(), 64, "交易号"));
        transaction.setProvider(InputSanitizer.requiredPlainText(request.provider(), 32, "支付渠道"));
        transaction.setAmount(request.amount());
        transaction.setSignatureValid(signatureValid);
        transaction.setCallbackPayload(callbackPayloadSummary(request));

        if (!signatureValid) {
            transaction.setStatus(PaymentTransaction.Status.FAILED);
            order.addPaymentTransaction(transaction);
            order.addAuditLog(audit(null, "PAYMENT_CALLBACK_REJECTED", order.getStatus().name(), order.getStatus().name(), "支付回调签名无效"));
            throw new SecurityException("Invalid payment signature");
        }

        if (!sameAmount(order.getPayableAmount(), request.amount())) {
            transaction.setStatus(PaymentTransaction.Status.FAILED);
            order.addPaymentTransaction(transaction);
            order.addAuditLog(audit(null, "PAYMENT_AMOUNT_MISMATCH", order.getStatus().name(), order.getStatus().name(), "支付金额不一致"));
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        transaction.setStatus("SUCCESS".equals(request.status()) ? PaymentTransaction.Status.SUCCESS : PaymentTransaction.Status.FAILED);
        transaction.setPaidAt(LocalDateTime.now());
        order.addPaymentTransaction(transaction);

        if (transaction.getStatus() == PaymentTransaction.Status.SUCCESS) {
            confirmPaidOrder(order, "支付回调验签成功");
        } else {
            order.setPaymentStatus(PlatformOrder.PaymentStatus.FAILED);
            order.addAuditLog(audit(null, "PAYMENT_FAILED", order.getStatus().name(), order.getStatus().name(), "支付失败回调"));
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
        return orderRepository.save(order);
    }

    @Transactional
    public PlatformOrder createFromLegacyHotelBooking(HotelBooking booking) {
        String idempotencyKey = "LEGACY_HOTEL_" + booking.getId();
        Optional<PlatformOrder> existing = orderRepository.findByUserIdAndIdempotencyKey(booking.getUser().getId(), idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean confirmed = booking.getStatus() == HotelBooking.Status.CONFIRMED;
        PlatformOrder order = legacyOrder(booking.getUser(), idempotencyKey,
                "LEGACY_HOTEL_BOOKING", booking.getId(), confirmed);
        order.setCustomerName(booking.getGuestName());
        order.setCustomerPhone(booking.getPhone());
        order.setCustomerNote(booking.getNote());
        order.setProductSummary(booking.getHotel().getName() + " · " + booking.getRoomName());
        order.setTotalAmount(defaultMoney(booking.getTotalPrice()));
        order.setPayableAmount(order.getTotalAmount());

        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.HOTEL_ROOM);
        item.setProductId(booking.getHotel().getId());
        item.setProductName(booking.getHotel().getName());
        item.setSkuName(booking.getRoomName());
        item.setServiceStartDate(booking.getCheckInDate());
        item.setServiceEndDate(booking.getCheckOutDate());
        item.setQuantity(booking.getGuests());
        item.setUnitPrice(defaultMoney(booking.getRoomPrice()));
        item.setSubtotal(defaultMoney(booking.getTotalPrice()));
        item.setStatus(confirmed ? OrderItem.Status.CONFIRMED : OrderItem.Status.LOCKED);
        item.setLegacyReferenceType("HOTEL_BOOKING");
        item.setLegacyReferenceId(booking.getId());
        order.addItem(item);
        if (confirmed) {
            addLegacyPaymentAndVoucher(order, item);
        }
        return orderRepository.save(order);
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
            order.addAuditLog(audit(actor, "LEGACY_CANCELLED", from.name(), order.getStatus().name(), reason));
        });
    }

    public boolean verifyCallbackSignature(PaymentCallbackRequest request) {
        String payload = request.orderNo() + "|" + request.transactionNo() + "|" + request.amount().setScale(2, RoundingMode.HALF_UP) + "|" + request.status();
        String expected = hmacSha256(payload, callbackSecret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8));
    }

    private OrderItem buildOrderItem(CreateOrderItemRequest request, int sortOrder) {
        OrderItem.ProductType productType = OrderItem.ProductType.valueOf(request.getProductType());
        return switch (productType) {
            case SCENIC_SPOT -> scenicSpotOrderItem(request, sortOrder);
            case HOTEL_ROOM -> hotelRoomOrderItem(request, sortOrder);
            default -> throw new IllegalArgumentException("当前商品类型暂未接入统一下单");
        };
    }

    private OrderItem scenicSpotOrderItem(CreateOrderItemRequest request, int sortOrder) {
        ScenicSpot spot = scenicSpotRepository.findById(request.getProductId())
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
            throw new IllegalArgumentException("酒店房型不能为空");
        }
        Hotel hotel = hotelRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Hotel not found"));
        RoomType roomType = roomTypeRepository.findById(request.getSkuId())
                .orElseThrow(() -> new NoSuchElementException("Room type not found"));
        if (roomType.getHotel() == null || !hotel.getId().equals(roomType.getHotel().getId())) {
            throw new IllegalArgumentException("房型不属于该酒店");
        }
        LocalDate checkIn = request.getServiceStartDate();
        LocalDate checkOut = request.getServiceEndDate();
        long nights = checkOut == null ? 0 : ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1 || nights > 30) {
            throw new IllegalArgumentException("酒店入住日期不合法");
        }
        BigDecimal roomPrice = defaultMoney(roomType.getPrice());
        BigDecimal subtotal = roomPrice.multiply(BigDecimal.valueOf(nights));
        BigDecimal serviceFee = subtotal.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);

        OrderItem item = new OrderItem();
        item.setProductType(OrderItem.ProductType.HOTEL_ROOM);
        item.setProductId(hotel.getId());
        item.setSkuId(roomType.getId());
        item.setProductName(hotel.getName());
        item.setSkuName(roomType.getName());
        item.setServiceStartDate(checkIn);
        item.setServiceEndDate(checkOut);
        item.setQuantity(defaultQuantity(request.getQuantity()));
        item.setUnitPrice(roomPrice);
        item.setSubtotal(subtotal.add(serviceFee));
        item.setSortOrder(sortOrder);
        return item;
    }

    private void attachCancellationPolicy(OrderItem item) {
        cancellationPolicyRepository.findFirstByProductTypeAndActiveTrueOrderByPriorityDesc(item.getProductType())
                .map(CancellationPolicy::getId)
                .ifPresent(item::setCancellationPolicyId);
    }

    private void createInventoryLocks(PlatformOrder order) {
        for (OrderItem item : order.getItems()) {
            InventoryLock lock = new InventoryLock();
            lock.setOrder(order);
            lock.setOrderItem(item);
            lock.setProductType(item.getProductType());
            lock.setProductId(item.getProductId());
            lock.setSkuId(item.getSkuId());
            lock.setServiceDate(item.getServiceStartDate());
            lock.setQuantity(item.getQuantity());
            lock.setExpiresAt(order.getExpiresAt());
            inventoryLockRepository.save(lock);
        }
    }

    private PlatformOrder expireIfNeeded(PlatformOrder order) {
        if (order.getStatus() == PlatformOrder.Status.PENDING_PAYMENT
                && order.getExpiresAt() != null
                && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            transition(order, null, PlatformOrder.Status.EXPIRED, "库存锁超时释放");
            order.getItems().forEach(item -> item.setStatus(OrderItem.Status.EXPIRED));
            releaseLocks(order, InventoryLock.Status.EXPIRED);
        }
        return order;
    }

    private void confirmPaidOrder(PlatformOrder order, String note) {
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
            inventoryLockRepository.save(lock);
        });
    }

    private RefundOrder buildRefund(PlatformOrder order, OrderItem item, BigDecimal amount, String reason) {
        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(nextBusinessNo("RFD"));
        refund.setOrderItem(item);
        refund.setAmount(amount);
        refund.setReason(InputSanitizer.optionalTextBlock(reason, 500, "退款原因"));
        return refund;
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
        order.addAuditLog(audit(user, "LEGACY_MIRROR_CREATED", null, order.getStatus().name(), "旧预订写入统一订单中心"));
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
        order.addAuditLog(audit(user, "LEGACY_MIRROR_CREATED", null, order.getStatus().name(), "旧预订写入统一订单中心，等待支付"));
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
            return "旅行订单";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " 等 " + items.size() + " 项";
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
        return InputSanitizer.optionalPlainText(value, 96, "幂等键");
    }

    private int defaultQuantity(Integer quantity) {
        return quantity == null ? 1 : quantity;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextBusinessNo(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
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
                order.getCustomerPhone(),
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
