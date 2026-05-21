package com.tibet.tourism.modules.order.web.dto;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BookingRequest {

    @NotNull(message = "景点不能为空")
    private Long spotId;

    @NotNull(message = "游览日期不能为空")
    @FutureOrPresent(message = "游览日期不能早于今天")
    private LocalDate visitDate;

    @NotNull(message = "门票数量不能为空")
    @Min(value = 1, message = "门票数量不能少于1张")
    @Max(value = 20, message = "门票数量不能超过20张")
    private Integer ticketCount;

    public Long getSpotId() {
        return spotId;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }
}
