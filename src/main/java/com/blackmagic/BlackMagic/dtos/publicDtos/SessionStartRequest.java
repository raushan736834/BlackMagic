package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartRequest {
    @NotBlank(message = "QR token is required")
    private String qrToken;

    private Integer partySize;
    private String deviceId;
}
