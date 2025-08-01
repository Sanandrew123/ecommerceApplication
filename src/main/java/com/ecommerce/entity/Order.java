/*
文件级分析：
- 职责：订单实体类，映射数据库中的订单主表，是电商系统的核心交易实体
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：Order是电商业务中的核心概念，代表一次完整的交易
- 调用关系：关联User用户、OrderItem订单项，被Payment支付记录引用

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 包含订单的完整信息：基本信息、金额、地址、状态等
3. 支持订单拆分、合并的设计考虑
4. 集成物流跟踪和时间节点记录
5. 预留营销活动和优惠券扩展空间
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.ecommerce.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体类
 * 
 * 电商系统的核心交易实体，包含订单的完整信息：
 * 1. 基本信息：订单号、用户、创建时间等
 * 2. 金额信息：商品金额、运费、优惠、实付金额等
 * 3. 收货信息：收货人、地址、电话等
 * 4. 状态信息：订单状态、各环节时间节点
 * 5. 物流信息：快递公司、运单号、跟踪信息
 * 6. 扩展信息：备注、标签、营销活动等
 * 
 * 业务特点：
 * - 订单一旦创建，基本信息不可修改（金额、商品等）
 * - 状态流转有严格的业务规则约束
 * - 支持部分退款、部分发货等复杂场景
 * - 记录完整的时间轨迹，便于客服和分析
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"user", "orderItems"})
@ToString(callSuper = true, exclude = {"user", "orderItems"})
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_no", columnList = "order_no", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_user_status", columnList = "user_id, status"),
        @Index(name = "idx_created_time", columnList = "created_at DESC"),
        @Index(name = "idx_total_amount", columnList = "total_amount DESC"),
        @Index(name = "idx_payment_time", columnList = "payment_time")
})
public class Order extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 订单号
     * 全局唯一的订单标识符，对用户可见
     * 格式建议：年月日 + 随机数，如：20240101001234567
     */
    @NotBlank(message = "订单号不能为空")
    @Size(max = 50, message = "订单号长度不能超过50字符")
    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;
    
    /**
     * 关联用户
     * 多对一关联，每个订单属于一个用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_user"))
    @NotNull(message = "订单用户不能为空")
    @JsonIgnore
    private User user;
    
    /**
     * 订单状态
     * 使用枚举映射，控制订单的生命周期
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    
    /**
     * 订单类型
     * 1-普通订单，2-预售订单，3-团购订单，4-秒杀订单
     */
    @Column(name = "order_type", nullable = false)
    private Integer orderType = 1;
    
    /**
     * 订单来源
     * 1-PC端，2-移动端，3-微信小程序，4-APP
     */
    @Column(name = "source", nullable = false)
    private Integer source = 1;
    
    // ======================== 金额信息字段 ========================
    
    /**
     * 商品总金额
     * 所有商品的小计金额之和（不含优惠）
     */
    @NotNull(message = "商品总金额不能为空")
    @DecimalMin(value = "0.00", message = "商品总金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "商品总金额格式不正确")
    @Column(name = "product_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal productAmount;
    
    /**
     * 运费金额
     * 配送费用
     */
    @DecimalMin(value = "0.00", message = "运费金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "运费金额格式不正确")
    @Column(name = "shipping_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    
    /**
     * 优惠金额
     * 各种优惠活动的总优惠金额
     */
    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "优惠金额格式不正确")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 实付金额
     * 用户实际需要支付的金额（商品金额 + 运费 - 优惠）
     */
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.01", message = "实付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "实付金额格式不正确")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    /**
     * 已支付金额
     * 实际已支付的金额，支持部分支付场景
     */
    @DecimalMin(value = "0.00", message = "已支付金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "已支付金额格式不正确")
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    /**
     * 已退款金额
     * 已退还给用户的金额
     */
    @DecimalMin(value = "0.00", message = "已退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "已退款金额格式不正确")
    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    // ======================== 收货信息字段 ========================
    
    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过50字符")
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;
    
    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人电话格式不正确")
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;
    
    /**
     * 省份
     */
    @NotBlank(message = "省份不能为空")
    @Size(max = 20, message = "省份长度不能超过20字符")
    @Column(name = "province", nullable = false, length = 20)
    private String province;
    
    /**
     * 城市
     */
    @NotBlank(message = "城市不能为空")
    @Size(max = 20, message = "城市长度不能超过20字符")
    @Column(name = "city", nullable = false, length = 20)
    private String city;
    
    /**
     * 区县
     */
    @NotBlank(message = "区县不能为空")
    @Size(max = 20, message = "区县长度不能超过20字符")
    @Column(name = "district", nullable = false, length = 20)
    private String district;
    
    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址长度不能超过200字符")
    @Column(name = "address", nullable = false, length = 200)
    private String address;
    
    /**
     * 邮政编码
     */
    @Pattern(regexp = "^\\d{6}$", message = "邮政编码格式不正确")
    @Column(name = "postal_code", length = 6)
    private String postalCode;
    
    // ======================== 物流信息字段 ========================
    
    /**
     * 快递公司编码
     * 如：SF（顺丰）、YTO（圆通）、ZTO（中通）等
     */
    @Size(max = 20, message = "快递公司编码长度不能超过20字符")
    @Column(name = "shipping_company", length = 20)
    private String shippingCompany;
    
    /**
     * 快递公司名称
     */
    @Size(max = 50, message = "快递公司名称长度不能超过50字符")
    @Column(name = "shipping_company_name", length = 50)
    private String shippingCompanyName;
    
    /**
     * 快递单号
     */
    @Size(max = 50, message = "快递单号长度不能超过50字符")
    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;
    
    /**
     * 物流状态
     * 1-待发货，2-已发货，3-运输中，4-派件中，5-已签收，6-异常
     */
    @Column(name = "shipping_status")
    private Integer shippingStatus;
    
    /**
     * 物流信息
     * JSON格式存储物流跟踪信息
     */
    @Column(name = "shipping_info", columnDefinition = "JSON")
    private String shippingInfo;
    
    // ======================== 时间节点字段 ========================
    
    /**
     * 支付时间
     * 用户完成支付的时间
     */
    @Column(name = "payment_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;
    
    /**
     * 发货时间
     * 商家发货的时间
     */
    @Column(name = "shipping_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shippingTime;
    
    /**
     * 送达时间
     * 快递送达的时间
     */
    @Column(name = "delivery_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveryTime;
    
    /**
     * 完成时间
     * 用户确认收货的时间
     */
    @Column(name = "completion_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;
    
    /**
     * 取消时间
     * 订单被取消的时间
     */
    @Column(name = "cancellation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancellationTime;
    
    /**
     * 支付超时时间
     * 订单支付的截止时间，超时后自动取消
     */
    @Column(name = "payment_timeout")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTimeout;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 用户备注
     * 用户下单时的留言
     */
    @Size(max = 500, message = "用户备注长度不能超过500字符")
    @Column(name = "user_remark", length = 500)
    private String userRemark;
    
    /**
     * 商家备注
     * 商家处理订单时的内部备注
     */
    @Size(max = 500, message = "商家备注长度不能超过500字符")
    @Column(name = "merchant_remark", length = 500)
    private String merchantRemark;
    
    /**
     * 取消原因
     * 订单被取消的原因
     */
    @Size(max = 200, message = "取消原因长度不能超过200字符")
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;
    
    /**
     * 优惠券ID
     * 使用的优惠券标识
     */
    @Column(name = "coupon_id")
    private Long couponId;
    
    /**
     * 促销活动ID
     * 参与的促销活动标识
     */
    @Column(name = "promotion_id")
    private Long promotionId;
    
    /**
     * 扩展属性
     * JSON格式存储订单的额外属性
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    // ======================== 关联关系 ========================
    
    /**
     * 订单项列表
     * 一对多关联，一个订单包含多个订单项
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @JsonIgnore
    private List<OrderItem> orderItems = new ArrayList<>();
    
    // ======================== 业务方法 ========================
    
    /**
     * 获取完整收货地址
     * 
     * @return 格式化的完整地址
     */
    public String getFullAddress() {
        return String.format("%s %s %s %s", 
                province != null ? province : "",
                city != null ? city : "",
                district != null ? district : "",
                address != null ? address : "").trim();
    }
    
    /**
     * 判断订单是否可以支付
     * 
     * @return true表示可以支付，false表示不能支付
     */
    public boolean canPay() {
        return this.status == OrderStatus.PENDING_PAYMENT && 
               this.paymentTimeout != null && 
               LocalDateTime.now().isBefore(this.paymentTimeout);
    }
    
    /**
     * 判断订单是否支付超时
     * 
     * @return true表示支付超时，false表示未超时
     */
    public boolean isPaymentTimeout() {
        return this.status == OrderStatus.PENDING_PAYMENT && 
               this.paymentTimeout != null && 
               LocalDateTime.now().isAfter(this.paymentTimeout);
    }
    
    /**
     * 判断订单是否可以取消
     * 
     * @return true表示可以取消，false表示不能取消
     */
    public boolean canCancel() {
        return this.status != null && this.status.isCancellable();
    }
    
    /**
     * 判断订单是否可以申请退款
     * 
     * @return true表示可以申请退款，false表示不能申请退款
     */
    public boolean canRefund() {
        return this.status != null && this.status.isRefundable();
    }
    
    /**
     * 判断订单是否可以确认收货
     * 
     * @return true表示可以确认收货，false表示不能确认收货
     */
    public boolean canConfirmDelivery() {
        return this.status == OrderStatus.DELIVERED;
    }
    
    /**
     * 判断订单是否可以评价
     * 
     * @return true表示可以评价，false表示不能评价
     */
    public boolean canReview() {
        return this.status != null && this.status.canReview();
    }
    
    /**
     * 计算订单总金额
     * 商品金额 + 运费 - 优惠金额
     * 
     * @return 计算后的总金额
     */
    public BigDecimal calculateTotalAmount() {
        BigDecimal total = this.productAmount != null ? this.productAmount : BigDecimal.ZERO;
        total = total.add(this.shippingAmount != null ? this.shippingAmount : BigDecimal.ZERO);
        total = total.subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
        return total.max(BigDecimal.ZERO); // 确保不为负数
    }
    
    /**
     * 更新订单状态
     * 同时更新相关的时间字段
     * 
     * @param newStatus 新状态
     * @param remark 备注信息
     */
    public void updateStatus(OrderStatus newStatus, String remark) {
        if (newStatus == null || !this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("无效的状态流转：" + this.status + " -> " + newStatus);
        }
        
        this.status = newStatus;
        LocalDateTime now = LocalDateTime.now();
        
        // 根据状态更新对应的时间字段
        switch (newStatus) {
            case PAID -> {
                this.paymentTime = now;
                this.paidAmount = this.totalAmount;
            }
            case SHIPPED -> this.shippingTime = now;
            case DELIVERED -> this.deliveryTime = now;
            case COMPLETED -> this.completionTime = now;
            case CANCELLED -> {
                this.cancellationTime = now;
                this.cancelReason = remark;
            }
        }
        
        // 更新商家备注
        if (remark != null && !remark.trim().isEmpty()) {
            this.merchantRemark = remark;
        }
    }
    
    /**
     * 设置物流信息
     * 
     * @param companyCode 快递公司编码
     * @param companyName 快递公司名称
     * @param trackingNumber 快递单号
     */
    public void setShippingInfo(String companyCode, String companyName, String trackingNumber) {
        this.shippingCompany = companyCode;
        this.shippingCompanyName = companyName;
        this.trackingNumber = trackingNumber;
        this.shippingStatus = 2; // 已发货
        
        // 更新订单状态为已发货
        if (this.status == OrderStatus.CONFIRMED) {
            updateStatus(OrderStatus.SHIPPED, "商家已发货");
        }
    }
    
    /**
     * 计算应退款金额
     * 
     * @return 应退款金额
     */
    public BigDecimal getRefundableAmount() {
        return this.paidAmount.subtract(this.refundedAmount);
    }
    
    /**
     * 获取订单商品数量
     * 
     * @return 订单中商品的总数量
     */
    public int getTotalQuantity() {
        return this.orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    /**
     * 获取订单商品种类数
     * 
     * @return 订单中不同商品的种类数
     */
    public int getProductTypeCount() {
        return this.orderItems.size();
    }
}