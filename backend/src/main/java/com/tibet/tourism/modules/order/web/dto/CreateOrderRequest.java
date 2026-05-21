package com.tibet.tourism.modules.order.web.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public class CreateOrderRequest {
    @Size(max = 96)
    private String idempotencyKey;

    @Size(max = 64)
    private String customerName;

    @Size(max = 32)
    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{6,32}$", message = "手机号格式不合法")
    private String customerPhone;

    @Size(max = 500)
    private String customerNote;

    @NotEmpty
    @Valid
    private List<CreateOrderItemRequest> items;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }

    public List<CreateOrderItemRequest> getItems() { return items; }
    public void setItems(List<CreateOrderItemRequest> items) { this.items = items; }
}
