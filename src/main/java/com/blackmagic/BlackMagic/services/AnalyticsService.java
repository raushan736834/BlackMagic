package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.models.Order;
import com.blackmagic.BlackMagic.repos.MenuItemRepository;
import com.blackmagic.BlackMagic.repos.OrderRepository;
import com.blackmagic.BlackMagic.repos.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MenuItemRepository menuItemRepository;


    public Map<String, Object> getDailyReport(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        long totalOrders = orders.size();
        long completedOrders = 0;
        long cancelledOrders = 0;
        double totalRevenue = 0.0;

        for (Order o : orders) {
            if (o.getStatus() == Order.OrderStatus.SERVED) completedOrders++;
            else if (o.getStatus() == Order.OrderStatus.CANCELLED) cancelledOrders++;

            if (o.getPaymentStatus() == Order.PaymentStatus.PAID) {
                totalRevenue += o.getTotal();
            }
        }

        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("totalOrders", totalOrders);
        result.put("completedOrders", completedOrders);
        result.put("cancelledOrders", cancelledOrders);
        result.put("totalRevenue", totalRevenue);
        result.put("avgOrderValue", avgOrderValue);
        return result;
    }

    public List<Map<String, Object>> getPopularItems(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        Map<String, Integer> itemCounts = new HashMap<>();
        for (Order order : orders) {
            order.getItems().forEach(item ->
                    itemCounts.merge(item.getItemName(), item.getQuantity(), Integer::sum)
            );
        }

        PriorityQueue<Map.Entry<String, Integer>> topHeap =
                new PriorityQueue<>(Map.Entry.comparingByValue()); // min-heap by quantity

        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            topHeap.offer(entry);
            if (topHeap.size() > 10) topHeap.poll();
        }

        List<Map<String, Object>> topList = new ArrayList<>(topHeap.size());
        // Extract from heap to list in descending order
        List<Map.Entry<String, Integer>> temp = new ArrayList<>();
        while (!topHeap.isEmpty()) temp.add(topHeap.poll());
        for (int i = temp.size() - 1; i >= 0; i--) {
            Map<String, Object> map = new HashMap<>();
            map.put("itemName", temp.get(i).getKey());
            map.put("quantity", temp.get(i).getValue());
            topList.add(map);
        }

        return topList;
    }
}