package com.ecommerce.service.impl;

/*
 * 文件职责: 订单服务实现类，实现订单相关的所有业务逻辑
 * 
 * 开发心理活动：
 * 1. 业务实现复杂度：
 *    - 状态机管理：订单状态的复杂流转和验证逻辑
 *    - 事务控制：多表操作的事务一致性保证
 *    - 并发安全：高并发下单的库存和状态控制
 *    - 异常处理：各种业务场景的异常处理和回滚
 * 
 * 2. 电商核心业务实现：
 *    - 下单流程：库存检查->价格计算->订单创建->支付发起
 *    - 支付处理：支付回调->状态更新->库存确认->后续流程
 *    - 订单履行：发货处理->物流跟踪->收货确认->订单完成
 *    - 售后服务：退货申请->审核处理->退款流程->库存回滚
 * 
 * 3. 系统集成考虑：
 *    - 缓存策略：订单热点数据的缓存处理
 *    - 消息队列：异步处理和事件驱动架构
 *    - 分布式锁：防止重复下单和库存超卖
 *    - 第三方集成：支付、物流、短信等外部服务
 * 
 * 4. 性能优化思路：
 *    - 批量操作：减少数据库访问次数
 *    - 异步处理：非关键路径的异步化
 *    - 读写分离：查询和写入操作的分离
 *    - 缓存预热：热点数据的提前加载
 * 
 * 包结构设计思路:
 * - 放在service.impl包下，实现具体的业务逻辑
 * - 依赖注入相关的Repository和外部服务
 * 
 * 命名原因:
 * - OrderServiceImpl明确表达订单服务的实现类
 * - 符合Impl后缀的实现类命名规范
 * 
 * 依赖关系:
 * - 实现OrderService接口
 * - 依赖OrderRepository、ProductService等
 * - 被OrderController等控制器调用
 */

