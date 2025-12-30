package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final TableBookingRepository bookingRepository;
    private final TableRepository tableRepository;

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        // Validate table
        Table table = tableRepository.findByTableNumber(request.getTableNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getActive()) {
            throw new BusinessException("Table is not available for booking");
        }

        // Check capacity
        if (table.getCapacity() != null && request.getPartySize() > table.getCapacity()) {
            throw new BusinessException("Party size exceeds table capacity");
        }

        // Check for conflicts
        List<TableBooking> conflicts = bookingRepository.findConflictingBookings(
                request.getBookingDate(),
                request.getTimeSlot()
        );

        boolean hasConflict = conflicts.stream()
                .anyMatch(b -> b.getTableId().equals(table.getId()));

        if (hasConflict) {
            throw new BusinessException("Table already booked for this time slot");
        }

        // Create booking
        TableBooking booking = TableBooking.builder()
                .tableId(table.getId())
                .customerName(request.getCustomerName())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .partySize(request.getPartySize())
                .bookingDate(request.getBookingDate())
                .timeSlot(request.getTimeSlot())
                .status(TableBooking.BookingStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                .build();

        booking = bookingRepository.save(booking);

        log.info("Created booking {} for table {} on {}",
                booking.getId(), request.getTableNumber(), request.getBookingDate());

        return toBookingResponse(booking, table.getTableNumber());
    }

    @Transactional
    public BookingResponse updateBookingStatus(String bookingId, BookingUpdateRequest request) {
        TableBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        TableBooking.BookingStatus newStatus = TableBooking.BookingStatus.valueOf(request.getStatus());

        booking.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED:
                booking.setConfirmedAt(LocalDateTime.now());
                break;
            case ARRIVED:
                booking.setArrivedAt(LocalDateTime.now());
                break;
            case COMPLETED:
                booking.setCompletedAt(LocalDateTime.now());
                break;
            case CANCELLED:
                booking.setCancelledAt(LocalDateTime.now());
                break;
        }

        bookingRepository.save(booking);

        log.info("Updated booking {} status to {}", bookingId, newStatus);

        Table table = tableRepository.findById(booking.getTableId()).orElse(null);
        return toBookingResponse(booking, table != null ? table.getTableNumber() : null);
    }

    public List<BookingResponse> getBookingsForDate(LocalDate date) {
        List<TableBooking> bookings = bookingRepository
                .findByBookingDateAndStatus(date, TableBooking.BookingStatus.CONFIRMED);

        return bookings.stream()
                .map(booking -> {
                    Table table = tableRepository.findById(booking.getTableId()).orElse(null);
                    return toBookingResponse(booking, table != null ? table.getTableNumber() : null);
                })
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getCustomerBookings(String mobile) {
        List<TableBooking> bookings = bookingRepository.findByMobileOrderByCreatedAtDesc(mobile);

        return bookings.stream()
                .map(booking -> {
                    Table table = tableRepository.findById(booking.getTableId()).orElse(null);
                    return toBookingResponse(booking, table != null ? table.getTableNumber() : null);
                })
                .collect(Collectors.toList());
    }

    private BookingResponse toBookingResponse(TableBooking booking, Integer tableNumber) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .tableNumber(tableNumber)
                .customerName(booking.getCustomerName())
                .mobile(booking.getMobile())
                .partySize(booking.getPartySize())
                .bookingDate(booking.getBookingDate())
                .timeSlot(booking.getTimeSlot())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
