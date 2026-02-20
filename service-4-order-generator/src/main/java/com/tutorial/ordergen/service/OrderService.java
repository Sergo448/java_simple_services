package com.tutorial.ordergen.service;

import org.springframework.stereotype.Service;
import com.tutorial.ordergen.repository.OrderRepository;
import com.tutorial.ordergen.model.Order;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;


@Service
public class OrderService {

    // TODO: методы: findAll, findById, create, findNewSince
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order create(Order order) {
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> findNewSince(LocalDateTime since) {
        return orderRepository.findByCreatedAtAfter(since);
    }
}
