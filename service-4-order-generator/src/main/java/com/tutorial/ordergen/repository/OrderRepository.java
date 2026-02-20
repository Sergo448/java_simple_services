package com.tutorial.ordergen.repository;

import com.tutorial.ordergen.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;



@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCreatedAtAfter(LocalDateTime since);
    List<Order> findByStatus(String status);
    List<Order> findByPriceGreaterThan(Double price);
    List<Order> findByProductContaining(String keyword);
    List<Order> findByStatusAndCreatedAtAfter(String status, LocalDateTime since);
}
