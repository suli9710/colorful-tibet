package com.tibet.tourism.modules.order.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class InvoiceRequest {
    @NotBlank
    @Size(max = 160)
    private String invoiceTitle;

    @Size(max = 64)
    @Pattern(regexp = "^$|^[A-Z0-9]{6,64}$", message = "\u7a0e\u53f7\u683c\u5f0f\u4e0d\u5408\u6cd5")
    private String taxNo;

    public String getInvoiceTitle() { return invoiceTitle; }
    public void setInvoiceTitle(String invoiceTitle) { this.invoiceTitle = invoiceTitle; }

    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
}
