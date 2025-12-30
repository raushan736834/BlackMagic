package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.TableSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableSessionRepository extends MongoRepository<TableSession, String> {
    Optional<TableSession> findBySessionCode(String sessionCode);

    Optional<TableSession> findByTableIdAndStatus(String tableId, TableSession.SessionStatus status);

    List<TableSession> findByStatusAndLastActivityAtBefore(
            TableSession.SessionStatus status,
            LocalDateTime timestamp
    );

    @Query("{ 'status': 'ACTIVE', 'tableId': ?0 }")
    Optional<TableSession> findActiveSessionByTableId(String tableId);

    List<TableSession> findByTableIdOrderByCreatedAtDesc(String tableId);
}
