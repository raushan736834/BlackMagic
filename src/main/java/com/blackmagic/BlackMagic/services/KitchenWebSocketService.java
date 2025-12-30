package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.kitchenDtos.KitchenOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenWebSocketService {

    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public void sendOrderToKitchen(KitchenOrderDTO order) {
        messagingTemplate.convertAndSend("/topic/kitchen/orders", order);
        log.info("Sent order {} to kitchen via WebSocket", order.getOrderCode());
    }

    public void sendOrderUpdate(String orderId, String status) {
        messagingTemplate.convertAndSend("/topic/kitchen/updates/" + orderId,
                (Object) Map.of("orderId", orderId, "status", status, "timestamp", LocalDateTime.now()));
    }

    public void notifyTableStatus(String sessionCode, String status) {
        messagingTemplate.convertAndSend("/topic/table/" + sessionCode + "/status",
                (Object) Map.of("status", status, "timestamp", LocalDateTime.now()));
    }
}
