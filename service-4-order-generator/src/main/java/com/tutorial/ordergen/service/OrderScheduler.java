package com.tutorial.ordergen.service;

import org.springframework.stereotype.Service;
import com.tutorial.ordergen.repository.OrderRepository;
import com.tutorial.ordergen.model.Order;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;




@Component
public class OrderScheduler {

    private final OrderService orderService;

    public OrderScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 10000)
    public void generateOrder() {
        String[] products = {
            "Laptop",
            "Smartphone",
            "Headphones",
            "Camera",
            "Smartwatch"
        };

        String[] statuses = {
            "NEW",
            "PENDING",
            "COMPLETED",
            "CANCELLED"
        };

        int quantity = ThreadLocalRandom.current().nextInt(1, 11);
        double price = ThreadLocalRandom.current().nextDouble(1000, 2000);
        Order order = new Order();
        order.setProduct(products[ThreadLocalRandom.current().nextInt(products.length)]);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setStatus(statuses[ThreadLocalRandom.current().nextInt(statuses.length)]);

        System.out.println(">>>Generated order: " + order.getProduct()
            + " | quantity: " + quantity
            + " | price: " + price
            + " | status: " + order.getStatus());
        orderService.create(order);
    }
}
