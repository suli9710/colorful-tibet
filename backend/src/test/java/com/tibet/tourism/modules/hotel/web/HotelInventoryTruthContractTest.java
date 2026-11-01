package com.tibet.tourism.modules.hotel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.web.dto.PublicRoomTypeResponse;
import com.tibet.tourism.modules.order.web.dto.RefundResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HotelInventoryTruthContractTest {
    @Test
    void guestCapacityNeverImpliesKnownRoomInventory() {
        RoomType room = new RoomType();
        room.setCapacity(4);
        room.setPrice(BigDecimal.TEN);
        var response = PublicRoomTypeResponse.from(room);
        assertNull(response.inventoryUnits());
        assertEquals("UNKNOWN", response.inventoryStatus());
        assertEquals("REFERENCE", response.priceStatus());
        assertEquals("SUPPLIER_CONFIRMATION_REQUIRED", response.fulfillmentStatus());
    }

    @Test
    void configuredStockIsNotDescribedAsSupplierConfirmed() throws Exception {
        RoomType room = new RoomType();
        room.setInventoryUnits(3);
        var response = PublicRoomTypeResponse.from(room);
        assertEquals("CONFIGURED", response.inventoryStatus());
        assertEquals("UNAVAILABLE", response.priceStatus());
        String json = new ObjectMapper().writeValueAsString(response);
        assertTrue(json.contains("\"fulfillmentStatus\":\"SUPPLIER_CONFIRMATION_REQUIRED\""));
        assertFalse(json.contains("VERIFIED_AVAILABLE"));
    }

    @Test
    void zeroStockIsSoldOutAndNullPriceNeverBecomesFree() {
        RoomType room = new RoomType();
        room.setInventoryUnits(0);
        var response = PublicRoomTypeResponse.from(room);
        assertEquals("SOLD_OUT", response.inventoryStatus());
        assertEquals("UNAVAILABLE", response.priceStatus());
    }

    @Test
    void completedRefundRecordDoesNotClaimChannelSettlement() throws Exception {
        var refund = new RefundResponse(1L, 2L, "RFD-1", BigDecimal.TEN, "COMPLETED", null, null, null);
        assertEquals("MANUAL_RECORD_ONLY", refund.settlementStatus());
        assertTrue(new ObjectMapper().writeValueAsString(refund).contains("\"settlementStatus\":\"MANUAL_RECORD_ONLY\""));
    }
}
