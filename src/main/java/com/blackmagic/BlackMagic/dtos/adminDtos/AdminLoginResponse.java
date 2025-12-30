package com.blackmagic.BlackMagic.dtos.adminDtos;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminLoginResponse {
    private String token;
    private String username;
    private String role;
    private List<String> permissions;
}
