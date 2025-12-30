package com.blackmagic.BlackMagic.scheduler;

import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsScheduler {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Generate daily analytics report
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional(readOnly = true)
    public void generateDailyReport() {
        log.info("Generating daily analytics report...");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        long totalOrders = orders.size();
        long servedOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.SERVED)
                .count();
        long cancelledOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED)
                .count();

        double totalRevenue = orders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .mapToDouble(Order::getTotal)
                .sum();

        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        // Calculate average preparation time
        double avgPrepTime = orders.stream()
                .filter(o -> o.getReadyAt() != null && o.getAcceptedAt() != null)
                .mapToLong(o -> java.time.Duration.between(
                        o.getAcceptedAt(), o.getReadyAt()).toMinutes())
                .average()
                .orElse(0);

        log.info("Daily Report for {}: Orders={}, Served={}, Cancelled={}, Revenue={}, AvgValue={}, AvgPrepTime={}min",
                yesterday, totalOrders, servedOrders, cancelledOrders,
                totalRevenue, avgOrderValue, avgPrepTime);

        // Here you could save this report to a database or send via email
    }

    /**
     * Update menu item popularity metrics
     */
    @Scheduled(cron = "0 30 2 * * ?") // Daily at 2:30 AM
    @Transactional
    public void updatePopularityMetrics() {
        log.info("Updating menu item popularity...");

        LocalDate lastWeek = LocalDate.now().minusDays(7);
        LocalDateTime start = lastWeek.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<Order> recentOrders = orderRepository.findByCreatedAtBetween(start, end);

        // Count item frequencies
        java.util.Map<String, Long> itemCounts = recentOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(java.util.stream.Collectors.groupingBy(
                        Order.OrderItem::getMenuItemId,
                        java.util.stream.Collectors.counting()
                ));

        // Get top 10 items
        List<String> topItemIds = itemCounts.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(java.util.Map.Entry::getKey)
                .toList();

        // Update tags for popular items
        List<MenuItem> allItems = menuItemRepository.findAll();
        for (MenuItem item : allItems) {
            List<String> tags = item.getTags() != null ?
                    new java.util.ArrayList<>(item.getTags()) : new java.util.ArrayList<>();

            if (topItemIds.contains(item.getId())) {
                if (!tags.contains("popular")) {
                    tags.add("popular");
                    item.setTags(tags);
                    menuItemRepository.save(item);
                }
            } else {
                tags.remove("popular");
                item.setTags(tags);
                menuItemRepository.save(item);
            }
        }

        log.info("Updated popularity for {} items", topItemIds.size());
    }
}

