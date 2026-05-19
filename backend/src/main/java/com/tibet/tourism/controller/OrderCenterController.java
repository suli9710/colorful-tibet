package com.tibet.tourism.controller;

import com.tibet.tourism.dto.order.*;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.security.JwtAuthSupport;
import com.tibet.tourism.service.OrderCenterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class OrderCenterController {

    private final OrderCenterService orderCenterService;
    private final JwtAuthSupport jwtAuthSupport;

    public OrderCenterController(OrderCenterService orderCenterService, JwtAuthSupport jwtAuthSupport) {
        this.orderCenterService = orderCenterService;
        this.jwtAuthSupport = jwtAuthSupport;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                         HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.createOrder(user, request, idempotencyKey));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单商品不存在"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "订单参数不合法"));
        }
    }

    @GetMapping("/api/orders/my")
    public ResponseEntity<?> myOrders(HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        return ResponseEntity.ok(orderCenterService.getMyOrders(user));
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.ok(orderCenterService.getOrder(user, id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        }
    }

    @PostMapping("/api/orders/{id}/cancel")
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
    public ResponseEntity<?> requestRefund(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) RefundRequest request,
                                           HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.requestRefund(user, id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/orders/{id}/invoice")
    public ResponseEntity<?> requestInvoice(@PathVariable Long id,
                                            @Valid @RequestBody InvoiceRequest request,
                                            HttpServletRequest httpRequest) {
        User user = jwtAuthSupport.resolveCurrentUser(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderCenterService.requestInvoice(user, id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/payments/callbacks/mock")
    public ResponseEntity<?> paymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        try {
            return ResponseEntity.ok(orderCenterService.handlePaymentCallback(request));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "支付回调签名无效"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "订单不存在"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
