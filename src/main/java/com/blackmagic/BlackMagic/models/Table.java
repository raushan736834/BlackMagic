package com.blackmagic.BlackMagic.models;


import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;


@Data
@Builder
@Document(collection = "tables")
public class Table {
    @Id
    private String id;

    @Indexed(unique = true)
    private Integer tableNumber;

    @Indexed(unique = true)
    private String qrToken; // UUID for QR code

    private Boolean active;
    private Integer capacity;
    private String location; // e.g., "Indoor", "Outdoor", "Window"

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking
}
