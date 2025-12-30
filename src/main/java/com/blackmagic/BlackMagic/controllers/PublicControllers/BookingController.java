package com.blackmagic.BlackMagic.controllers.PublicControllers;

import com.blackmagic.BlackMagic.dtos.apiResponse.ApiResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.BookingCreateRequest;
import com.blackmagic.BlackMagic.dtos.publicDtos.BookingResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.BookingUpdateRequest;
import com.blackmagic.BlackMagic.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {
        log.info("Creating booking for table {} on {}",
                request.getTableNumber(), request.getBookingDate());
        BookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.ok(ApiResponse.success("Booking created", booking));
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody BookingUpdateRequest request) {
        log.info("Updating booking {} to status {}", bookingId, request.getStatus());
        BookingResponse booking = bookingService.updateBookingStatus(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking updated", booking));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsForDate(
            @PathVariable String date) {
        LocalDate bookingDate = LocalDate.parse(date);
        log.info("Fetching bookings for date: {}", date);
        List<BookingResponse> bookings = bookingService.getBookingsForDate(bookingDate);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/customer/{mobile}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCustomerBookings(
            @PathVariable String mobile) {
        log.info("Fetching bookings for customer: {}", mobile);
        List<BookingResponse> bookings = bookingService.getCustomerBookings(mobile);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}