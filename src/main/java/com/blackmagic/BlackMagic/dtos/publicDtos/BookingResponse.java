package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private String bookingId;
    private Integer tableNumber;
    private String customerName;
    private String mobile;
    private Integer partySize;
    private LocalDate bookingDate;
    private String timeSlot;
    private String status;
    private LocalDateTime createdAt;
}
