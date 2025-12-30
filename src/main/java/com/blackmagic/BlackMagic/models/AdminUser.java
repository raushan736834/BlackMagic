package com.blackmagic.BlackMagic.models;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "admin_users")
public class AdminUser {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String name;
    private String mobile;

    @Indexed
    private UserRole role;

    private List<String> permissions;
    private List<String> assignedTables; // For waiters

    private Boolean active;
    private LocalDateTime lastLoginAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum UserRole {
        SUPER_ADMIN, MANAGER, CHEF, WAITER, CASHIER
    }
}