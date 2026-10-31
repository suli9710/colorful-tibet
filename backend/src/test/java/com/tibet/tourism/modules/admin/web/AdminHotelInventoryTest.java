package com.tibet.tourism.modules.admin.web;

import com.tibet.tourism.modules.hotel.domain.RoomType;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.hotel.infra.HotelRepository;
import com.tibet.tourism.modules.hotel.infra.RoomTypeRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminHotelInventoryTest {
    @Test
    void inventoryChangesUseSameRoomRowLockAsReservations() {
        var hotels = mock(HotelRepository.class);
        var rooms = mock(RoomTypeRepository.class);
        var bookings = mock(HotelBookingRepository.class);
        RoomType room = new RoomType();
        room.setId(1L);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(rooms.save(room)).thenReturn(room);
        var controller = new AdminHotelController(hotels, rooms, bookings);
        var result = controller.updateRoomType(1L, new AdminHotelController.RoomTypeRequest(
                "Twin", BigDecimal.TEN, 2, null, null, 0, 7));
        assertEquals(200, result.getStatusCode().value());
        assertEquals(7, room.getInventoryUnits());
        verify(rooms).findByIdForUpdate(1L);
        verify(rooms, never()).findById(any());
    }

    @Test
    void unknownStockIsNotInventedOnCreate() {
        var request = new AdminHotelController.RoomTypeRequest("Twin", BigDecimal.TEN, 2, null, null, 0);
        assertNull(request.inventoryUnits());
    }
}
