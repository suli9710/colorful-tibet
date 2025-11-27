package com.tibet.tourism.service;

import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    public Booking createBooking(Booking booking) {
        booking.setStatus(Booking.Status.PENDING);
        return bookingRepository.save(booking);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
}
