package com.sparta.msa.lesson.domain.order.repository;

import com.sparta.msa.lesson.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
