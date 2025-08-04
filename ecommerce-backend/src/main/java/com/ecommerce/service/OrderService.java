package com.ecommerce.service;

/*
 * 文件职责: 订单服务接口，定义订单相关的业务操作方法
 * 
 * 开发心理活动：
 * 1. 订单业务设计原则：
 *    - 业务完整性：覆盖订单从创建到完成的整个生命周期
 *    - 状态一致性：确保订单状态流转的正确性和完整性
 *    - 事务安全：保证订单操作的原子性和数据一致性
 *    - 性能优化：支持高并发下单和大数据量订单查询
 * 
 * 2. 电商订单核心业务：
 *    - 订单创建：库存检查、价格计算、优惠应用、订单生成
 *    - 支付管理：支付流程、支付回调、支付状态更新
 *    - 订单履行：发货处理、物流跟踪、签收确认
 *    - 售后服务：退货退款、换货处理、投诉管理
 * 
 * 3. 业务复杂度考虑：
 *    - 库存管理：库存预占、释放、扣减的并发安全
 *    - 价格计算：商品价格、运费、优惠券、积分的复杂计算
 *    - 状态管理：订单状态、支付状态、物流状态的协调
 *    - 异常处理：支付失败、库存不足、系统异常的处理
 * 
 * 4. 系统集成设计：
 *    - 支付系统：对接多种支付方式和第三方支付
 *    - 库存系统：实时库存查询和扣减操作
 *    - 物流系统：物流公司对接和状态同步
 *    - 消息系统：订单状态变更的异步通知
 * 
 * 包结构设计思路:
 * - 放在service包下，定义订单业务的核心接口
 * - 与实现类分离，便于替换和测试
 * 
 * 命名原因:
 * - OrderService明确表达订单业务服务功能
 * - 符合Service后缀的命名规范
 * 
 * 依赖关系:
 * - 被OrderController调用，提供业务处理
 * - 被OrderServiceImpl实现，定义业务契约
 * - 独立于具体实现，便于扩展和测试
 */

import com.ecommerce.dto.request.order.OrderCreateRequest;
import com.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单服务接口
 * 
 * 功能说明：
 * 1. 定义订单管理的核心业务操作
 * 2. 提供订单创建、支付、履行的完整流程
 * 3. 支持订单查询、统计和分析功能
 * 4. 确保订单业务的一致性和完整性
 * 
 * 业务模块：
 * 1. 订单生命周期：创建、支付、发货、完成、取消
 * 2. 支付管理：支付处理、回调、退款操作
 * 3. 库存管理：库存检查、预占、扣减、释放
 * 4. 物流管理：发货处理、物流跟踪、签收管理
 * 5. 售后服务：退货、退款、换货、投诉处理
 * 6. 统计分析：订单统计、销售分析、用户行为
 * 
 * 设计特性：
 * 1. 接口导向：基于接口编程，便于扩展和测试
 * 2. 异常安全：明确的异常类型和处理策略
 * 3. 性能优化：缓存策略和批量操作支持
 * 4. 业务完整：覆盖电商订单的完整业务场景
 * 5. 状态管理：严格的状态流转规则和验证
 * 6. 事务控制：关键操作的事务保证
 * 
 * 使用场景：
 * 1. 用户购物：下单、支付、查询、取消
 * 2. 商家管理：订单处理、发货、售后
 * 3. 系统运营：订单统计、分析、监控
 * 4. 第三方集成：支付、物流、ERP系统对接
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
public interface OrderService {

    // ========== 订单创建和管理 ==========

    /**
     * 创建订单
     * 
     * 业务流程：
     * 1. 参数验证：校验订单创建请求的完整性和合法性
     * 2. 商品验证：验证商品存在性、上架状态、价格一致性
     * 3. 库存检查：检查商品库存是否充足，支持多SKU
     * 4. 价格计算：计算商品金额、运费、优惠、实付金额
     * 5. 优惠处理：优惠券验证、积分扣减、促销活动
     * 6. 库存预占：预占商品库存，设置过期时间
     * 7. 订单生成：生成订单编号，创建订单和订单项
     * 8. 状态初始化：设置订单初始状态和支付状态
     * 9. 事件通知：发送订单创建事件，触发后续流程
     * 10. 返回结果：返回订单详情和支付信息
     * 
     * 异常处理：
     * - 商品不存在或已下架：BusinessException
     * - 库存不足：InsufficientStockException
     * - 价格不一致：PriceInconsistentException
     * - 优惠券无效：InvalidCouponException
     * - 系统异常：SystemException
     * 
     * 性能优化：
     * - 批量查询商品信息减少数据库访问
     * - 使用Redis锁控制高并发下单
     * - 异步处理非关键业务逻辑
     * 
     * @param request 订单创建请求
     * @param userId 用户ID
     * @return 创建成功的订单信息
     * @throws BusinessException 业务逻辑异常
     */
    OrderCreationResult createOrder(OrderCreateRequest request, Long userId);

