package com.blackmagic.BlackMagic.controllers.PublicControllers;

import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Kitchen staff sends status updates
     * Message sent to: /app/kitchen/order/{orderId}/status
     * Broadcast to: /topic/kitchen/updates/{orderId}
     */
    @MessageMapping("/kitchen/order/{orderId}/status")
    @SendTo("/topic/kitchen/updates/{orderId}")
    public Map<String, Object> updateOrderStatus(
            @DestinationVariable String orderId,
            OrderStatusUpdateRequest request) {

        log.info("WebSocket: Updating order {} to status {}", orderId, request.getStatus());

        try {
            orderService.updateOrderStatus(orderId, request);

            return Map.of(
                    "success", true,
                    "orderId", orderId,
                    "status", request.getStatus(),
                    "timestamp", LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Failed to update order status via WebSocket", e);
            return Map.of(
                    "success", false,
                    "orderId", orderId,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Broadcast order updates to specific table
     */
    public void notifyTable(String sessionCode, Map<String, Object> update) {
        messagingTemplate.convertAndSend("/topic/table/" + sessionCode + "/updates", (Object) update);
        log.debug("Sent update to table session: {}", sessionCode);
    }

    /**
     * Kitchen requests order details
     */
    @MessageMapping("/kitchen/order/{orderId}/details")
    @SendTo("/topic/kitchen/order-details/{orderId}")
    public OrderResponse getOrderDetails(@DestinationVariable String orderId) {
        log.info("WebSocket: Fetching order details for {}", orderId);
        return orderService.getOrderById(orderId);
    }
}

