package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TableCreateRequest {
    @NotNull
    private Integer tableNumber;
    private Integer capacity;
    private String location;
}
