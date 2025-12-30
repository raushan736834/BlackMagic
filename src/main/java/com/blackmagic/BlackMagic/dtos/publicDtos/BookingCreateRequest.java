package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingCreateRequest {
    @NotNull
    private Integer tableNumber;

    @NotBlank
    private String customerName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$")
    private String mobile;

    private String email;

    @NotNull
    @Min(1)
    private Integer partySize;

    @NotNull
    @Future
    private LocalDate bookingDate;

    @NotBlank
    private String timeSlot;

    private String specialRequests;
}
