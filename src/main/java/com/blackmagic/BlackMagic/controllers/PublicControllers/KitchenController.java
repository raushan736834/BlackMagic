package com.blackmagic.BlackMagic.controllers.PublicControllers;

import com.blackmagic.BlackMagic.dtos.apiResponse.ApiResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.OrderResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.OrderStatusUpdateRequest;
import com.blackmagic.BlackMagic.exception.ResourceNotFoundException;
import com.blackmagic.BlackMagic.models.Order;
import com.blackmagic.BlackMagic.repos.OrderRepository;
import com.blackmagic.BlackMagic.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class KitchenController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrders() {
        log.info("Fetching active kitchen orders");
        List<Order> orders = orderRepository.findActiveKitchenOrders();
        List<OrderResponse> responses = orders.stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("Updating order {} status to {}", orderId, request.getStatus());
        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Status updated", order));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return ResponseEntity.ok(ApiResponse.success(toOrderResponse(order)));
    }

    private OrderResponse toOrderResponse(Order order) {
        // Convert Order to OrderResponse
        return orderService.toOrderResponse(order);
    }
}
