package com.ecommerce.entity;

/*
 * 文件职责: 订单实体类，定义电商系统中的订单信息和状态管理
 * 
 * 开发心理活动：
 * 1. 订单系统设计原则：
 *    - 状态管理：订单从创建到完成的完整生命周期
 *    - 数据完整性：保证订单数据的一致性和准确性
 *    - 业务拓展性：支持各种促销、优惠券、积分等功能
 *    - 审计追踪：记录订单的所有变更历史
 * 
 * 2. 电商订单核心业务：
 *    - 订单创建：用户下单时的订单信息记录
 *    - 支付管理：订单支付状态和支付信息
 *    - 物流跟踪：订单发货、配送、签收状态
 *    - 售后处理：退款、退货、换货等售后服务
 * 
 * 3. 订单状态设计考虑：
 *    - 状态流转：明确的状态流转规则和约束
 *    - 并发控制：多用户同时操作订单的并发安全
 *    - 异常处理：支付失败、库存不足等异常情况
 *    - 状态回滚：支持订单状态的回滚和修正
 * 
 * 4. 数据建模思路：
 *    - 订单主表：存储订单基本信息和状态
 *    - 关联设计：与用户、商品、支付、物流的关联关系
 *    - 金额计算：订单总金额、优惠金额、实付金额
 *    - 时间记录：订单各个关键节点的时间戳
 * 
 * 包结构设计思路:
 * - 放在entity包下，作为核心业务实体
 * - 与User、Product等实体形成完整的业务模型
 * 
 * 命名原因:
 * - Order明确表达订单业务概念
 * - 符合领域模型的命名规范
 * 
 * 依赖关系:
 * - 关联User实体（买家信息）
 * - 关联OrderItem实体（订单明细）
 * - 关联Payment实体（支付信息）
 * - 被OrderService等业务层使用
 */

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体类
 * 
 * 功能说明：
 * 1. 定义电商系统中订单的完整信息结构
 * 2. 管理订单从创建到完成的整个生命周期
 * 3. 支持订单状态流转和业务规则验证
 * 4. 提供订单金额计算和优惠处理功能
 * 
 * 业务特性：
 * 1. 订单状态管理：支持完整的订单状态流转
 * 2. 金额计算：商品金额、运费、优惠、实付金额
 * 3. 时间追踪：记录订单各个关键时间节点
 * 4. 用户关联：关联买家和可选的收货人信息
 * 5. 商品明细：一对多关联订单商品明细
 * 6. 支付集成：关联支付信息和支付状态
 * 7. 物流信息：配送地址和物流跟踪
 * 8. 售后支持：退款、退货、换货状态
 * 
 * 数据设计：
 * 1. 主键策略：使用自增主键保证唯一性
 * 2. 订单编号：业务唯一编号，用于对外展示
 * 3. 金额字段：使用BigDecimal保证精度
 * 4. 枚举状态：使用枚举管理订单和支付状态
 * 5. 关联关系：合理的外键关联和级联策略
 * 6. 索引优化：在常用查询字段上建立索引
 * 7. 审计字段：创建时间、更新时间、操作人
 * 8. 软删除：支持逻辑删除保留历史数据
 * 
 * 使用场景：
 * 1. 用户下单：创建新订单记录
 * 2. 订单查询：根据各种条件查询订单
 * 3. 状态更新：订单支付、发货、完成等状态变更
 * 4. 金额计算：计算订单各项金额和优惠
 * 5. 报表统计：订单数据统计和分析
 * 6. 售后处理：退款退货等售后业务
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_no", columnList = "orderNo", unique = true),
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_payment_status", columnList = "paymentStatus"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_status", columnList = "userId,status"),
    @Index(name = "idx_created_status", columnList = "createdAt,status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"orderItems"}) // 避免循环引用
public class Order {

    // ========== 基础字段 ==========

    /**
     * 订单ID - 主键
     * 
     * 设计考虑：
     * - 使用自增策略保证唯一性和性能
     * - 作为内部系统的主键使用
     * - 不对外暴露，使用orderNo作为业务编号
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 订单编号 - 业务唯一标识
     * 
     * 编号规则设计：
     * - 格式：yyyyMMddHHmmss + 6位随机数
     * - 示例：20240804143025123456
     * - 用途：对外展示、用户查询、客服处理
     * - 特性：唯一性、可读性、时间含义
     * 
     * 业务价值：
     * - 用户友好：便于用户记忆和查询
     * - 客服支持：便于客服快速定位订单
     * - 系统集成：第三方系统对接的标识
     * - 数据分析：可从编号中提取时间信息
     */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    @NotBlank(message = "订单编号不能为空")
    @Size(max = 32, message = "订单编号长度不能超过32位")
    private String orderNo;

    /**
     * 用户ID - 买家标识
     * 
     * 关联设计：
     * - 外键关联users表
     * - 支持用户订单查询
     * - 用于权限验证和数据隔离
     * - 统计用户消费行为
     */
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户信息快照
     * 
     * 快照设计考虑：
     * - 订单创建时记录用户信息快照
     * - 避免用户信息变更影响历史订单
     * - 便于订单数据的完整性和一致性
     * - 支持用户信息的历史追踪
     */
    @Column(name = "user_name", length = 100)
    @Size(max = 100, message = "用户姓名长度不能超过100")
    private String userName;

    @Column(name = "user_phone", length = 20)
    @Size(max = 20, message = "用户手机号长度不能超过20")
    private String userPhone;

    @Column(name = "user_email", length = 255)
    @Email(message = "用户邮箱格式不正确")
    @Size(max = 255, message = "用户邮箱长度不能超过255")
    private String userEmail;

    // ========== 订单状态 ==========

    /**
     * 订单状态
     * 
     * 状态流转设计：
     * PENDING(待支付) -> PAID(已支付) -> SHIPPED(已发货) -> DELIVERED(已签收) -> COMPLETED(已完成)
     *                                                                    -> CANCELLED(已取消)
     *                                                     -> RETURNED(已退货)
     *                 -> CANCELLED(已取消)
     * 
     * 状态说明：
     * - PENDING: 订单已创建，等待支付
     * - PAID: 订单已支付，等待发货
     * - SHIPPED: 订单已发货，在途中
     * - DELIVERED: 订单已签收，等待确认
     * - COMPLETED: 订单已完成，交易结束
     * - CANCELLED: 订单已取消
     * - RETURNED: 订单已退货
     * - REFUNDED: 订单已退款
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "订单状态不能为空")
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * 支付状态
     * 
     * 支付状态设计：
     * - UNPAID: 未支付
     * - PAYING: 支付中
     * - PAID: 已支付
     * - FAILED: 支付失败
     * - REFUNDING: 退款中
     * - REFUNDED: 已退款
     * - PARTIAL_REFUNDED: 部分退款
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @NotNull(message = "支付状态不能为空")
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /**
     * 物流状态
     * 
     * 物流状态设计：
     * - NOT_SHIPPED: 未发货
     * - PREPARING: 备货中
     * - SHIPPED: 已发货
     * - IN_TRANSIT: 运输中
     * - OUT_FOR_DELIVERY: 派送中
     * - DELIVERED: 已签收
     * - RETURNED: 已退回
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 20)
    @NotNull(message = "物流状态不能为空")
    private ShippingStatus shippingStatus = ShippingStatus.NOT_SHIPPED;

    // ========== 金额信息 ==========

    /**
     * 商品总金额
     * 
     * 计算规则：
     * - 所有订单项的小计金额之和
     * - 不包含运费和优惠
     * - 用于金额校验和统计
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "商品总金额不能为空")
    @DecimalMin(value = "0.01", message = "商品总金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品总金额格式不正确")
    private BigDecimal totalAmount;

    /**
     * 运费
     * 
     * 运费计算：
     * - 基于商品重量、体积、配送距离
     * - 支持免运费条件设置
     * - 可与优惠券组合使用
     */
    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "运费不能为空")
    @DecimalMin(value = "0.00", message = "运费不能为负数")
    @Digits(integer = 8, fraction = 2, message = "运费格式不正确")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * 优惠金额
     * 
     * 优惠来源：
     * - 优惠券优惠
     * - 会员折扣
     * - 满减活动
     * - 积分抵扣
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "优惠金额格式不正确")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 实付金额
     * 
     * 计算公式：
     * 实付金额 = 商品总金额 + 运费 - 优惠金额
     * 
     * 约束条件：
     * - 实付金额必须大于0
     * - 实付金额 = totalAmount + shippingFee - discountAmount
     */
    @Column(name = "actual_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.01", message = "实付金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "实付金额格式不正确")
    private BigDecimal actualAmount;

    // ========== 收货信息 ==========

    /**
     * 收货人姓名
     */
    @Column(name = "receiver_name", nullable = false, length = 100)
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 100, message = "收货人姓名长度不能超过100")
    private String receiverName;

    /**
     * 收货人手机号
     */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人手机号格式不正确")
    private String receiverPhone;

    /**
     * 收货人邮箱
     */
    @Column(name = "receiver_email", length = 255)
    @Email(message = "收货人邮箱格式不正确")
    @Size(max = 255, message = "收货人邮箱长度不能超过255")
    private String receiverEmail;

    /**
     * 收货地址 - 省份
     */
    @Column(name = "receiver_province", nullable = false, length = 50)
    @NotBlank(message = "收货省份不能为空")
    @Size(max = 50, message = "收货省份长度不能超过50")
    private String receiverProvince;

    /**
     * 收货地址 - 城市
     */
    @Column(name = "receiver_city", nullable = false, length = 50)
    @NotBlank(message = "收货城市不能为空")
    @Size(max = 50, message = "收货城市长度不能超过50")
    private String receiverCity;

    /**
     * 收货地址 - 区县
     */
    @Column(name = "receiver_district", nullable = false, length = 50)
    @NotBlank(message = "收货区县不能为空")
    @Size(max = 50, message = "收货区县长度不能超过50")
    private String receiverDistrict;

    /**
     * 收货地址 - 详细地址
     */
    @Column(name = "receiver_address", nullable = false, length = 500)
    @NotBlank(message = "详细收货地址不能为空")
    @Size(max = 500, message = "详细收货地址长度不能超过500")
    private String receiverAddress;

    /**
     * 收货地址 - 邮政编码
     */
    @Column(name = "receiver_zip_code", length = 10)
    @Pattern(regexp = "^\\d{6}$", message = "邮政编码格式不正确")
    private String receiverZipCode;

    // ========== 物流信息 ==========

    /**
     * 物流公司
     */
    @Column(name = "shipping_company", length = 100)
    @Size(max = 100, message = "物流公司名称长度不能超过100")
    private String shippingCompany;

    /**
     * 物流单号
     */
    @Column(name = "tracking_number", length = 100)
    @Size(max = 100, message = "物流单号长度不能超过100")
    private String trackingNumber;

    /**
     * 发货时间
     */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /**
     * 签收时间
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // ========== 优惠信息 ==========

    /**
     * 使用的优惠券ID
     */
    @Column(name = "coupon_id")
    private Long couponId;

    /**
     * 优惠券编号
     */
    @Column(name = "coupon_code", length = 50)
    @Size(max = 50, message = "优惠券编号长度不能超过50")
    private String couponCode;

    /**
     * 积分抵扣金额
     */
    @Column(name = "points_discount", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "积分抵扣金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "积分抵扣金额格式不正确")
    private BigDecimal pointsDiscount = BigDecimal.ZERO;

    /**
     * 使用积分数量
     */
    @Column(name = "points_used")
    @Min(value = 0, message = "使用积分数量不能为负数")
    private Integer pointsUsed = 0;

    // ========== 订单备注 ==========

    /**
     * 用户备注
     */
    @Column(name = "user_remark", length = 500)
    @Size(max = 500, message = "用户备注长度不能超过500")
    private String userRemark;

    /**
     * 管理员备注
     */
    @Column(name = "admin_remark", length = 500)
    @Size(max = 500, message = "管理员备注长度不能超过500")
    private String adminRemark;

    // ========== 时间信息 ==========

    /**
     * 订单创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 订单更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 支付时间
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 取消时间
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ========== 审计字段 ==========

    /**
     * 创建者ID
     */
    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 最后修改者ID
     */
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private Long lastModifiedBy;

    /**
     * 版本号 - 乐观锁
     */
    @Version
    @Column(name = "version")
    private Long version = 0L;

    /**
     * 逻辑删除标记
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // ========== 关联关系 ==========

    /**
     * 订单明细列表
     * 
     * 关联设计：
     * - 一对多关系：一个订单包含多个订单项
     * - 级联操作：订单删除时级联删除订单项
     * - 延迟加载：避免N+1查询问题
     * - 排序规则：按订单项ID排序
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // ========== 订单状态枚举 ==========

    /**
     * 订单状态枚举
     */
    public enum OrderStatus {
        PENDING("待支付"),
        PAID("已支付"),
        SHIPPED("已发货"),
        DELIVERED("已签收"),
        COMPLETED("已完成"),
        CANCELLED("已取消"),
        RETURNED("已退货"),
        REFUNDED("已退款");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 支付状态枚举
     */
    public enum PaymentStatus {
        UNPAID("未支付"),
        PAYING("支付中"),
        PAID("已支付"),
        FAILED("支付失败"),
        REFUNDING("退款中"),
        REFUNDED("已退款"),
        PARTIAL_REFUNDED("部分退款");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 物流状态枚举
     */
    public enum ShippingStatus {
        NOT_SHIPPED("未发货"),
        PREPARING("备货中"),
        SHIPPED("已发货"),
        IN_TRANSIT("运输中"),
        OUT_FOR_DELIVERY("派送中"),
        DELIVERED("已签收"),
        RETURNED("已退回");

        private final String description;

        ShippingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 计算实付金额
     * 
     * 计算逻辑：
     * 实付金额 = 商品总金额 + 运费 - 优惠金额 - 积分抵扣
     * 
     * @return 计算后的实付金额
     */
    public BigDecimal calculateActualAmount() {
        BigDecimal amount = totalAmount
                .add(shippingFee != null ? shippingFee : BigDecimal.ZERO)
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .subtract(pointsDiscount != null ? pointsDiscount : BigDecimal.ZERO);
        
        // 确保实付金额不为负数
        return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
    }

    /**
     * 检查订单是否可以取消
     * 
     * @return 是否可以取消
     */
    public boolean canCancel() {
        return status == OrderStatus.PENDING || 
               (status == OrderStatus.PAID && shippingStatus == ShippingStatus.NOT_SHIPPED);
    }

    /**
     * 检查订单是否可以发货
     * 
     * @return 是否可以发货
     */
    public boolean canShip() {
        return status == OrderStatus.PAID && shippingStatus == ShippingStatus.NOT_SHIPPED;
    }

    /**
     * 检查订单是否可以完成
     * 
     * @return 是否可以完成
     */
    public boolean canComplete() {
        return status == OrderStatus.DELIVERED && paymentStatus == PaymentStatus.PAID;
    }

    /**
     * 检查订单是否可以申请退款
     * 
     * @return 是否可以申请退款
     */
    public boolean canRefund() {
        return (status == OrderStatus.PAID || status == OrderStatus.SHIPPED || 
                status == OrderStatus.DELIVERED) && 
               paymentStatus == PaymentStatus.PAID;
    }

    /**
     * 获取完整收货地址
     * 
     * @return 完整的收货地址字符串
     */
    public String getFullAddress() {
        return receiverProvince + receiverCity + receiverDistrict + receiverAddress;
    }

    /**
     * 添加订单项
     * 
     * @param orderItem 订单项
     */
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 移除订单项
     * 
     * @param orderItem 订单项
     */
    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }

    /**
     * 清空订单项
     */
    public void clearOrderItems() {
        orderItems.forEach(item -> item.setOrder(null));
        orderItems.clear();
    }

    /**
     * JPA生命周期回调 - 持久化之前
     */
    @PrePersist
    protected void onCreate() {
        if (actualAmount == null) {
            actualAmount = calculateActualAmount();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    /**
     * JPA生命周期回调 - 更新之前
     */
    @PreUpdate
    protected void onUpdate() {
        actualAmount = calculateActualAmount();
    }
}