package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.kitchenDtos.KitchenOrderDTO;
import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final TableSessionRepository sessionRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final KitchenWebSocketService kitchenWebSocketService;
    private final RefundService refundService;

    private static final Double TAX_RATE = 0.05; // 5% tax
    private static final Integer CANCELLATION_WINDOW_MINUTES = 5;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        // Validate session
        TableSession session = sessionRepository.findBySessionCode(request.getSessionCode())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getStatus() != TableSession.SessionStatus.ACTIVE) {
            throw new BusinessException("Session is not active");
        }

        // Validate and fetch menu items
        List<Order.OrderItem> orderItems = new ArrayList<>();
        double subTotal = 0.0;
        int totalPrepTime = 0;

        for (OrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemReq.getMenuItemId()));

            if (!menuItem.getAvailable()) {
                throw new BusinessException("Item not available: " + menuItem.getName());
            }

            double itemTotal = menuItem.getPrice() * itemReq.getQuantity();
            subTotal += itemTotal;

            orderItems.add(Order.OrderItem.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .price(menuItem.getPrice())
                    .quantity(itemReq.getQuantity())
                    .specialRequest(itemReq.getSpecialRequest())
                    .status(Order.OrderItem.ItemStatus.PENDING)
                    .build());

            totalPrepTime = Math.max(totalPrepTime, menuItem.getPreparationTimeMinutes());
        }

        double taxAmount = subTotal * TAX_RATE;
        double total = subTotal + taxAmount;

        // Create order
        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .sessionId(session.getId())
                .orderCode(orderCode)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .items(orderItems)
                .specialInstructions(request.getSpecialInstructions())
                .subTotal(subTotal)
                .taxAmount(taxAmount)
                .total(total)
                .estimatedPrepTimeMinutes(totalPrepTime)
                .createdAt(LocalDateTime.now())
                .modifications(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        // Update session activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Send to kitchen
        sendToKitchen(order, session);

        log.info("Created order {} for session {}", orderCode, request.getSessionCode());

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId, OrderCancelRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if cancellation is allowed
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("Order already cancelled");
        }

        if (order.getStatus() == Order.OrderStatus.SERVED) {
            throw new BusinessException("Cannot cancel served order");
        }

        // Check cancellation window
        LocalDateTime cancellationDeadline = order.getCreatedAt()
                .plusMinutes(CANCELLATION_WINDOW_MINUTES);

        if (LocalDateTime.now().isAfter(cancellationDeadline) &&
                order.getStatus() != Order.OrderStatus.PLACED) {
            throw new BusinessException("Cancellation window expired");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(request.getReason());

        // Add modification record
        order.getModifications().add(Order.OrderModification.builder()
                .timestamp(LocalDateTime.now())
                .modificationType("CANCEL_ORDER")
                .details(request.getReason())
                .build());

        orderRepository.save(order);

        // Process automatic refund if order was paid
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            try {
                refundService.processAutomaticRefund(orderId);
            } catch (Exception e) {
                log.error("Automatic refund failed for cancelled order {}", orderId, e);
                // Don't throw - order is still cancelled
            }
        }

        log.info("Cancelled order {}", order.getOrderCode());

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(request.getStatus());

        // Validate state transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        switch (newStatus) {
            case IN_KITCHEN:
                order.setAcceptedAt(LocalDateTime.now());
                break;
            case READY:
                order.setReadyAt(LocalDateTime.now());
                break;
            case SERVED:
                order.setServedAt(LocalDateTime.now());
                break;
        }

        if (request.getStaffId() != null) {
            order.setAssignedToStaffId(request.getStaffId());
        }

        orderRepository.save(order);

        log.info("Updated order {} status to {}", order.getOrderCode(), newStatus);

        return toOrderResponse(order);
    }

    public List<OrderResponse> getSessionOrders(String sessionCode) {
        TableSession session = sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        List<Order> orders = orderRepository.findBySessionIdOrderByCreatedAtDesc(session.getId());

        return orders.stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus next) {
        Map<Order.OrderStatus, List<Order.OrderStatus>> validTransitions = Map.of(
                Order.OrderStatus.PLACED, Arrays.asList(Order.OrderStatus.IN_KITCHEN, Order.OrderStatus.CANCELLED),
                Order.OrderStatus.IN_KITCHEN, Arrays.asList(Order.OrderStatus.PREPARING, Order.OrderStatus.CANCELLED),
                Order.OrderStatus.PREPARING, Arrays.asList(Order.OrderStatus.READY),
                Order.OrderStatus.READY, Arrays.asList(Order.OrderStatus.SERVED)
        );

        if (!validTransitions.getOrDefault(current, Collections.emptyList()).contains(next)) {
            throw new BusinessException("Invalid status transition from " + current + " to " + next);
        }
    }

    private void sendToKitchen(Order order, TableSession session) {
        Table table = tableRepository.findById(session.getTableId()).orElse(null);

        KitchenOrderDTO kitchenOrder = KitchenOrderDTO.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .tableNumber(table != null ? table.getTableNumber() : null)
                .items(order.getItems().stream()
                        .map(item -> KitchenOrderDTO.KitchenItemDTO.builder()
                                .itemName(item.getItemName())
                                .quantity(item.getQuantity())
                                .specialRequest(item.getSpecialRequest())
                                .status(item.getStatus().name())
                                .build())
                        .collect(Collectors.toList()))
                .specialInstructions(order.getSpecialInstructions())
                .receivedAt(order.getCreatedAt())
                .estimatedPrepTime(order.getEstimatedPrepTimeMinutes())
                .status(order.getStatus().name())
                .build();

        kitchenWebSocketService.sendOrderToKitchen(kitchenOrder);
    }

    private String generateOrderCode() {
        return "ORD-" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + String.format("%04d", new Random().nextInt(10000));
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(order);
    }

    public OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .items(order.getItems().stream()
                        .map(item -> OrderResponse.OrderItemDTO.builder()
                                .menuItemId(item.getMenuItemId())
                                .itemName(item.getItemName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .specialRequest(item.getSpecialRequest())
                                .status(item.getStatus().name())
                                .build())
                        .collect(Collectors.toList()))
                .subTotal(order.getSubTotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .estimatedPrepTimeMinutes(order.getEstimatedPrepTimeMinutes())
                .createdAt(order.getCreatedAt())
                .specialInstructions(order.getSpecialInstructions())
                .build();
    }
}