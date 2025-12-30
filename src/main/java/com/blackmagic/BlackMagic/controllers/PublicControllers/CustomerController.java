package com.blackmagic.BlackMagic.controllers.PublicControllers;

import com.blackmagic.BlackMagic.dtos.adminDtos.AdminUserCreateRequest;
import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.dtos.apiResponse.*;
import com.blackmagic.BlackMagic.models.AdminUser;
import com.blackmagic.BlackMagic.models.MenuItem;
import com.blackmagic.BlackMagic.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomerController {

    private final TableSessionService sessionService;
    private final MenuService menuService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ModelMapper modelMapper;
    private final AdminService adminService;



    @PostMapping("/session/start")
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            @Valid @RequestBody SessionStartRequest request) {
        log.info("Starting session for QR token");
        SessionResponse response = sessionService.startSession(request);
        return ResponseEntity.ok(ApiResponse.success("Session started", response));
    }

    @PostMapping("/session/{sessionCode}/close")
    public ResponseEntity<ApiResponse<Void>> closeSession(
            @PathVariable String sessionCode) {
        log.info("Closing session: {}", sessionCode);
        sessionService.closeSession(sessionCode);
        return ResponseEntity.ok(ApiResponse.success("Session closed", null));
    }

    @GetMapping("/menu")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu() {
        log.info("Fetching menu");
        MenuResponse menu = menuService.getMenu();
        return ResponseEntity.ok(ApiResponse.success(menu));
    }


    @GetMapping("/menu/popular")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> getPopularItems() {
        log.info("Fetching popular items");

        List<MenuItem> items = menuService.getPopularItems();

        List<MenuItemDTO> dtoList = items.stream()
                .map(item -> modelMapper.map(item, MenuItemDTO.class))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        log.info("Creating order for session: {}", request.getSessionCode());
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created", order));
    }

    @GetMapping("/session/{sessionCode}/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getSessionOrders(
            @PathVariable String sessionCode) {
        log.info("Fetching orders for session: {}", sessionCode);
        List<OrderResponse> orders = orderService.getSessionOrders(sessionCode);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderCancelRequest request) {
        log.info("Cancelling order: {}", orderId);
        OrderResponse order = orderService.cancelOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    @PostMapping("/payments/initiate")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {
        log.info("Initiating payment for order: {}", request.getOrderId());
        PaymentInitiateResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @PostMapping("/payments/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        log.info("Verifying payment");
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified", response));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUser>> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {
        log.info("Creating admin user: {}", request.getUsername());
        AdminUser user = adminService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created", user));
    }
}