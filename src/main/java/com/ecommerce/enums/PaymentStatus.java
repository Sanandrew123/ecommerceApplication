/*
文件级分析：
- 职责：定义支付状态枚举，规范支付记录在系统中的各种状态和流转
- 包结构考虑：位于enums包下，与其他业务枚举统一管理
- 命名原因：PaymentStatus清晰表明这是支付状态枚举
- 调用关系：被Payment实体类使用，在支付处理业务中进行状态判断和流转

设计思路：
1. 涵盖支付从发起到完成的完整生命周期
2. 支持支付失败、超时、撤销等异常流程
3. 提供状态流转的业务方法，确保状态变更的合法性
4. 考虑第三方支付平台的回调处理场景
*/
package com.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 支付状态枚举
 * 
 * 定义支付记录在系统中的各种状态，包括：
 * - PENDING: 待支付状态，支付单已创建等待用户支付
 * - PROCESSING: 支付处理中，正在与第三方支付平台交互
 * - SUCCESS: 支付成功，资金已到账
 * - FAILED: 支付失败，支付未成功
 * - CANCELLED: 支付取消，用户主动取消或系统取消
 * - TIMEOUT: 支付超时，超过有效期未完成支付
 * - REFUNDING: 退款中，正在处理退款
 * - REFUNDED: 已退款，退款已完成
 * - PARTIAL_REFUNDED: 部分退款，订单存在部分退款
 * 
 * 状态流转规则：
 * PENDING -> PROCESSING (发起支付)
 * PENDING -> CANCELLED (取消支付)
 * PENDING -> TIMEOUT (支付超时)
 * PROCESSING -> SUCCESS (支付成功)
 * PROCESSING -> FAILED (支付失败)
 * PROCESSING -> CANCELLED (支付取消)
 * SUCCESS -> REFUNDING (申请退款)
 * REFUNDING -> REFUNDED (退款完成)
 * REFUNDING -> PARTIAL_REFUNDED (部分退款完成)
 * REFUNDING -> SUCCESS (退款失败，恢复原状态)
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {
    
    /**
     * 待支付状态
     * 支付单已创建，等待用户支付，有支付时限
     */
    PENDING(1, "待支付"),
    
    /**
     * 支付处理中状态
     * 正在与第三方支付平台进行交互处理
     */
    PROCESSING(2, "支付处理中"),
    
    /**
     * 支付成功状态
     * 支付已完成，资金已到账
     */
    SUCCESS(3, "支付成功"),
    
    /**
     * 支付失败状态
     * 支付未成功，需要重新支付或选择其他支付方式
     */
    FAILED(4, "支付失败"),
    
    /**
     * 支付取消状态
     * 用户主动取消或系统自动取消
     */
    CANCELLED(5, "支付取消"),
    
    /**
     * 支付超时状态
     * 超过支付有效期，支付单自动失效
     */
    TIMEOUT(6, "支付超时"),
    
    /**
     * 退款中状态
     * 正在处理退款申请
     */
    REFUNDING(7, "退款中"),
    
    /**
     * 已退款状态
     * 退款已完成，资金已返还
     */
    REFUNDED(8, "已退款"),
    
    /**
     * 部分退款状态
     * 订单存在部分退款，但仍有未退款部分
     */
    PARTIAL_REFUNDED(9, "部分退款");
    
    /** 状态码 */
    private final Integer code;
    
    /** 状态描述 */
    private final String description;
    
    /**
     * 根据状态码获取支付状态枚举
     * 
     * @param code 状态码
     * @return 对应的支付状态枚举，如果找不到则返回null
     */
    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(PaymentStatus.values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据状态描述获取支付状态枚举
     * 
     * @param description 状态描述
     * @return 对应的支付状态枚举，如果找不到则返回null
     */
    public static PaymentStatus fromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(PaymentStatus.values())
                .filter(status -> status.getDescription().equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
    
    // ======================== 状态分组定义 ========================
    
    /** 进行中的支付状态 */
    private static final Set<PaymentStatus> ACTIVE_STATUSES = Set.of(
            PENDING, PROCESSING, REFUNDING
    );
    
    /** 已完成的支付状态（最终状态） */
    private static final Set<PaymentStatus> FINISHED_STATUSES = Set.of(
            SUCCESS, FAILED, CANCELLED, TIMEOUT, REFUNDED, PARTIAL_REFUNDED
    );
    
    /** 支付成功相关状态 */
    private static final Set<PaymentStatus> SUCCESS_STATUSES = Set.of(
            SUCCESS, PARTIAL_REFUNDED
    );
    
    /** 支付失败相关状态 */
    private static final Set<PaymentStatus> FAILED_STATUSES = Set.of(
            FAILED, CANCELLED, TIMEOUT
    );
    
    /** 可以退款的支付状态 */
    private static final Set<PaymentStatus> REFUNDABLE_STATUSES = Set.of(
            SUCCESS, PARTIAL_REFUNDED
    );
    
    /** 退款相关状态 */
    private static final Set<PaymentStatus> REFUND_STATUSES = Set.of(
            REFUNDING, REFUNDED, PARTIAL_REFUNDED
    );
    
    // ======================== 业务判断方法 ========================
    
    /**
     * 判断支付是否处于活跃状态
     * 活跃状态表示支付还在处理流程中
     * 
     * @return true表示活跃，false表示非活跃
     */
    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
    
    /**
     * 判断支付是否已完成
     * 完成状态表示支付流程已结束
     * 
     * @return true表示已完成，false表示未完成
     */
    public boolean isFinished() {
        return FINISHED_STATUSES.contains(this);
    }
    
    /**
     * 判断支付是否成功
     * 
     * @return true表示支付成功，false表示支付未成功
     */
    public boolean isSuccess() {
        return SUCCESS_STATUSES.contains(this);
    }
    
    /**
     * 判断支付是否失败
     * 
     * @return true表示支付失败，false表示支付未失败
     */
    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }
    
    /**
     * 判断支付是否可以退款
     * 
     * @return true表示可以退款，false表示不能退款
     */
    public boolean isRefundable() {
        return REFUNDABLE_STATUSES.contains(this);
    }
    
    /**
     * 判断是否为退款相关状态
     * 
     * @return true表示退款相关，false表示非退款相关
     */
    public boolean isRefundRelated() {
        return REFUND_STATUSES.contains(this);
    }
    
    /**
     * 判断是否为待处理状态
     * 需要系统或用户进行后续操作的状态
     * 
     * @return true表示待处理，false表示不需要处理
     */
    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }
    
    /**
     * 判断支付是否可以重新支付
     * 
     * @return true表示可以重新支付，false表示不能重新支付
     */
    public boolean canRetry() {
        return this == FAILED || this == TIMEOUT || this == CANCELLED;
    }
    
    // ======================== 状态流转方法 ========================
    
    /**
     * 检查是否可以流转到目标状态
     * 
     * @param targetStatus 目标状态
     * @return true表示可以流转，false表示不能流转
     */
    public boolean canTransitionTo(PaymentStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        
        return switch (this) {
            case PENDING -> Set.of(PROCESSING, CANCELLED, TIMEOUT).contains(targetStatus);
            case PROCESSING -> Set.of(SUCCESS, FAILED, CANCELLED).contains(targetStatus);
            case SUCCESS -> Set.of(REFUNDING).contains(targetStatus);
            case REFUNDING -> Set.of(REFUNDED, PARTIAL_REFUNDED, SUCCESS).contains(targetStatus);
            case PARTIAL_REFUNDED -> Set.of(REFUNDING).contains(targetStatus);
            case FAILED, CANCELLED, TIMEOUT, REFUNDED -> false; // 终态，不能再流转
        };
    }
    
    /**
     * 获取可以流转到的状态列表
     * 
     * @return 可以流转到的状态集合
     */
    public Set<PaymentStatus> getNextPossibleStatuses() {
        return switch (this) {
            case PENDING -> Set.of(PROCESSING, CANCELLED, TIMEOUT);
            case PROCESSING -> Set.of(SUCCESS, FAILED, CANCELLED);
            case SUCCESS -> Set.of(REFUNDING);
            case REFUNDING -> Set.of(REFUNDED, PARTIAL_REFUNDED, SUCCESS);
            case PARTIAL_REFUNDED -> Set.of(REFUNDING);
            case FAILED, CANCELLED, TIMEOUT, REFUNDED -> Set.of(); // 终态
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
            case PENDING -> 1;
            case PROCESSING -> 2;
            case FAILED -> 3;
            case REFUNDING -> 4;
            case SUCCESS -> 5;
            case PARTIAL_REFUNDED -> 6;
            case REFUNDED -> 7;
            case CANCELLED -> 8;
            case TIMEOUT -> 9;
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
            case PENDING -> "status-warning";
            case PROCESSING -> "status-info";
            case SUCCESS -> "status-success";
            case FAILED, TIMEOUT -> "status-danger";
            case CANCELLED -> "status-secondary";
            case REFUNDING -> "status-warning";
            case REFUNDED, PARTIAL_REFUNDED -> "status-info";
        };
    }
}