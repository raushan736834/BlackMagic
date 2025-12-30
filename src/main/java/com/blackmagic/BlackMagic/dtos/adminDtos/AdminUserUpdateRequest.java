package com.blackmagic.BlackMagic.dtos.adminDtos;

import lombok.Data;
import java.util.List;

@Data
public class AdminUserUpdateRequest {
    private String name;
    private String email;
    private String role;
    private Boolean active;
    private List<String> assignedTables;
}