    /**
     * 取消订单
     * 
     * 取消流程：
     * 1. 权限验证：验证用户是否有权限取消订单
     * 2. 状态检查：检查订单当前状态是否允许取消
     * 3. 支付检查：检查订单支付状态和退款处理
     * 4. 库存释放：释放已预占的商品库存
     * 5. 优惠回滚：回滚优惠券使用和积分扣减
     * 6. 状态更新：更新订单状态为已取消
     * 7. 时间记录：记录取消时间和取消原因
     * 8. 退款处理：如已支付则发起退款流程
     * 9. 通知发送：通知用户和商家订单取消
     * 
     * 取消规则：
     * - 待支付订单：可直接取消
     * - 已支付未发货：可取消并退款
     * - 已发货：不能取消，只能申请退货
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param reason 取消原因
     * @return 取消是否成功
     * @throws BusinessException 业务逻辑异常
     */
    boolean cancelOrder(Long orderId, Long userId, String reason);

    /**
     * 删除订单（软删除）
     * 
     * 删除条件：
     * - 订单状态为已取消或已完成
     * - 用户本人或管理员权限
     * - 超过一定时间的历史订单
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 删除是否成功
     */
    boolean deleteOrder(Long orderId, Long userId);

    /**
     * 恢复已删除的订单
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 恢复是否成功
     */
    boolean restoreOrder(Long orderId, Long userId);

    // ========== 订单查询 ==========

    /**
     * 根据订单ID获取订单详情
     * 
     * 查询策略：
     * 1. 权限验证：验证用户是否有权限查看订单
     * 2. 缓存查询：优先从缓存中获取订单信息
     * 3. 数据库查询：缓存未命中时从数据库查询
     * 4. 关联数据：加载订单项、支付信息等关联数据
     * 5. 状态计算：计算订单当前可执行的操作
     * 6. 信息过滤：根据用户权限过滤敏感信息
     * 
     * @param orderId 订单ID
     * @param userId 用户ID（可选，用于权限控制）
     * @return 订单详情
     * @throws BusinessException 订单不存在或无权限查看
     */
    Optional<OrderDetailResponse> getOrderById(Long orderId, Long userId);

    /**
     * 根据订单编号获取订单详情
     * 
     * @param orderNo 订单编号
     * @param userId 用户ID（可选）
     * @return 订单详情
     */
    Optional<OrderDetailResponse> getOrderByNo(String orderNo, Long userId);

    /**
     * 分页查询用户的订单列表
     * 
     * 查询功能：
     * 1. 基础筛选：按状态、时间范围筛选
     * 2. 关键词搜索：按订单编号、商品名称搜索
     * 3. 排序支持：按创建时间、更新时间排序
     * 4. 分页处理：支持大数据量的分页查询
     * 5. 权限过滤：只返回用户有权限查看的订单
     * 
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param paymentStatus 支付状态（可选）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @param keyword 搜索关键词（可选）
     * @param pageable 分页参数
     * @return 订单分页列表
     */
    Page<OrderListResponse> getUserOrders(Long userId, Order.OrderStatus status, 
                                        Order.PaymentStatus paymentStatus,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        String keyword, Pageable pageable);

    /**
     * 管理员分页查询所有订单
     * 
     * @param status 订单状态（可选）
     * @param paymentStatus 支付状态（可选）
     * @param shippingStatus 物流状态（可选）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @param keyword 搜索关键词（可选）
     * @param pageable 分页参数
     * @return 订单分页列表
     */
    Page<OrderListResponse> getAllOrders(Order.OrderStatus status,
                                       Order.PaymentStatus paymentStatus,
                                       Order.ShippingStatus shippingStatus,
                                       LocalDateTime startDate, LocalDateTime endDate,
                                       String keyword, Pageable pageable);

    /**
     * 获取用户的订单数量统计
     * 
     * @param userId 用户ID
     * @return 各状态订单数量统计
     */
    Map<String, Long> getUserOrderCounts(Long userId);

    // ========== 支付管理 ==========

