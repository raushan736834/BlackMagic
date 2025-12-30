package com.blackmagic.BlackMagic.models;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "table_sessions")
public class TableSession {
    @Id
    private String id;

    @Indexed
    private String tableId;

    @Indexed(unique = true)
    private String sessionCode; // Unique session identifier

    @Indexed
    private SessionStatus status; // ACTIVE, COMPLETED, EXPIRED

    private Integer activeCustomers;
    private List<String> deviceIds; // Track customer devices

    @Indexed
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    private LocalDateTime lastActivityAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @Version
    private Long version;

    public enum SessionStatus {
        ACTIVE, COMPLETED, EXPIRED
    }
}
