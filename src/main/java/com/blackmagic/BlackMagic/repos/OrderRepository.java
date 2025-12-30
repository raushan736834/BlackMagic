package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Query("{ 'status': { $in: ?0 } }")
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);

    @Query("{ 'status': { $in: ['IN_KITCHEN', 'PREPARING'] } }")
    List<Order> findActiveKitchenOrders();

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("{ 'sessionId': ?0, 'status': { $ne: 'CANCELLED' } }")
    List<Order> findNonCancelledOrdersBySession(String sessionId);

    long countByStatusAndCreatedAtAfter(Order.OrderStatus status, LocalDateTime timestamp);
}
