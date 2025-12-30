package com.blackmagic.BlackMagic.controllers.AdminControllers;

import com.blackmagic.BlackMagic.dtos.adminDtos.AdminLoginRequest;
import com.blackmagic.BlackMagic.dtos.adminDtos.AdminLoginResponse;
import com.blackmagic.BlackMagic.dtos.adminDtos.AdminUserCreateRequest;
import com.blackmagic.BlackMagic.dtos.apiResponse.ApiResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.PaymentResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.RefundRequest;
import com.blackmagic.BlackMagic.dtos.publicDtos.TableCreateRequest;
import com.blackmagic.BlackMagic.dtos.publicDtos.TableUpdateRequest;
import com.blackmagic.BlackMagic.exception.ResourceNotFoundException;
import com.blackmagic.BlackMagic.models.AdminUser;
import com.blackmagic.BlackMagic.models.Order;
import com.blackmagic.BlackMagic.models.Payment;
import com.blackmagic.BlackMagic.models.Table;
import com.blackmagic.BlackMagic.repos.OrderRepository;
import com.blackmagic.BlackMagic.repos.PaymentRepository;
import com.blackmagic.BlackMagic.repos.TableRepository;
import com.blackmagic.BlackMagic.services.AdminService;
import com.blackmagic.BlackMagic.services.AnalyticsService;
import com.blackmagic.BlackMagic.services.MenuService;
import com.blackmagic.BlackMagic.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final AnalyticsService analyticsService;
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login attempt: {}", request.getUsername());
        AdminLoginResponse response = adminService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUser>> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {
        log.info("Creating admin user: {}", request.getUsername());
        AdminUser user = adminService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created", user));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUser>>> getAllUsers() {
        log.info("Fetching all admin users");
        List<AdminUser> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/analytics/daily/{date}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyReport(
            @PathVariable String date) {
        LocalDate reportDate = LocalDate.parse(date);
        log.info("Fetching daily report for: {}", date);
        Map<String, Object> report = analyticsService.getDailyReport(reportDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/analytics/popular-items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPopularItems(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        log.info("Fetching popular items from {} to {}", startDate, endDate);
        List<Map<String, Object>> items = analyticsService.getPopularItems(start, end);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Fetching all orders");

        List<Order> orders;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            orders = orderRepository.findByCreatedAtBetween(start, end);
        } else {
            orders = orderRepository.findAll();
        }

        if (status != null) {
            Order.OrderStatus filterStatus = Order.OrderStatus.valueOf(status);
            orders = orders.stream()
                    .filter(o -> o.getStatus() == filterStatus)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<Table>>> getAllTables() {
        log.info("Fetching all tables");
        List<Table> tables = tableRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<Table>> createTable(
            @Valid @RequestBody TableCreateRequest request) {
        log.info("Creating table: {}", request.getTableNumber());

        Table table = Table.builder()
                .tableNumber(request.getTableNumber())
                .qrToken(UUID.randomUUID().toString())
                .active(true)
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .build();

        table = tableRepository.save(table);
        return ResponseEntity.ok(ApiResponse.success("Table created", table));
    }

    @PatchMapping("/tables/{tableId}")
    public ResponseEntity<ApiResponse<Table>> updateTable(
            @PathVariable String tableId,
            @Valid @RequestBody TableUpdateRequest request) {
        log.info("Updating table: {}", tableId);

        Table table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (request.getActive() != null) {
            table.setActive(request.getActive());
        }
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getLocation() != null) {
            table.setLocation(request.getLocation());
        }

        table = tableRepository.save(table);
        return ResponseEntity.ok(ApiResponse.success("Table updated", table));
    }

    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiateRefund(
            @PathVariable String orderId,
            @Valid @RequestBody RefundRequest request) {
        log.info("Initiating refund for order: {}", orderId);

        PaymentResponse response;
        if (request.getRefundAmount() != null && request.getRefundAmount() > 0) {
            // Partial refund
            response = paymentService.initiatePartialRefund(
                    orderId,
                    request.getRefundAmount(),
                    request.getReason()
            );
        } else {
            // Full refund
            response = paymentService.initiateRefund(orderId, request.getReason());
        }

        return ResponseEntity.ok(ApiResponse.success("Refund initiated", response));
    }

    @GetMapping("/payments/{orderId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentDetails(
            @PathVariable String orderId) {
        log.info("Fetching payment details for order: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