    /**
     * 发起订单支付
     * 
     * 支付流程：
     * 1. 订单验证：验证订单状态和支付条件
     * 2. 金额验证：验证支付金额与订单金额一致
     * 3. 支付方式：根据支付方式调用相应的支付接口
     * 4. 支付记录：创建支付记录，记录支付信息
     * 5. 状态更新：更新订单支付状态为支付中
     * 6. 返回支付：返回支付参数供前端调用
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param paymentMethod 支付方式
     * @return 支付信息
     * @throws BusinessException 支付条件不满足
     */
    PaymentResult initiatePayment(Long orderId, Long userId, String paymentMethod);

    /**
     * 处理支付回调
     * 
     * 回调处理：
     * 1. 签名验证：验证第三方支付的回调签名
     * 2. 订单匹配：根据订单号匹配对应订单
     * 3. 状态检查：检查订单当前状态是否符合预期
     * 4. 支付确认：确认支付结果和金额
     * 5. 状态更新：更新订单和支付状态
     * 6. 库存确认：确认预占库存转为已售
     * 7. 后续流程：触发发货等后续业务流程
     * 8. 通知发送：发送支付成功通知
     * 
     * @param paymentData 支付回调数据
     * @return 处理结果
     */
    PaymentCallbackResult handlePaymentCallback(Map<String, Object> paymentData);

    /**
     * 订单退款
     * 
     * 退款流程：
     * 1. 退款验证：验证退款条件和金额
     * 2. 退款申请：向第三方支付发起退款
     * 3. 状态更新：更新订单支付状态
     * 4. 库存处理：处理退货商品库存
     * 5. 优惠回滚：回滚优惠券和积分
     * 6. 通知发送：发送退款通知
     * 
     * @param orderId 订单ID
     * @param refundAmount 退款金额
     * @param reason 退款原因
     * @param operatorId 操作者ID
     * @return 退款结果
     */
    RefundResult refundOrder(Long orderId, BigDecimal refundAmount, String reason, Long operatorId);

    // ========== 订单履行 ==========

    /**
     * 订单发货
     * 
     * 发货流程：
     * 1. 权限验证：验证操作者权限
     * 2. 状态检查：检查订单是否可以发货
     * 3. 库存确认：确认商品库存和发货数量
     * 4. 物流信息：记录物流公司和快递单号
     * 5. 状态更新：更新订单状态为已发货
     * 6. 时间记录：记录发货时间
     * 7. 物流跟踪：启动物流状态跟踪
     * 8. 通知发送：发送发货通知给用户
     * 
     * @param orderId 订单ID
     * @param shippingCompany 物流公司
     * @param trackingNumber 快递单号
     * @param operatorId 操作者ID
     * @return 发货是否成功
     */
    boolean shipOrder(Long orderId, String shippingCompany, String trackingNumber, Long operatorId);

    /**
     * 更新物流状态
     * 
     * @param orderId 订单ID
     * @param shippingStatus 物流状态
     * @param statusDesc 状态描述
     * @param operatorId 操作者ID
     * @return 更新是否成功
     */
    boolean updateShippingStatus(Long orderId, Order.ShippingStatus shippingStatus, 
                               String statusDesc, Long operatorId);

    /**
     * 确认收货
     * 
     * 收货确认：
     * 1. 权限验证：验证用户权限
     * 2. 状态检查：检查订单是否可以确认收货
     * 3. 状态更新：更新订单状态为已签收
     * 4. 时间记录：记录签收时间
     * 5. 自动确认：启动自动确认收货定时任务
     * 6. 评价提醒：发送商品评价提醒
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 确认收货是否成功
     */
    boolean confirmDelivery(Long orderId, Long userId);

    /**
     * 完成订单
     * 
     * 完成条件：
     * - 订单已签收
     * - 超过自动确认时间
     * - 用户手动确认
     * 
     * @param orderId 订单ID
     * @param operatorId 操作者ID
     * @return 完成是否成功
     */
    boolean completeOrder(Long orderId, Long operatorId);

    // ========== 售后服务 ==========

    /**
     * 申请退货
     * 
     * 退货流程：
     * 1. 退货条件：检查商品是否支持退货
     * 2. 申请创建：创建退货申请记录
     * 3. 状态更新：更新订单项状态
     * 4. 审核流程：提交商家审核
     * 5. 通知发送：发送退货申请通知
     * 
     * @param orderId 订单ID
     * @param itemIds 退货商品项ID列表
     * @param reason 退货原因
     * @param description 退货描述
     * @param userId 用户ID
     * @return 退货申请结果
     */
    ReturnResult applyReturn(Long orderId, List<Long> itemIds, String reason, 
                           String description, Long userId);

