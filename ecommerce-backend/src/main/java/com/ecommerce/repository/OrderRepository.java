package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNo(String orderNo);
    
    Optional<Order> findByIdAndIsDeletedTrue(Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderNo = :orderNo")
    Optional<Order> findByOrderNoWithItems(@Param("orderNo") String orderNo);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "AND (:keyword IS NULL OR o.orderNo LIKE %:keyword%) " +
           "AND o.isDeleted = false")
    Page<Order> findUserOrders(@Param("userId") Long userId,
                              @Param("status") Order.OrderStatus status,
                              @Param("paymentStatus") Order.PaymentStatus paymentStatus,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("keyword") String keyword,
                              Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:shippingStatus IS NULL OR o.shippingStatus = :shippingStatus) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "AND (:keyword IS NULL OR o.orderNo LIKE %:keyword%) " +
           "AND o.isDeleted = false")
    Page<Order> findAllOrders(@Param("status") Order.OrderStatus status,
                             @Param("paymentStatus") Order.PaymentStatus paymentStatus,
                             @Param("shippingStatus") Order.ShippingStatus shippingStatus,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             @Param("keyword") String keyword,
                             Pageable pageable);
    
    @Query("SELECT o.status as status, COUNT(o) as count FROM Order o " +
           "WHERE o.userId = :userId AND o.isDeleted = false GROUP BY o.status")
    Map<String, Long> countOrdersByUserIdAndStatus(@Param("userId") Long userId);
}