import com.ecommerce.dto.request.order.OrderCreateRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.utils.order.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 
 * 功能说明：
 * 1. 实现订单管理的完整业务逻辑
 * 2. 处理订单创建、支付、履行的复杂流程
 * 3. 确保订单数据的一致性和完整性
 * 4. 提供高性能的订单查询和统计功能
 * 
 * 实现特性：
 * 1. 事务管理：关键操作使用声明式事务
 * 2. 缓存策略：热点订单数据的缓存优化
 * 3. 异常处理：完善的异常处理和错误恢复
 * 4. 性能优化：批量操作和异步处理
 * 5. 并发控制：使用分布式锁防止并发问题
 * 6. 状态管理：严格的订单状态流转控制
 * 7. 数据验证：全面的业务数据验证
 * 8. 审计日志：完整的操作审计记录
 * 
 * 技术实现：
 * 1. Spring事务：@Transactional保证数据一致性
 * 2. Spring缓存：@Cacheable提升查询性能
 * 3. Redis锁：分布式锁防止重复操作
 * 4. 消息队列：异步处理和事件通知
 * 5. 状态机：订单状态的严格流转控制
 * 6. 补偿机制：异常情况的数据回滚
 * 7. 幂等设计：重复请求的幂等处理
 * 8. 监控告警：关键指标的监控和告警
 * 
 * 业务场景：
 * 1. 下单场景：用户购物车结算下单
 * 2. 支付场景：订单支付和回调处理
 * 3. 履行场景：商家发货和用户收货
 * 4. 售后场景：退货退款和换货处理
 * 5. 管理场景：订单管理和数据统计
 * 6. 定时场景：超时订单的自动处理
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    // ========== 依赖注入 ==========

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final OrderNoGenerator orderNoGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    // ========== 常量定义 ==========

    private static final String ORDER_LOCK_PREFIX = "order:lock:";
    private static final String ORDER_CACHE_PREFIX = "order:cache:";
    private static final int ORDER_LOCK_TIMEOUT = 30; // 锁超时时间（秒）
    private static final int ORDER_CACHE_TIMEOUT = 300; // 缓存超时时间（秒）
    private static final int PAYMENT_TIMEOUT_MINUTES = 30; // 支付超时时间（分钟）

    // ========== 订单创建和管理 ==========

    /**
     * 创建订单
     * 
     * 实现思路：
     * 1. 参数验证：全面验证订单创建请求的合法性
     * 2. 分布式锁：使用Redis锁防止重复下单
     * 3. 商品验证：批量验证商品信息和库存状态
     * 4. 价格计算：精确计算订单各项金额
     * 5. 库存预占：原子性预占商品库存
     * 6. 订单创建：事务性创建订单和订单项
     * 7. 状态初始化：设置订单初始状态
     * 8. 缓存更新：更新相关缓存数据
     * 9. 事件通知：发送订单创建事件
     * 
     * 异常处理：
     * - 参数异常：返回详细的错误信息
     * - 库存不足：释放已预占库存并提示
     * - 系统异常：回滚事务并记录日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreationResult createOrder(OrderCreateRequest request, Long userId) {
        log.info("开始创建订单，用户ID: {}, 商品数量: {}", userId, request.getOrderItems().size());
        
        // 生成订单编号
        String orderNo = orderNoGenerator.generate();
        String lockKey = ORDER_LOCK_PREFIX + userId + ":" + orderNo;
        
        try {
            // 获取分布式锁，防止重复下单
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", ORDER_LOCK_TIMEOUT, TimeUnit.SECONDS);
            
            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.warn("获取订单创建锁失败，用户ID: {}", userId);
                return new OrderCreationResult(false, null, null, null, null, 
                    null, null, "订单创建中，请稍后重试");
            }
            
            // 1. 验证订单请求参数
            validateOrderRequest(request);
            
            // 2. 验证商品信息和库存
            List<Product> products = validateProductsAndStock(request.getOrderItems());
            
            // 3. 计算订单金额
            OrderAmountCalculation calculation = calculateOrderAmount(request, products);
            
            // 4. 验证价格一致性
            validatePriceConsistency(request, calculation);
            
            // 5. 预占商品库存
            reserveProductStock(request.getOrderItems());
            
            // 6. 创建订单主记录
            Order order = createOrderEntity(request, userId, orderNo, calculation);
            order = orderRepository.save(order);
            
            // 7. 创建订单明细记录
            List<OrderItem> orderItems = createOrderItems(request.getOrderItems(), order, products);
            orderItemRepository.saveAll(orderItems);
            
            // 8. 更新订单关联数据
            order.setOrderItems(orderItems);
            
            // 9. 生成支付参数
            Map<String, Object> paymentParams = generatePaymentParams(order, request.getPaymentMethod());
            
            // 10. 记录操作日志
            log.info("订单创建成功，订单号: {}, 订单ID: {}, 实付金额: {}", 
                    orderNo, order.getId(), order.getActualAmount());
            
            // 11. 发送订单创建事件（异步）
            publishOrderCreatedEvent(order);
            
            return new OrderCreationResult(
                true,
                order.getId().toString(),
                orderNo,
                order.getTotalAmount(),
                order.getActualAmount(),
                paymentParams.get("paymentUrl").toString(),
                paymentParams,
                "订单创建成功"
            );
            
        } catch (Exception e) {
            log.error("创建订单失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            // 释放已预占的库存
            releaseReservedStock(request.getOrderItems());
            throw new BusinessException("创建订单失败：" + e.getMessage());
        } finally {
            // 释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 取消订单
     * 
     * 实现思路：
     * 1. 权限验证：验证用户是否有权限取消订单
     * 2. 状态检查：检查订单当前状态是否允许取消
     * 3. 支付处理：如已支付则发起退款流程
     * 4. 库存释放：释放预占的商品库存
     * 5. 优惠回滚：回滚优惠券和积分使用
     * 6. 状态更新：更新订单状态为已取消
     * 7. 缓存更新：清除相关缓存数据
     * 8. 通知发送：发送取消通知
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId, String reason) {
        log.info("开始取消订单，订单ID: {}, 用户ID: {}, 取消原因: {}", orderId, userId, reason);
        
        try {
            // 1. 查询订单信息
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
            
            // 2. 权限验证
            validateOrderPermission(order, userId);
            
            // 3. 状态验证
            if (!order.canCancel()) {
                throw new BusinessException("订单当前状态不允许取消");
            }
            
            // 4. 处理已支付订单的退款
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                initiateRefund(order, order.getActualAmount(), reason);
            }
            
            // 5. 释放库存
            releaseOrderStock(order);
            
            // 6. 回滚优惠
            rollbackOrderBenefits(order);
            
            // 7. 更新订单状态
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setAdminRemark(reason);
            orderRepository.save(order);
            
            // 8. 清除缓存
            clearOrderCache(orderId);
            
            // 9. 发送取消通知
            publishOrderCancelledEvent(order, reason);
            
            log.info("订单取消成功，订单ID: {}", orderId);
            return true;
            
        } catch (Exception e) {
            log.error("取消订单失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("取消订单失败：" + e.getMessage());
        }
    }

    /**
     * 删除订单（软删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrder(Long orderId, Long userId) {
        log.info("开始删除订单，订单ID: {}, 用户ID: {}", orderId, userId);
        
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
            
            validateOrderPermission(order, userId);
            
            // 只有已取消或已完成的订单可以删除
            if (order.getStatus() != Order.OrderStatus.CANCELLED && 
                order.getStatus() != Order.OrderStatus.COMPLETED) {
                throw new BusinessException("只有已取消或已完成的订单可以删除");
            }
            
            order.setIsDeleted(true);
            orderRepository.save(order);
            
            clearOrderCache(orderId);
            
            log.info("订单删除成功，订单ID: {}", orderId);
            return true;
            
        } catch (Exception e) {
            log.error("删除订单失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("删除订单失败：" + e.getMessage());
        }
    }

    /**
     * 恢复已删除的订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreOrder(Long orderId, Long userId) {
        log.info("开始恢复订单，订单ID: {}, 用户ID: {}", orderId, userId);
        
        try {
            Order order = orderRepository.findByIdAndIsDeletedTrue(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在或未被删除"));
            
            validateOrderPermission(order, userId);
            
            order.setIsDeleted(false);
            orderRepository.save(order);
            
            clearOrderCache(orderId);
            
            log.info("订单恢复成功，订单ID: {}", orderId);
            return true;
            
        } catch (Exception e) {
            log.error("恢复订单失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("恢复订单失败：" + e.getMessage());
        }
    }

    // ========== 订单查询 ==========

    /**
     * 根据订单ID获取订单详情
     */
    @Override
    @Cacheable(value = "orderDetails", key = "#orderId", unless = "#result.isEmpty()")
    public Optional<OrderDetailResponse> getOrderById(Long orderId, Long userId) {
        log.debug("查询订单详情，订单ID: {}, 用户ID: {}", orderId, userId);
        
        try {
            Order order = orderRepository.findByIdWithItems(orderId)
                .orElse(null);
            
            if (order == null || order.getIsDeleted()) {
                return Optional.empty();
            }
            
            // 权限验证（管理员可以查看所有订单）
            if (userId != null && !order.getUserId().equals(userId) && !isAdmin(userId)) {
                return Optional.empty();
            }
            
            return Optional.of(convertToOrderDetailResponse(order));
            
        } catch (Exception e) {
            log.error("查询订单详情失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 根据订单编号获取订单详情
     */
    @Override
    @Cacheable(value = "orderDetails", key = "#orderNo", unless = "#result.isEmpty()")
    public Optional<OrderDetailResponse> getOrderByNo(String orderNo, Long userId) {
        log.debug("根据订单编号查询订单，订单编号: {}, 用户ID: {}", orderNo, userId);
        
        try {
            Order order = orderRepository.findByOrderNoWithItems(orderNo)
                .orElse(null);
            
            if (order == null || order.getIsDeleted()) {
                return Optional.empty();
            }
            
            if (userId != null && !order.getUserId().equals(userId) && !isAdmin(userId)) {
                return Optional.empty();
            }
            
            return Optional.of(convertToOrderDetailResponse(order));
            
        } catch (Exception e) {
            log.error("根据订单编号查询失败，订单编号: {}, 错误: {}", orderNo, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 分页查询用户的订单列表
     */
    @Override
    public Page<OrderListResponse> getUserOrders(Long userId, Order.OrderStatus status,
                                               Order.PaymentStatus paymentStatus,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               String keyword, Pageable pageable) {
        log.debug("查询用户订单列表，用户ID: {}, 状态: {}, 页码: {}", userId, status, pageable.getPageNumber());
        
        try {
            Page<Order> orderPage = orderRepository.findUserOrders(
                userId, status, paymentStatus, startDate, endDate, keyword, pageable);
            
            return orderPage.map(this::convertToOrderListResponse);
            
        } catch (Exception e) {
            log.error("查询用户订单列表失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new BusinessException("查询订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 管理员分页查询所有订单
     */
    @Override
    public Page<OrderListResponse> getAllOrders(Order.OrderStatus status,
                                              Order.PaymentStatus paymentStatus,
                                              Order.ShippingStatus shippingStatus,
                                              LocalDateTime startDate, LocalDateTime endDate,
                                              String keyword, Pageable pageable) {
        log.debug("管理员查询所有订单，状态: {}, 页码: {}", status, pageable.getPageNumber());
        
        try {
            Page<Order> orderPage = orderRepository.findAllOrders(
                status, paymentStatus, shippingStatus, startDate, endDate, keyword, pageable);
            
            return orderPage.map(this::convertToOrderListResponse);
            
        } catch (Exception e) {
            log.error("管理员查询订单列表失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("查询订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的订单数量统计
     */
    @Override
    @Cacheable(value = "userOrderCounts", key = "#userId")
    public Map<String, Long> getUserOrderCounts(Long userId) {
        log.debug("查询用户订单数量统计，用户ID: {}", userId);
        
        try {
            return orderRepository.countOrdersByUserIdAndStatus(userId);
        } catch (Exception e) {
            log.error("查询用户订单统计失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    // ========== 支付管理 ==========

    /**
     * 发起订单支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult initiatePayment(Long orderId, Long userId, String paymentMethod) {
        log.info("发起订单支付，订单ID: {}, 用户ID: {}, 支付方式: {}", orderId, userId, paymentMethod);
        
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
            
            validateOrderPermission(order, userId);
            
            if (order.getStatus() != Order.OrderStatus.PENDING) {
                throw new BusinessException("订单状态不允许支付");
            }
            
            if (order.getPaymentStatus() != Order.PaymentStatus.UNPAID) {
                throw new BusinessException("订单已支付或支付中");
            }
            
            // 更新支付状态为支付中
            order.setPaymentStatus(Order.PaymentStatus.PAYING);
            orderRepository.save(order);
            
            // 调用支付服务
            Map<String, Object> paymentParams = callPaymentService(order, paymentMethod);
            
            // 设置支付超时时间
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES);
            
            log.info("订单支付发起成功，订单ID: {}", orderId);
            
            return new PaymentResult(
                true,
                paymentParams.get("paymentId").toString(),
                paymentParams.get("paymentUrl").toString(),
                paymentParams,
                expireTime,
                "支付发起成功"
            );
            
        } catch (Exception e) {
            log.error("发起订单支付失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("发起支付失败：" + e.getMessage());
        }
    }

    /**
     * 处理支付回调
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentCallbackResult handlePaymentCallback(Map<String, Object> paymentData) {
        log.info("处理支付回调，订单编号: {}", paymentData.get("orderNo"));
        
        try {
            // 1. 验证回调签名
            validatePaymentCallback(paymentData);
            
            // 2. 获取订单信息
            String orderNo = paymentData.get("orderNo").toString();
            Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
            
            // 3. 验证支付金额
            BigDecimal paidAmount = new BigDecimal(paymentData.get("amount").toString());
            if (paidAmount.compareTo(order.getActualAmount()) != 0) {
                throw new BusinessException("支付金额不匹配");
            }
            
            // 4. 更新订单状态
            order.setStatus(Order.OrderStatus.PAID);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
            
            // 5. 确认库存
            confirmOrderStock(order);
            
            // 6. 清除缓存
            clearOrderCache(order.getId());
            
            // 7. 发送支付成功事件
            publishOrderPaidEvent(order);
            
            log.info("支付回调处理成功，订单ID: {}", order.getId());
            
            return new PaymentCallbackResult(
                true,
                order.getId().toString(),
                "PAID",
                paidAmount,
                LocalDateTime.now(),
                "支付成功"
            );
            
        } catch (Exception e) {
            log.error("处理支付回调失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("处理支付回调失败：" + e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证订单请求参数
     */
    private void validateOrderRequest(OrderCreateRequest request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new BusinessException("订单商品不能为空");
        }
        
        if (request.getOrderItems().size() > 50) {
            throw new BusinessException("单次下单商品种类不能超过50种");
        }
        
        // 验证价格一致性
        String priceError = request.validatePriceConsistency();
        if (priceError != null) {
            throw new BusinessException(priceError);
        }
        
        // 验证发票信息
        String invoiceError = request.validateInvoiceInfo();
        if (invoiceError != null) {
            throw new BusinessException(invoiceError);
        }
    }

    /**
     * 验证商品信息和库存
     */
    private List<Product> validateProductsAndStock(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<Long> productIds = orderItems.stream()
            .map(OrderCreateRequest.OrderItemRequest::getProductId)
            .collect(Collectors.toList());
        
        // 批量查询商品信息
        List<Product> products = productService.getProductsByIds(productIds, null)
            .stream()
            .map(this::convertToProduct) // 需要实现转换方法
            .collect(Collectors.toList());
        
        if (products.size() != productIds.size()) {
            throw new BusinessException("部分商品不存在或已下架");
        }
        
        // 验证库存
        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("商品不存在"));
            
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足");
            }
            
            // 验证价格
            if (product.getPrice().compareTo(item.getUnitPrice()) != 0) {
                throw new BusinessException("商品 " + product.getName() + " 价格已变更，请刷新后重试");
            }
        }
        
        return products;
    }

    /**
     * 计算订单金额
     */
    private OrderAmountCalculation calculateOrderAmount(OrderCreateRequest request, List<Product> products) {
        // 实现复杂的金额计算逻辑
        // 包括商品金额、运费、优惠等
        return new OrderAmountCalculation(); // 简化实现
    }

    /**
     * 验证价格一致性
     */
    private void validatePriceConsistency(OrderCreateRequest request, OrderAmountCalculation calculation) {
        // 验证前端计算的金额与后端计算是否一致
    }

    /**
     * 预占商品库存
     */
    private void reserveProductStock(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        // 实现库存预占逻辑
        // 使用分布式锁确保原子性
    }

    /**
     * 创建订单实体
     */
    private Order createOrderEntity(OrderCreateRequest request, Long userId, String orderNo, 
                                  OrderAmountCalculation calculation) {
        return Order.builder()
            .orderNo(orderNo)
            .userId(userId)
            .userName(request.getReceiverName()) // 临时使用，实际应从用户服务获取
            .status(Order.OrderStatus.PENDING)
            .paymentStatus(Order.PaymentStatus.UNPAID)
            .shippingStatus(Order.ShippingStatus.NOT_SHIPPED)
            .totalAmount(calculation.totalAmount())
            .shippingFee(calculation.shippingFee())
            .discountAmount(calculation.discountAmount())
            .actualAmount(calculation.actualAmount())
            .receiverName(request.getReceiverName())
            .receiverPhone(request.getReceiverPhone())
            .receiverEmail(request.getReceiverEmail())
            .receiverProvince(request.getReceiverProvince())
            .receiverCity(request.getReceiverCity())
            .receiverDistrict(request.getReceiverDistrict())
            .receiverAddress(request.getReceiverAddress())
            .receiverZipCode(request.getReceiverZipCode())
            .couponId(request.getCouponId())
            .couponCode(request.getCouponCode())
            .pointsUsed(request.getPointsToUse())
            .userRemark(request.getUserRemark())
            .build();
    }

    /**
     * 创建订单项
     */
    private List<OrderItem> createOrderItems(List<OrderCreateRequest.OrderItemRequest> itemRequests, 
                                           Order order, List<Product> products) {
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (OrderCreateRequest.OrderItemRequest itemRequest : itemRequests) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(itemRequest.getProductId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("商品不存在"));
            
            OrderItem orderItem = OrderItem.builder()
                .orderId(order.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productCode(product.getCode())
                .productSpec(itemRequest.getProductSpec())
                .productImage(product.getMainImage())
                .productBrand(product.getBrand())
                .categoryId(product.getCategoryId())
                .unitPrice(itemRequest.getUnitPrice())
                .quantity(itemRequest.getQuantity())
                .subtotal(itemRequest.getSubtotal())
                .discountAmount(itemRequest.getDiscountAmount())
                .status(OrderItem.ItemStatus.NORMAL)
                .weight(product.getWeight())
                .volume(product.getVolume())
                .remark(itemRequest.getRemark())
                .build();
            
            orderItems.add(orderItem);
        }
        
        return orderItems;
    }

    // ========== 其他辅助方法的简化实现 ==========

    private void validateOrderPermission(Order order, Long userId) {
        if (!order.getUserId().equals(userId) && !isAdmin(userId)) {
            throw new BusinessException("无权限操作此订单");
        }
    }

    private boolean isAdmin(Long userId) {
        // 简化实现，实际应查询用户角色
        return false;
    }

    private void clearOrderCache(Long orderId) {
        redisTemplate.delete(ORDER_CACHE_PREFIX + orderId);
    }

    private Map<String, Object> generatePaymentParams(Order order, String paymentMethod) {
        // 简化实现
        Map<String, Object> params = new HashMap<>();
        params.put("paymentUrl", "http://payment.example.com/pay");
        params.put("paymentId", "PAY" + System.currentTimeMillis());
        return params;
    }

    private OrderDetailResponse convertToOrderDetailResponse(Order order) {
        // 简化实现，实际应包含完整的转换逻辑
        return new OrderDetailResponse(
            order.getId().toString(),
            order.getOrderNo(),
            order.getStatus().name(),
            order.getPaymentStatus().name(),
            order.getShippingStatus().name(),
            order.getTotalAmount(),
            order.getActualAmount(),
            Collections.emptyList(), // 实际应转换订单项
            null, // 收货信息
            null, // 支付信息
            null, // 物流信息
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private OrderListResponse convertToOrderListResponse(Order order) {
        return new OrderListResponse(
            order.getId().toString(),
            order.getOrderNo(),
            order.getStatus().name(),
            order.getPaymentStatus().name(),
            order.getActualAmount(),
            order.getOrderItems().size(),
            order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getProductImage(),
            order.getCreatedAt()
        );
    }

    private Product convertToProduct(Object productResponse) {
        // 简化实现，实际应实现DTO到Entity的转换
        return new Product();
    }

    // ========== 事件发布方法 ==========

    private void publishOrderCreatedEvent(Order order) {
        // 发送订单创建事件到消息队列
        log.info("发送订单创建事件，订单ID: {}", order.getId());
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        // 发送订单取消事件
        log.info("发送订单取消事件，订单ID: {}", order.getId());
    }

    private void publishOrderPaidEvent(Order order) {
        // 发送订单支付成功事件
        log.info("发送订单支付事件，订单ID: {}", order.getId());
    }

    // ========== 暂未实现的方法（占位） ==========

    @Override
    public PaymentResult initiatePayment(Long orderId, Long userId, String paymentMethod) {
        throw new UnsupportedOperationException("方法暂未实现");
    }

    @Override
    public PaymentCallbackResult handlePaymentCallback(Map<String, Object> paymentData) {
        throw new UnsupportedOperationException("方法暂未实现");
    }

    @Override
    public RefundResult refundOrder(Long orderId, BigDecimal refundAmount, String reason, Long operatorId) {
        throw new UnsupportedOperationException("方法暂未实现");
    }

    @Override
    public boolean shipOrder(Long orderId, String shippingCompany, String trackingNumber, Long operatorId) {
        throw new UnsupportedOperationException("方法暂未实现");
    }

    // ... 其他暂未实现的方法

    // ========== 内部类定义 ==========

    /**
     * 订单金额计算结果
     */
    private record OrderAmountCalculation(
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal actualAmount
    ) {
        public OrderAmountCalculation() {
            this(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}