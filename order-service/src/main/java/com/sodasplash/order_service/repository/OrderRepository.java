package com.sodasplash.order_service.repository;

import com.sodasplash.order_service.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = "quoteItems")
    List<Order> findAll();

    @EntityGraph(attributePaths = "quoteItems")
    List<Order> findByEmail(String email);

    @EntityGraph(attributePaths = "quoteItems")
    List<Order> findByReferralEmail(String email);

    Optional<Order> findByQuoteNumber(String quoteNumber);
}