    /**
     * 处理退货申请
     * 
     * @param returnId 退货申请ID
     * @param approved 是否批准
     * @param remark 处理备注
     * @param operatorId 操作者ID
     * @return 处理结果
     */
    boolean processReturn(Long returnId, boolean approved, String remark, Long operatorId);

    /**
     * 申请换货
     * 
     * @param orderId 订单ID
     * @param itemId 换货商品项ID
     * @param newSpec 新规格
     * @param reason 换货原因
     * @param userId 用户ID
     * @return 换货申请结果
     */
    ExchangeResult applyExchange(Long orderId, Long itemId, String newSpec, 
                               String reason, Long userId);

    // ========== 批量操作 ==========

    /**
     * 批量取消订单
     * 
     * @param orderIds 订单ID列表
     * @param reason 取消原因
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchCancelOrders(List<Long> orderIds, String reason, Long operatorId);

    /**
     * 批量发货
     * 
     * @param shippingInfos 发货信息列表
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchShipOrders(List<ShippingInfo> shippingInfos, Long operatorId);

    /**
     * 批量更新订单状态
     * 
     * @param orderIds 订单ID列表
     * @param status 目标状态
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchUpdateOrderStatus(List<Long> orderIds, Order.OrderStatus status, Long operatorId);

    // ========== 统计分析 ==========

    /**
     * 获取订单统计信息
     * 
     * 统计维度：
     * 1. 基础统计：订单总数、各状态分布
     * 2. 金额统计：总销售额、平均订单金额
     * 3. 时间统计：按日、周、月的订单趋势
     * 4. 用户统计：新老用户订单分布
     * 5. 商品统计：热销商品排行
     * 6. 地域统计：订单地域分布
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 订单统计信息
     */
    OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取用户订单统计
     * 
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 用户订单统计
     */
    UserOrderStatistics getUserOrderStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取订单趋势数据
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param dimension 统计维度（daily/weekly/monthly）
     * @return 订单趋势数据
     */
    List<OrderTrendData> getOrderTrend(LocalDateTime startDate, LocalDateTime endDate, String dimension);

    /**
     * 获取商品销售排行
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param limit 返回数量
     * @return 商品销售排行
     */
    List<ProductSalesRank> getProductSalesRank(LocalDateTime startDate, LocalDateTime endDate, int limit);

    // ========== 定时任务 ==========

    /**
     * 自动取消超时未支付订单
     * 
     * @return 取消的订单数量
     */
    int cancelTimeoutOrders();

    /**
     * 自动确认收货超时订单
     * 
     * @return 确认的订单数量
     */
    int autoConfirmDelivery();

    /**
     * 自动完成订单
     * 
     * @return 完成的订单数量
     */
    int autoCompleteOrders();

    /**
     * 释放超时预占库存
     * 
     * @return 释放的库存记录数
     */
    int releaseTimeoutStock();

    // ========== 业务结果类定义 ==========

    /**
     * 订单创建结果
     */
    record OrderCreationResult(
        boolean success,              // 创建是否成功
        String orderId,               // 订单ID
        String orderNo,               // 订单编号
        BigDecimal totalAmount,       // 订单总金额
        BigDecimal actualAmount,      // 实付金额
        String paymentUrl,            // 支付链接
        Map<String, Object> paymentParams, // 支付参数
        String message                // 结果消息
    ) {}

    /**
     * 支付结果
     */
    record PaymentResult(
        boolean success,              // 支付发起是否成功
        String paymentId,             // 支付ID
        String paymentUrl,            // 支付链接
        Map<String, Object> paymentParams, // 支付参数
        LocalDateTime expireTime,     // 支付过期时间
        String message                // 结果消息
    ) {}

    /**
     * 支付回调结果
     */
    record PaymentCallbackResult(
        boolean success,              // 处理是否成功
        String orderId,               // 订单ID
        String paymentStatus,         // 支付状态
        BigDecimal paidAmount,        // 支付金额
        LocalDateTime paidTime,       // 支付时间
        String message                // 结果消息
    ) {}

    /**
     * 退款结果
     */
    record RefundResult(
        boolean success,              // 退款是否成功
        String refundId,              // 退款ID
        BigDecimal refundAmount,      // 退款金额
        String refundStatus,          // 退款状态
        LocalDateTime refundTime,     // 退款时间
        String message                // 结果消息
    ) {}

    /**
     * 退货结果
     */
    record ReturnResult(
        boolean success,              // 申请是否成功
        String returnId,              // 退货申请ID
        String returnNo,              // 退货编号
        String status,                // 申请状态
        LocalDateTime applyTime,      // 申请时间
        String message                // 结果消息
    ) {}

