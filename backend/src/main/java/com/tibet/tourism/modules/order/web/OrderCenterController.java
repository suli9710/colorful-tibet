package com.tibet.tourism.modules.order.web;
import com.tibet.tourism.common.security.antibot.BehaviorData;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.domain.Invoice;
import com.tibet.tourism.modules.order.web.dto.CancelOrderRequest;
import com.tibet.tourism.modules.order.web.dto.CreateOrderRequest;
import com.tibet.tourism.modules.order.web.dto.InvoiceRequest;
import com.tibet.tourism.modules.order.web.dto.PaymentCallbackRequest;
import com.tibet.tourism.modules.order.web.dto.RefundRequest;
import com.tibet.tourism.modules.order.web.dto.RefundReviewRequest;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderCenterController {

    private static final String ORDER_REQUEST_FAILED = "Order request could not be processed";
    private static final String ORDER_ACTION_UNAVAILABLE = "Order action is not available";
    private static final String PAYMENT_CALLBACK_FAILED = "Payment callback could not be processed";

    private final OrderCenterService orderCenterService;
    private final JwtAuthSupport jwtAuthSupport;
    private final RiskAssessmentService riskAssessmentService;

    public OrderCenterController(OrderCenterService orderCenterService,
                                 JwtAuthSupport jwtAuthSupport,
                                 RiskAssessmentService riskAssessmentService) {
        this.orderCenterService = orderCenterService;
        this.jwtAuthSupport = jwtAuthSupport;
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping("/api/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                         @RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
                                         @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
                                         @RequestHeader(value = "X-Behavior-Data", required = false) String behaviorData,
                                         HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);

        RiskResult risk = riskAssessmentService.assess(
                recaptchaToken, fingerprint, user.getId(), behaviorData,
                httpRequest.getRemoteAddr(), "/api/orders");
        if (risk.decision() != RiskResult.Decision.ALLOW) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", risk.decision() == RiskResult.Decision.BLOCK ? "请求被安全系统拦截" : "请完成安全验证",
                    "code", "ANTIBOT_" + risk.decision().name()));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.createOrder(user, request, idempotencyKey));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单商品不存在"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", ORDER_REQUEST_FAILED));
        }
    }

    @GetMapping("/api/orders/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> myOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        return pagedContent(orderCenterService.getMyOrders(user, pageable));
    }

    @GetMapping("/api/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(orderCenterService.getOrder(user, id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        }
    }

    @PostMapping("/api/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) CancelOrderRequest request,
                                         HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(orderCenterService.cancelOrder(user, id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        }
    }

    @PostMapping("/api/orders/{id}/refunds")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestRefund(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) RefundRequest request,
                                           HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.requestRefund(user, id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ORDER_ACTION_UNAVAILABLE));
        }
    }

    @PostMapping("/api/admin/orders/{orderId}/refunds/{refundId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reviewRefund(@PathVariable Long orderId,
                                          @PathVariable Long refundId,
                                          @Valid @RequestBody RefundReviewRequest request,
                                          HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(orderCenterService.reviewRefund(user, orderId, refundId, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "退款记录不存在"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", ORDER_REQUEST_FAILED));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ORDER_ACTION_UNAVAILABLE));
        }
    }

    @PostMapping("/api/orders/{id}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestInvoice(@PathVariable Long id,
                                            @Valid @RequestBody InvoiceRequest request,
                                            HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.requestInvoice(user, id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ORDER_ACTION_UNAVAILABLE));
        }
    }

    @DeleteMapping("/api/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteClosedOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            orderCenterService.deleteClosedOrder(user, id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ORDER_ACTION_UNAVAILABLE));
        }
    }

    @PostMapping("/api/payments/callbacks/mock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> paymentCallback(
            @Valid @RequestBody PaymentCallbackRequest request,
            @RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            @RequestHeader(value = "X-Behavior-Data", required = false) String behaviorData,
            HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        RiskResult risk = riskAssessmentService.assess(
                recaptchaToken, fingerprint, user.getId(), behaviorData,
                httpRequest.getRemoteAddr(), "/api/payments/callbacks/mock");
        if (risk.decision() != RiskResult.Decision.ALLOW) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "请求被安全系统拦截",
                    "code", "ANTIBOT_" + risk.decision().name()));
        }

        try {
            return ResponseEntity.ok(orderCenterService.handleMockPaymentCallback(user, request));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "支付回调签名无效"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "支付回调未启用"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", PAYMENT_CALLBACK_FAILED));
        }
    }

    private <T> ResponseEntity<List<T>> pagedContent(Page<T> page) {
        return ResponseEntity.ok()
                .header("X-Page", String.valueOf(page.getNumber()))
                .header("X-Size", String.valueOf(page.getSize()))
                .header("X-Total-Elements", String.valueOf(page.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(page.getTotalPages()))
                .body(page.getContent());
    }
}
