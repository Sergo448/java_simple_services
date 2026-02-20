package com.tutorial.ordergen.controller;

import org.springframework.web.bind.annotation.*;
import com.tutorial.ordergen.service.OrderService;
import com.tutorial.ordergen.model.Order;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;





@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        return orderService.create(order);
    }

    @GetMapping("/new")
    public List<Order> getNewOrders(
        @RequestParam("since") String sinceStr) {
        LocalDateTime since = LocalDateTime.parse(sinceStr);
        return orderService.findNewSince(since);
    }

}