    /**
     * 换货结果
     */
    record ExchangeResult(
        boolean success,              // 申请是否成功
        String exchangeId,            // 换货申请ID
        String exchangeNo,            // 换货编号
        String status,                // 申请状态
        LocalDateTime applyTime,      // 申请时间
        String message                // 结果消息
    ) {}

    /**
     * 批量操作结果
     */
    record BatchOperationResult(
        int total,                    // 总数
        int success,                  // 成功数
        int failed,                   // 失败数
        List<String> errors,          // 错误信息列表
        Map<String, Object> details   // 详细结果
    ) {}

    /**
     * 发货信息
     */
    record ShippingInfo(
        Long orderId,                 // 订单ID
        String shippingCompany,       // 物流公司
        String trackingNumber,        // 快递单号
        String remark                 // 备注
    ) {}

    /**
     * 订单统计信息
     */
    record OrderStatistics(
        long totalOrders,                     // 订单总数
        BigDecimal totalAmount,               // 总销售额
        BigDecimal avgOrderAmount,            // 平均订单金额
        Map<String, Long> statusDistribution, // 状态分布
        Map<String, BigDecimal> amountByStatus, // 各状态金额统计
        Map<String, Long> ordersByRegion,     // 地域分布
        long newCustomerOrders,               // 新客户订单数
        long returningCustomerOrders          // 老客户订单数
    ) {}

    /**
     * 用户订单统计
     */
    record UserOrderStatistics(
        long totalOrders,             // 总订单数
        BigDecimal totalAmount,       // 总消费金额
        BigDecimal avgOrderAmount,    // 平均订单金额
        long completedOrders,         // 完成订单数
        long cancelledOrders,         // 取消订单数
        String customerLevel,         // 客户等级
        LocalDateTime firstOrderTime, // 首次下单时间
        LocalDateTime lastOrderTime   // 最后下单时间
    ) {}

    /**
     * 订单趋势数据
     */
    record OrderTrendData(
        LocalDateTime date,           // 日期
        long orderCount,              // 订单数量
        BigDecimal orderAmount,       // 订单金额
        long customerCount,           // 客户数量
        BigDecimal avgOrderAmount     // 平均订单金额
    ) {}

    /**
     * 商品销售排行
     */
    record ProductSalesRank(
        Long productId,               // 商品ID
        String productName,           // 商品名称
        String productImage,          // 商品图片
        long salesCount,              // 销售数量
        BigDecimal salesAmount,       // 销售金额
        long orderCount,              // 订单数量
        BigDecimal avgPrice           // 平均价格
    ) {}

    /**
     * 订单详情响应
     */
    record OrderDetailResponse(
        String orderId,               // 订单ID
        String orderNo,               // 订单编号
        String status,                // 订单状态
        String paymentStatus,         // 支付状态
        String shippingStatus,        // 物流状态
        BigDecimal totalAmount,       // 订单总金额
        BigDecimal actualAmount,      // 实付金额
        List<OrderItemResponse> items, // 订单项列表
        ReceiverInfo receiverInfo,    // 收货信息
        PaymentInfo paymentInfo,      // 支付信息
        ShippingInfo shippingInfo,    // 物流信息
        LocalDateTime createdAt,      // 创建时间
        LocalDateTime updatedAt       // 更新时间
    ) {}

    /**
     * 订单列表响应
     */
    record OrderListResponse(
        String orderId,               // 订单ID
        String orderNo,               // 订单编号
        String status,                // 订单状态
        String paymentStatus,         // 支付状态
        BigDecimal actualAmount,      // 实付金额
        int itemCount,                // 商品数量  
        String firstProductImage,     // 第一个商品图片
        LocalDateTime createdAt       // 创建时间
    ) {}

    /**
     * 订单项响应
     */
    record OrderItemResponse(
        String productId,             // 商品ID
        String productName,           // 商品名称
        String productImage,          // 商品图片
        String productSpec,           // 商品规格
        BigDecimal unitPrice,         // 单价
        int quantity,                 // 数量
        BigDecimal subtotal           // 小计
    ) {}

    /**
     * 收货信息
     */
    record ReceiverInfo(
        String name,                  // 收货人
        String phone,                 // 手机号
        String address                // 收货地址
    ) {}

    /**
     * 支付信息
     */
    record PaymentInfo(
        String paymentMethod,         // 支付方式
        BigDecimal paidAmount,        // 支付金额
        LocalDateTime paidTime        // 支付时间
    ) {}
}