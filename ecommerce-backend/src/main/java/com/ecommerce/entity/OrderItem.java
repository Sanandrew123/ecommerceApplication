package com.ecommerce.entity;

/*
 * 文件职责: 订单项实体类，定义订单中的商品明细信息
 * 
 * 开发心理活动：
 * 1. 订单明细设计原则：
 *    - 数据快照：记录下单时的商品信息快照
 *    - 价格保护：避免商品价格变动影响已下单的订单
 *    - 完整性：包含计算订单总金额所需的所有信息
 *    - 追溯性：支持订单商品信息的历史追踪
 * 
 * 2. 电商订单项核心功能：
 *    - 商品信息：商品ID、名称、规格、图片等基本信息
 *    - 价格信息：单价、数量、小计等价格相关信息
 *    - 快照保存：下单时商品的完整信息快照
 *    - 状态管理：订单项的处理状态（正常、退货、换货等）
 * 
 * 3. 数据一致性考虑：
 *    - 金额计算：小计 = 单价 × 数量，确保计算准确性
 *    - 商品快照：避免商品信息变更影响历史订单展示
 *    - 关联完整性：与订单和商品的正确关联关系
 *    - 业务规则：数量、价格等业务约束的验证
 * 
 * 4. 性能优化思路：
 *    - 索引设计：订单ID、商品ID等常用查询字段建立索引
 *    - 数据冗余：适当冗余商品信息减少关联查询
 *    - 批量操作：支持订单项的批量创建和更新
 *    - 缓存策略：热点订单项数据的缓存处理
 * 
 * 包结构设计思路:
 * - 放在entity包下，作为订单领域的核心实体
 * - 与Order实体形成一对多的关联关系
 * 
 * 命名原因:
 * - OrderItem明确表达订单明细项的概念
 * - 符合电商业务领域的通用命名
 * 
 * 依赖关系:
 * - 多对一关联Order实体（订单主表）
 * - 关联Product实体（商品信息）
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

/**
 * 订单项实体类
 * 
 * 功能说明：
 * 1. 定义订单中每个商品的详细信息和数量
 * 2. 保存下单时商品的快照信息，避免后续变更影响
 * 3. 计算订单项的小计金额和相关费用
 * 4. 支持订单项级别的状态管理和业务操作
 * 
 * 业务特性：
 * 1. 商品快照：下单时保存商品的完整信息快照
 * 2. 价格计算：单价、数量、小计的准确计算
 * 3. 状态管理：支持订单项级别的状态跟踪
 * 4. 售后支持：支持单个商品的退货、换货等操作
 * 5. 优惠记录：记录商品级别的优惠信息
 * 6. 规格管理：支持商品规格选择和展示
 * 7. 库存关联：与商品库存系统的关联
 * 8. 评价关联：支持商品评价的关联
 * 
 * 数据设计：
 * 1. 主键策略：使用自增主键保证性能
 * 2. 外键关联：与订单和商品的正确关联
 * 3. 金额精度：使用BigDecimal确保金额计算精度
 * 4. 快照字段：冗余存储商品关键信息
 * 5. 索引优化：在常用查询字段建立索引
 * 6. 约束检查：数量、价格等业务约束
 * 7. 审计支持：创建时间、更新时间等审计字段
 * 8. 软删除：支持逻辑删除保留历史数据
 * 
 * 使用场景：
 * 1. 订单创建：根据购物车创建订单项
 * 2. 订单展示：展示订单的商品明细
 * 3. 金额计算：计算订单总金额和各项费用
 * 4. 库存扣减：根据订单项扣减商品库存
 * 5. 售后处理：处理单个商品的退货换货
 * 6. 数据统计：商品销量和收入统计
 * 7. 用户评价：关联商品评价和反馈
 * 8. 推荐系统：基于购买历史的商品推荐
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_id", columnList = "orderId"),
    @Index(name = "idx_product_id", columnList = "productId"),
    @Index(name = "idx_order_product", columnList = "orderId,productId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"order"}) // 避免循环引用
public class OrderItem {

    // ========== 基础字段 ==========

    /**
     * 订单项ID - 主键
     * 
     * 设计考虑：
     * - 使用自增策略保证唯一性和查询性能
     * - 作为订单项的唯一标识
     * - 支持订单项级别的操作和查询
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 订单ID - 外键
     * 
     * 关联设计：
     * - 外键关联orders表
     * - 用于查询指定订单的所有商品
     * - 支持订单聚合操作
     * - 级联删除策略
     */
    @Column(name = "order_id", nullable = false)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 商品ID - 外键
     * 
     * 关联设计：
     * - 外键关联products表
     * - 用于关联商品基本信息
     * - 支持商品维度的统计分析
     * - 库存扣减的依据
     */
    @Column(name = "product_id", nullable = false)
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    // ========== 商品快照信息 ==========

    /**
     * 商品名称快照
     * 
     * 快照设计考虑：
     * - 下单时保存商品名称，避免后续修改影响订单展示
     * - 确保订单历史数据的完整性和一致性
     * - 支持商品名称变更后的历史订单查看
     */
    @Column(name = "product_name", nullable = false, length = 255)
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称长度不能超过255")
    private String productName;

    /**
     * 商品编码快照
     * 
     * 用途：
     * - 商品的唯一标识码
     * - 库存管理和商品查找
     * - 与第三方系统对接的标识
     */
    @Column(name = "product_code", nullable = false, length = 100)
    @NotBlank(message = "商品编码不能为空")
    @Size(max = 100, message = "商品编码长度不能超过100")
    private String productCode;

    /**
     * 商品规格信息
     * 
     * 规格设计：
     * - 存储商品的规格选择信息（如：颜色、尺寸等）
     * - JSON格式存储，支持灵活的规格结构
     * - 用于订单展示和库存匹配
     */
    @Column(name = "product_spec", length = 1000)
    @Size(max = 1000, message = "商品规格信息长度不能超过1000")
    private String productSpec;

    /**
     * 商品图片URL
     * 
     * 图片快照：
     * - 保存下单时的商品主图
     * - 确保订单展示的图片一致性
     * - 支持订单历史的可视化展示
     */
    @Column(name = "product_image", length = 500)
    @Size(max = 500, message = "商品图片URL长度不能超过500")
    private String productImage;

    /**
     * 商品品牌
     */
    @Column(name = "product_brand", length = 100)
    @Size(max = 100, message = "商品品牌长度不能超过100")
    private String productBrand;

    /**
     * 商品分类ID
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 商品分类名称
     */
    @Column(name = "category_name", length = 100)
    @Size(max = 100, message = "商品分类名称长度不能超过100")
    private String categoryName;

    // ========== 价格和数量信息 ==========

    /**
     * 商品单价
     * 
     * 价格设计：
     * - 保存下单时的商品价格快照
     * - 使用BigDecimal确保精度
     * - 避免商品价格变动影响订单计算
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "商品单价不能为空")
    @DecimalMin(value = "0.01", message = "商品单价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品单价格式不正确")
    private BigDecimal unitPrice;

    /**
     * 购买数量
     * 
     * 数量约束：
     * - 必须为正整数
     * - 不能超过库存限制
     * - 支持商品的最小起订量
     */
    @Column(name = "quantity", nullable = false)
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    @Max(value = 99999, message = "购买数量不能超过99999")
    private Integer quantity;

    /**
     * 小计金额
     * 
     * 计算规则：
     * - 小计 = 单价 × 数量
     * - 自动计算，确保数据一致性
     * - 用于订单总金额的计算
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "小计金额不能为空")
    @DecimalMin(value = "0.01", message = "小计金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "小计金额格式不正确")
    private BigDecimal subtotal;

    // ========== 优惠信息 ==========

    /**
     * 商品原价
     * 
     * 用途：
     * - 记录商品的原始价格
     * - 计算优惠幅度
     * - 展示价格对比
     */
    @Column(name = "original_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "商品原价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "商品原价格式不正确")
    private BigDecimal originalPrice;

    /**
     * 优惠金额
     * 
     * 优惠来源：
     * - 商品折扣
     * - 会员优惠
     * - 满减活动
     * - 商品级优惠券
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "优惠金额格式不正确")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 优惠类型
     * 
     * 优惠类型：
     * - NONE: 无优惠
     * - DISCOUNT: 商品折扣
     * - PROMOTION: 促销活动
     * - COUPON: 优惠券
     * - MEMBER: 会员优惠
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType = DiscountType.NONE;

    /**
     * 优惠描述
     */
    @Column(name = "discount_desc", length = 200)
    @Size(max = 200, message = "优惠描述长度不能超过200")
    private String discountDesc;

    // ========== 状态信息 ==========

    /**
     * 订单项状态
     * 
     * 状态设计：
     * - NORMAL: 正常状态
     * - RETURNED: 已退货
     * - EXCHANGED: 已换货
     * - CANCELLED: 已取消
     * - REFUNDED: 已退款
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "订单项状态不能为空")
    private ItemStatus status = ItemStatus.NORMAL;

    /**
     * 退货数量
     * 
     * 退货管理：
     * - 支持部分退货
     * - 不能超过购买数量
     * - 用于计算实际销售数量
     */
    @Column(name = "returned_quantity")
    @Min(value = 0, message = "退货数量不能为负数")
    private Integer returnedQuantity = 0;

    /**
     * 退款金额
     * 
     * 退款计算：
     * - 基于退货数量按比例计算
     * - 考虑优惠分摊的退款处理
     * - 不能超过订单项的实付金额
     */
    @Column(name = "refund_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "退款金额格式不正确")
    private BigDecimal refundAmount = BigDecimal.ZERO;

    // ========== 权重和物流信息 ==========

    /**
     * 商品重量（克）
     * 
     * 物流计算：
     * - 用于运费计算
     * - 物流方式选择
     * - 包装规格确定
     */
    @Column(name = "weight")
    @Min(value = 0, message = "商品重量不能为负数")
    private Integer weight;

    /**
     * 商品体积（立方厘米）
     * 
     * 体积计算：
     * - 长 × 宽 × 高
     * - 用于物流费用计算
     * - 包装选择参考
     */
    @Column(name = "volume")
    @Min(value = 0, message = "商品体积不能为负数")
    private Integer volume;

    // ========== 评价信息 ==========

    /**
     * 是否已评价
     */
    @Column(name = "is_reviewed", nullable = false)
    private Boolean isReviewed = false;

    /**
     * 评价ID
     */
    @Column(name = "review_id")
    private Long reviewId;

    /**
     * 评价时间
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ========== 备注信息 ==========

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 500)
    @Size(max = 500, message = "备注信息长度不能超过500")
    private String remark;

    // ========== 审计字段 ==========

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
     * 关联订单
     * 
     * 关联设计：
     * - 多对一关系：多个订单项属于一个订单
     * - 不级联删除：订单删除时保留历史记录
     * - 延迟加载：避免N+1查询问题
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Order order;

    // ========== 枚举定义 ==========

    /**
     * 优惠类型枚举
     */
    public enum DiscountType {
        NONE("无优惠"),
        DISCOUNT("商品折扣"),
        PROMOTION("促销活动"),
        COUPON("优惠券"),
        MEMBER("会员优惠"),
        GROUP_BUY("团购优惠"),
        FLASH_SALE("秒杀优惠");

        private final String description;

        DiscountType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 订单项状态枚举
     */
    public enum ItemStatus {
        NORMAL("正常"),
        RETURNED("已退货"),
        EXCHANGED("已换货"),
        CANCELLED("已取消"),
        REFUNDED("已退款"),
        PARTIALLY_RETURNED("部分退货");

        private final String description;

        ItemStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 计算小计金额
     * 
     * 计算逻辑：
     * 小计 = 单价 × 数量
     * 
     * @return 计算后的小计金额
     */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 计算实付金额
     * 
     * 计算逻辑：
     * 实付金额 = 小计金额 - 优惠金额
     * 
     * @return 实付金额
     */
    public BigDecimal calculateActualAmount() {
        BigDecimal actualAmount = subtotal;
        if (discountAmount != null) {
            actualAmount = actualAmount.subtract(discountAmount);
        }
        return actualAmount.compareTo(BigDecimal.ZERO) > 0 ? actualAmount : BigDecimal.ZERO;
    }

    /**
     * 计算优惠比例
     * 
     * @return 优惠比例（0-1之间）
     */
    public BigDecimal calculateDiscountRatio() {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return discountAmount.divide(originalPrice, 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 检查是否可以退货
     * 
     * @return 是否可以退货
     */
    public boolean canReturn() {
        return status == ItemStatus.NORMAL && 
               (returnedQuantity == null || returnedQuantity < quantity);
    }

    /**
     * 检查是否可以换货
     * 
     * @return 是否可以换货
     */
    public boolean canExchange() {
        return status == ItemStatus.NORMAL;
    }

    /**
     * 计算可退货数量
     * 
     * @return 可退货数量
     */
    public int getAvailableReturnQuantity() {
        if (returnedQuantity == null) {
            return quantity;
        }
        return Math.max(0, quantity - returnedQuantity);
    }

    /**
     * 获取实际销售数量
     * 
     * @return 实际销售数量（购买数量 - 退货数量）
     */
    public int getActualSaleQuantity() {
        if (returnedQuantity == null) {
            return quantity;
        }
        return Math.max(0, quantity - returnedQuantity);
    }

    /**
     * 检查商品规格是否匹配
     * 
     * @param spec 商品规格
     * @return 是否匹配
     */
    public boolean matchesSpec(String spec) {
        if (productSpec == null && spec == null) {
            return true;
        }
        if (productSpec == null || spec == null) {
            return false;
        }
        return productSpec.equals(spec);
    }

    /**
     * JPA生命周期回调 - 持久化之前
     */
    @PrePersist
    protected void onCreate() {
        if (subtotal == null) {
            subtotal = calculateSubtotal();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (isReviewed == null) {
            isReviewed = false;
        }
        if (returnedQuantity == null) {
            returnedQuantity = 0;
        }
        if (refundAmount == null) {
            refundAmount = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
    }

    /**
     * JPA生命周期回调 - 更新之前
     */
    @PreUpdate
    protected void onUpdate() {
        subtotal = calculateSubtotal();
    }
}