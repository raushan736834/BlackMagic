package com.blackmagic.BlackMagic.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "table_bookings")
public class TableBooking {
    @Id
    private String id;

    @Indexed
    private String tableId;

    private String customerName;
    private String mobile;
    private String email;
    private Integer partySize;

    @Indexed
    private LocalDate bookingDate;
    private String timeSlot; // e.g., "18:00-20:00"

    @Indexed
    private BookingStatus status;

    private String specialRequests;
    private LocalDateTime confirmedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum BookingStatus {
        PENDING, CONFIRMED, ARRIVED, COMPLETED, CANCELLED, NO_SHOW
    }
}
