package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Data;

@Data
public class BookingUpdateRequest {
    private String status; // CONFIRMED, CANCELLED, ARRIVED, etc.
}
