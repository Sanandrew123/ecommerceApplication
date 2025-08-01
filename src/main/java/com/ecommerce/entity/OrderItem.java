/*
文件级分析：
- 职责：订单项实体类，映射数据库中的订单明细表，记录订单中的具体商品信息
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：OrderItem表示订单中的一个商品项目，清晰明确
- 调用关系：多对一关联Order订单，多对一关联Product商品

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 记录订单中每个商品的详细信息（快照机制）
3. 支持商品规格信息，如颜色、尺寸等
4. 包含退货退款相关字段
5. 冗余存储商品信息，避免商品变更影响历史订单
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 订单项实体类
 * 
 * 记录订单中每个商品的详细信息，采用快照机制设计：
 * 1. 商品信息：商品ID、名称、价格、规格等（下单时快照）
 * 2. 数量信息：购买数量、退货数量等
 * 3. 金额信息：单价、小计金额、优惠金额等
 * 4. 状态信息：发货状态、退货状态等
 * 5. 扩展信息：商品快照、规格参数等
 * 
 * 快照机制说明：
 * - 订单创建时，将商品的关键信息快照到订单项中
 * - 即使后续商品信息发生变更，历史订单仍保持一致
 * - 便于订单查询、统计和售后处理
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"order", "product"})
@ToString(callSuper = true, exclude = {"order", "product"})
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_order_product", columnList = "order_id, product_id")
})
public class OrderItem extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 关联关系字段 ========================
    
    /**
     * 关联订单
     * 多对一关联，多个订单项属于一个订单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_order"))
    @NotNull(message = "关联订单不能为空")
    @JsonIgnore
    private Order order;
    
    /**
     * 关联商品
     * 多对一关联，记录商品ID用于关联查询
     * 注意：这里主要用于ID关联，商品详细信息使用快照字段
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_product"))
    @NotNull(message = "关联商品不能为空")
    @JsonIgnore
    private Product product;
    
    // ======================== 商品快照信息字段 ========================
    
    /**
     * 商品名称（快照）
     * 下单时的商品名称，不随商品表变化
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200字符")
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    
    /**
     * 商品SKU（快照）
     * 下单时的商品SKU编码
     */
    @Size(max = 100, message = "商品SKU长度不能超过100字符")
    @Column(name = "product_sku", length = 100)
    private String productSku;
    
    /**
     * 商品品牌（快照）
     * 下单时的商品品牌
     */
    @Size(max = 100, message = "商品品牌长度不能超过100字符")
    @Column(name = "product_brand", length = 100)
    private String productBrand;
    
    /**
     * 商品分类名称（快照）
     * 下单时的商品分类
     */
    @Size(max = 100, message = "商品分类长度不能超过100字符")
    @Column(name = "category_name", length = 100)
    private String categoryName;
    
    /**
     * 商品主图（快照）
     * 下单时的商品主图URL
     */
    @Size(max = 500, message = "商品主图URL长度不能超过500字符")
    @Column(name = "product_image", length = 500)
    private String productImage;
    
    /**
     * 商品规格（快照）
     * JSON格式存储商品规格信息，如颜色、尺寸等
     */
    @Column(name = "product_specs", columnDefinition = "JSON")
    private String productSpecs;
    
    // ======================== 价格信息字段 ========================
    
    /**
     * 商品单价
     * 下单时的商品价格（快照）
     */
    @NotNull(message = "商品单价不能为空")
    @DecimalMin(value = "0.01", message = "商品单价必须大于0")
    @Digits(integer = 10, fraction = 2, message = "商品单价格式不正确")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    
    /**
     * 商品原价
     * 下单时的商品原价，用于显示优惠信息
     */
    @DecimalMin(value = "0.00", message = "商品原价不能为负数")
    @Digits(integer = 10, fraction = 2, message = "商品原价格式不正确")
    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;
    
    // ======================== 数量信息字段 ========================
    
    /**
     * 购买数量
     * 用户购买的商品数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    /**
     * 已发货数量
     * 已经发货的商品数量，支持部分发货
     */
    @Min(value = 0, message = "已发货数量不能为负数")
    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity = 0;
    
    /**
     * 已退货数量
     * 已经退货的商品数量
     */
    @Min(value = 0, message = "已退货数量不能为负数")
    @Column(name = "returned_quantity", nullable = false)
    private Integer returnedQuantity = 0;
    
    // ======================== 金额信息字段 ========================
    
    /**
     * 小计金额
     * 该订单项的总金额（单价 × 数量）
     */
    @NotNull(message = "小计金额不能为空")
    @DecimalMin(value = "0.00", message = "小计金额不能为负数")
    @Digits(integer = 12, fraction = 2, message = "小计金额格式不正确")
    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalAmount;
    
    /**
     * 优惠金额
     * 该订单项享受的优惠金额
     */
    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "优惠金额格式不正确")
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 实际金额
     * 该订单项的实际支付金额（小计金额 - 优惠金额）
     */
    @NotNull(message = "实际金额不能为空")
    @DecimalMin(value = "0.00", message = "实际金额不能为负数")
    @Digits(integer = 12, fraction = 2, message = "实际金额格式不正确")
    @Column(name = "actual_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal actualAmount;
    
    /**
     * 已退款金额
     * 该订单项已退还的金额
     */
    @DecimalMin(value = "0.00", message = "已退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "已退款金额格式不正确")
    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    // ======================== 状态信息字段 ========================
    
    /**
     * 发货状态
     * 1-待发货，2-部分发货，3-全部发货
     */
    @Column(name = "shipping_status", nullable = false)
    private Integer shippingStatus = 1;
    
    /**
     * 退货状态
     * 0-无退货，1-申请退货，2-退货中，3-已退货，4-退货拒绝
     */
    @Column(name = "return_status", nullable = false)
    private Integer returnStatus = 0;
    
    /**
     * 是否已评价
     * 用户是否已对该商品进行评价
     */
    @Column(name = "is_reviewed", nullable = false)
    private Boolean isReviewed = false;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 商品快照
     * JSON格式存储下单时商品的完整信息
     */
    @Column(name = "product_snapshot", columnDefinition = "JSON")
    private String productSnapshot;
    
    /**
     * 备注信息
     * 针对该订单项的特殊说明
     */
    @Size(max = 500, message = "备注信息长度不能超过500字符")
    @Column(name = "remark", length = 500)
    private String remark;
    
    /**
     * 扩展属性
     * JSON格式存储订单项的额外属性
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    // ======================== 业务方法 ========================
    
    /**
     * 计算小计金额
     * 单价 × 数量
     * 
     * @return 计算后的小计金额
     */
    public BigDecimal calculateSubtotalAmount() {
        if (this.unitPrice != null && this.quantity != null) {
            return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * 计算实际金额
     * 小计金额 - 优惠金额
     * 
     * @return 计算后的实际金额
     */
    public BigDecimal calculateActualAmount() {
        BigDecimal subtotal = this.subtotalAmount != null ? this.subtotalAmount : BigDecimal.ZERO;
        BigDecimal discount = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        return subtotal.subtract(discount).max(BigDecimal.ZERO);
    }
    
    /**
     * 判断是否可以发货
     * 
     * @return true表示可以发货，false表示不能发货
     */
    public boolean canShip() {
        return this.shippedQuantity < this.quantity && this.returnStatus != 1;
    }
    
    /**
     * 判断是否全部发货
     * 
     * @return true表示全部发货，false表示未全部发货
     */
    public boolean isFullyShipped() {
        return this.shippedQuantity.equals(this.quantity);
    }
    
    /**
     * 判断是否可以退货
     * 
     * @return true表示可以退货，false表示不能退货
     */
    public boolean canReturn() {
        return this.shippedQuantity > this.returnedQuantity && this.returnStatus == 0;
    }
    
    /**
     * 判断是否可以评价
     * 
     * @return true表示可以评价，false表示不能评价
     */
    public boolean canReview() {
        return this.shippedQuantity > 0 && !this.isReviewed;
    }
    
    /**
     * 发货处理
     * 
     * @param shipQuantity 本次发货数量
     * @return true表示发货成功，false表示发货失败
     */
    public boolean ship(int shipQuantity) {
        if (shipQuantity <= 0 || shipQuantity > (this.quantity - this.shippedQuantity)) {
            return false;
        }
        
        this.shippedQuantity += shipQuantity;
        
        // 更新发货状态
        if (this.shippedQuantity.equals(this.quantity)) {
            this.shippingStatus = 3; // 全部发货
        } else {
            this.shippingStatus = 2; // 部分发货
        }
        
        return true;
    }
    
    /**
     * 退货处理
     * 
     * @param returnQuantity 退货数量
     * @return true表示退货成功，false表示退货失败
     */
    public boolean returnItem(int returnQuantity) {
        if (returnQuantity <= 0 || returnQuantity > (this.shippedQuantity - this.returnedQuantity)) {
            return false;
        }
        
        this.returnedQuantity += returnQuantity;
        this.returnStatus = 3; // 已退货
        
        return true;
    }
    
    /**
     * 获取可退货数量
     * 
     * @return 可退货的数量
     */
    public int getReturnableQuantity() {
        return this.shippedQuantity - this.returnedQuantity;
    }
    
    /**
     * 获取可发货数量
     * 
     * @return 可发货的数量
     */
    public int getShippableQuantity() {
        return this.quantity - this.shippedQuantity;
    }
    
    /**
     * 计算可退款金额
     * 
     * @return 可退款的金额
     */
    public BigDecimal getRefundableAmount() {
        return this.actualAmount.subtract(this.refundedAmount);
    }
    
    /**
     * 设置评价状态
     */
    public void markAsReviewed() {
        this.isReviewed = true;
    }
    
    /**
     * 获取发货状态描述
     * 
     * @return 发货状态的中文描述
     */
    public String getShippingStatusDescription() {
        return switch (this.shippingStatus) {
            case 1 -> "待发货";
            case 2 -> "部分发货";
            case 3 -> "全部发货";
            default -> "未知";
        };
    }
    
    /**
     * 获取退货状态描述
     * 
     * @return 退货状态的中文描述
     */
    public String getReturnStatusDescription() {
        return switch (this.returnStatus) {
            case 0 -> "无退货";
            case 1 -> "申请退货";
            case 2 -> "退货中";
            case 3 -> "已退货";
            case 4 -> "退货拒绝";
            default -> "未知";
        };
    }
}