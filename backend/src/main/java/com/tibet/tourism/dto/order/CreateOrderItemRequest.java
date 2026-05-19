package com.tibet.tourism.dto.order;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class CreateOrderItemRequest {
    @NotBlank
    @Pattern(regexp = "^(SCENIC_SPOT|HOTEL_ROOM|EXPERIENCE|CAR|GUIDE|ITINERARY)$")
    private String productType;

    @NotNull
    private Long productId;

    private Long skuId;

    @NotNull
    @FutureOrPresent
    private LocalDate serviceStartDate;

    private LocalDate serviceEndDate;

    @NotNull
    @Min(1)
    @Max(30)
    private Integer quantity;

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public LocalDate getServiceStartDate() { return serviceStartDate; }
    public void setServiceStartDate(LocalDate serviceStartDate) { this.serviceStartDate = serviceStartDate; }

    public LocalDate getServiceEndDate() { return serviceEndDate; }
    public void setServiceEndDate(LocalDate serviceEndDate) { this.serviceEndDate = serviceEndDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
