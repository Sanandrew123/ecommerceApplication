/*
文件级分析：
- 职责：定义订单状态枚举，规范订单在系统中的各种状态和流转
- 包结构考虑：位于enums包下，与其他业务枚举统一管理
- 命名原因：OrderStatus清晰表明这是订单状态枚举
- 调用关系：被Order实体类使用，在订单处理业务中进行状态判断和流转

设计思路：
1. 涵盖订单从创建到完成的完整生命周期
2. 支持订单取消、退款等异常流程
3. 提供状态流转的业务方法，确保状态变更的合法性
4. 考虑电商业务的复杂场景，如部分发货、部分退款等
*/
package com.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 订单状态枚举
 * 
 * 定义订单在系统中的各种状态，包括：
 * - PENDING_PAYMENT: 待支付状态，订单已创建等待支付
 * - PAID: 已支付状态，等待商家确认和发货
 * - CONFIRMED: 已确认状态，商家已确认订单
 * - SHIPPED: 已发货状态，商品已发出
 * - DELIVERED: 已送达状态，快递已送达
 * - COMPLETED: 已完成状态，用户确认收货
 * - CANCELLED: 已取消状态，订单被取消
 * - REFUNDING: 退款中状态，正在处理退款
 * - REFUNDED: 已退款状态，退款已完成
 * 
 * 状态流转规则：
 * PENDING_PAYMENT -> PAID (支付成功)
 * PENDING_PAYMENT -> CANCELLED (支付超时或主动取消)
 * PAID -> CONFIRMED (商家确认)
 * PAID -> REFUNDING (申请退款)
 * CONFIRMED -> SHIPPED (商家发货)
 * CONFIRMED -> REFUNDING (申请退款)
 * SHIPPED -> DELIVERED (物流送达)
 * DELIVERED -> COMPLETED (用户确认收货)
 * DELIVERED -> REFUNDING (申请退货退款)
 * REFUNDING -> REFUNDED (退款完成)
 * REFUNDING -> CONFIRMED (退款拒绝，恢复原状态)
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    
    /**
     * 待支付状态
     * 订单已创建，等待用户支付，有支付时限
     */
    PENDING_PAYMENT(1, "待支付"),
    
    /**
     * 已支付状态
     * 用户已完成支付，等待商家处理
     */
    PAID(2, "已支付"),
    
    /**
     * 已确认状态
     * 商家已确认订单，准备发货
     */
    CONFIRMED(3, "已确认"),
    
    /**
     * 已发货状态
     * 商品已发出，用户可跟踪物流
     */
    SHIPPED(4, "已发货"),
    
    /**
     * 已送达状态
     * 快递已送达，等待用户确认收货
     */
    DELIVERED(5, "已送达"),
    
    /**
     * 已完成状态
     * 用户已确认收货，订单完成，可以评价
     */
    COMPLETED(6, "已完成"),
    
    /**
     * 已取消状态
     * 订单已被取消，不会继续处理
     */
    CANCELLED(7, "已取消"),
    
    /**
     * 退款中状态
     * 正在处理退款申请
     */
    REFUNDING(8, "退款中"),
    
    /**
     * 已退款状态
     * 退款已完成，订单结束
     */
    REFUNDED(9, "已退款");
    
    /** 状态码 */
    private final Integer code;
    
    /** 状态描述 */
    private final String description;
    
    /**
     * 根据状态码获取订单状态枚举
     * 
     * @param code 状态码
     * @return 对应的订单状态枚举，如果找不到则返回null
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(OrderStatus.values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据状态描述获取订单状态枚举
     * 
     * @param description 状态描述
     * @return 对应的订单状态枚举，如果找不到则返回null
     */
    public static OrderStatus fromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(OrderStatus.values())
                .filter(status -> status.getDescription().equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
    
    // ======================== 状态分组定义 ========================
    
    /** 进行中的订单状态 */
    private static final Set<OrderStatus> ACTIVE_STATUSES = Set.of(
            PENDING_PAYMENT, PAID, CONFIRMED, SHIPPED, DELIVERED
    );
    
    /** 已结束的订单状态 */
    private static final Set<OrderStatus> FINISHED_STATUSES = Set.of(
            COMPLETED, CANCELLED, REFUNDED
    );
    
    /** 可以取消的订单状态 */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            PENDING_PAYMENT, PAID, CONFIRMED
    );
    
    /** 可以申请退款的订单状态 */
    private static final Set<OrderStatus> REFUNDABLE_STATUSES = Set.of(
            PAID, CONFIRMED, SHIPPED, DELIVERED
    );
    
    /** 需要用户操作的订单状态 */
    private static final Set<OrderStatus> USER_ACTION_REQUIRED = Set.of(
            PENDING_PAYMENT, DELIVERED
    );
    
    /** 需要商家操作的订单状态 */
    private static final Set<OrderStatus> MERCHANT_ACTION_REQUIRED = Set.of(
            PAID, CONFIRMED, REFUNDING
    );
    
    // ======================== 业务判断方法 ========================
    
    /**
     * 判断订单是否处于活跃状态
     * 活跃状态表示订单还在处理流程中
     * 
     * @return true表示活跃，false表示非活跃
     */
    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
    
    /**
     * 判断订单是否已结束
     * 结束状态表示订单流程已完结
     * 
     * @return true表示已结束，false表示未结束
     */
    public boolean isFinished() {
        return FINISHED_STATUSES.contains(this);
    }
    
    /**
     * 判断订单是否可以取消
     * 
     * @return true表示可以取消，false表示不能取消
     */
    public boolean isCancellable() {
        return CANCELLABLE_STATUSES.contains(this);
    }
    
    /**
     * 判断订单是否可以申请退款
     * 
     * @return true表示可以申请退款，false表示不能申请退款
     */
    public boolean isRefundable() {
        return REFUNDABLE_STATUSES.contains(this);
    }
    
    /**
     * 判断是否需要用户操作
     * 
     * @return true表示需要用户操作，false表示不需要
     */
    public boolean requiresUserAction() {
        return USER_ACTION_REQUIRED.contains(this);
    }
    
    /**
     * 判断是否需要商家操作
     * 
     * @return true表示需要商家操作，false表示不需要
     */
    public boolean requiresMerchantAction() {
        return MERCHANT_ACTION_REQUIRED.contains(this);
    }
    
    /**
     * 判断是否为待支付状态
     * 
     * @return true表示待支付，false表示非待支付
     */
    public boolean isPendingPayment() {
        return this == PENDING_PAYMENT;
    }
    
    /**
     * 判断是否已支付
     * 包括已支付、已确认、已发货、已送达、已完成等状态
     * 
     * @return true表示已支付，false表示未支付
     */
    public boolean isPaid() {
        return !Set.of(PENDING_PAYMENT, CANCELLED).contains(this);
    }
    
    /**
     * 判断是否为退款相关状态
     * 
     * @return true表示退款相关，false表示非退款相关
     */
    public boolean isRefundRelated() {
        return this == REFUNDING || this == REFUNDED;
    }
    
    /**
     * 判断是否可以评价
     * 只有已完成的订单才能评价
     * 
     * @return true表示可以评价，false表示不能评价
     */
    public boolean canReview() {
        return this == COMPLETED;
    }
    
    // ======================== 状态流转方法 ========================
    
    /**
     * 检查是否可以流转到目标状态
     * 
     * @param targetStatus 目标状态
     * @return true表示可以流转，false表示不能流转
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        
        return switch (this) {
            case PENDING_PAYMENT -> targetStatus == PAID || targetStatus == CANCELLED;
            case PAID -> targetStatus == CONFIRMED || targetStatus == REFUNDING;
            case CONFIRMED -> targetStatus == SHIPPED || targetStatus == REFUNDING;
            case SHIPPED -> targetStatus == DELIVERED;
            case DELIVERED -> targetStatus == COMPLETED || targetStatus == REFUNDING;
            case REFUNDING -> targetStatus == REFUNDED || targetStatus == CONFIRMED;
            case COMPLETED, CANCELLED, REFUNDED -> false; // 终态，不能再流转
        };
    }
    
    /**
     * 获取可以流转到的状态列表
     * 
     * @return 可以流转到的状态集合
     */
    public Set<OrderStatus> getNextPossibleStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(CONFIRMED, REFUNDING);
            case CONFIRMED -> Set.of(SHIPPED, REFUNDING);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED -> Set.of(COMPLETED, REFUNDING);
            case REFUNDING -> Set.of(REFUNDED, CONFIRMED);
            case COMPLETED, CANCELLED, REFUNDED -> Set.of(); // 终态
        };
    }
    
    /**
     * 获取状态的优先级
     * 用于排序和展示，数值越小优先级越高
     * 
     * @return 状态优先级
     */
    public int getPriority() {
        return switch (this) {
            case PENDING_PAYMENT -> 1;
            case PAID -> 2;
            case CONFIRMED -> 3;
            case SHIPPED -> 4;
            case DELIVERED -> 5;
            case REFUNDING -> 6;
            case COMPLETED -> 7;
            case REFUNDED -> 8;
            case CANCELLED -> 9;
        };
    }
    
    /**
     * 获取状态对应的CSS样式类
     * 用于前端展示不同状态的样式
     * 
     * @return CSS样式类名
     */
    public String getCssClass() {
        return switch (this) {
            case PENDING_PAYMENT -> "status-warning";
            case PAID, CONFIRMED -> "status-info";
            case SHIPPED, DELIVERED -> "status-primary";
            case COMPLETED -> "status-success";
            case CANCELLED -> "status-secondary";
            case REFUNDING, REFUNDED -> "status-danger";
        };
    }
}