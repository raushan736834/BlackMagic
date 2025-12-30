package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    private String sessionCode;
    private Integer tableNumber;
    private String status;
    private LocalDateTime startedAt;
}
