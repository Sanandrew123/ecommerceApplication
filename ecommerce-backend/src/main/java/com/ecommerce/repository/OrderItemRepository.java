package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderId(Long orderId);
    
    List<OrderItem> findByProductId(Long productId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId AND oi.isDeleted = false")
    List<OrderItem> findActiveByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId AND oi.isDeleted = false")
    List<OrderItem> findActiveByProductId(@Param("productId") Long productId);
}