/*
文件级分析：
- 职责：支付记录实体类，映射数据库中的支付记录表，记录每笔支付的详细信息
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：Payment表示支付记录，是电商系统中的重要交易记录实体
- 调用关系：多对一关联Order订单，多对一关联User用户，一对多关联PaymentLog支付日志

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 记录支付的完整信息：订单、用户、金额、方式、状态等
3. 支持第三方支付平台的交易号和回调处理
4. 包含手续费计算和退款相关字段
5. 预留扩展字段支持复杂的支付场景
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 * 
 * 记录系统中每笔支付的详细信息，包括：
 * 1. 基本信息：支付单号、关联订单、支付用户等
 * 2. 金额信息：支付金额、手续费、实际支付金额等
 * 3. 支付信息：支付方式、第三方交易号、支付时间等
 * 4. 状态信息：支付状态、处理结果、错误信息等
 * 5. 扩展信息：支付渠道参数、回调数据等
 * 
 * 业务特点：
 * - 支付记录一旦创建，核心信息不可修改
 * - 状态流转严格按照支付流程进行
 * - 支持第三方支付平台的异步回调处理
 * - 记录完整的支付链路信息，便于对账和查询
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"user", "order"})
@ToString(callSuper = true, exclude = {"user", "order"})
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_no", columnList = "payment_no", unique = true),
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_payment_method", columnList = "payment_method"),
        @Index(name = "idx_third_party_no", columnList = "third_party_transaction_no"),
        @Index(name = "idx_payment_time", columnList = "payment_time DESC"),
        @Index(name = "idx_amount", columnList = "payment_amount DESC")
})
public class Payment extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 支付单号
     * 全局唯一的支付标识符，对用户和商家可见
     * 格式建议：PAY + 年月日 + 随机数，如：PAY20240101001234567
     */
    @NotBlank(message = "支付单号不能为空")
    @Size(max = 50, message = "支付单号长度不能超过50字符")
    @Column(name = "payment_no", nullable = false, unique = true, length = 50)
    private String paymentNo;
    
    /**
     * 关联订单
     * 多对一关联，每笔支付属于一个订单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_order"))
    @NotNull(message = "关联订单不能为空")
    @JsonIgnore
    private Order order;
    
    /**
     * 关联用户
     * 多对一关联，每笔支付属于一个用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_user"))
    @NotNull(message = "支付用户不能为空")
    @JsonIgnore
    private User user;
    
    /**
     * 支付状态
     * 使用枚举映射，控制支付的生命周期
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    /**
     * 支付方式
     * 使用枚举映射，标识具体的支付渠道
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    // ======================== 金额信息字段 ========================
    
    /**
     * 支付金额
     * 本次支付的金额（不含手续费）
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "支付金额格式不正确")
    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;
    
    /**
     * 手续费金额
     * 支付渠道收取的手续费
     */
    @DecimalMin(value = "0.00", message = "手续费金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "手续费金额格式不正确")
    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;
    
    /**
     * 实际支付金额
     * 用户实际支付的金额（支付金额 + 手续费）
     */
    @NotNull(message = "实际支付金额不能为空")
    @DecimalMin(value = "0.01", message = "实际支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "实际支付金额格式不正确")
    @Column(name = "actual_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualAmount;
    
    /**
     * 已退款金额
     * 已退还给用户的金额
     */
    @DecimalMin(value = "0.00", message = "已退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "已退款金额格式不正确")
    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    // ======================== 第三方支付信息字段 ========================
    
    /**
     * 第三方交易号
     * 支付平台返回的交易流水号，用于对账和查询
     */
    @Size(max = 100, message = "第三方交易号长度不能超过100字符")
    @Column(name = "third_party_transaction_no", length = 100)
    private String thirdPartyTransactionNo;
    
    /**
     * 第三方支付渠道
     * 具体的支付渠道标识，如alipay_pc、wechat_h5等
     */
    @Size(max = 50, message = "第三方支付渠道长度不能超过50字符")
    @Column(name = "third_party_channel", length = 50)
    private String thirdPartyChannel;
    
    /**
     * 第三方支付状态
     * 第三方平台返回的支付状态
     */
    @Size(max = 20, message = "第三方支付状态长度不能超过20字符")
    @Column(name = "third_party_status", length = 20)
    private String thirdPartyStatus;
    
    /**
     * 支付链接或二维码
     * 第三方支付平台返回的支付链接或二维码内容
     */
    @Size(max = 500, message = "支付链接长度不能超过500字符")
    @Column(name = "payment_url", length = 500)
    private String paymentUrl;
    
    // ======================== 时间信息字段 ========================
    
    /**
     * 支付时间
     * 支付成功的时间
     */
    @Column(name = "payment_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;
    
    /**
     * 支付超时时间
     * 支付的截止时间，超时后自动关闭
     */
    @Column(name = "expire_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    /**
     * 通知时间
     * 第三方支付平台回调通知的时间
     */
    @Column(name = "notify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notifyTime;
    
    // ======================== 处理结果字段 ========================
    
    /**
     * 处理结果码
     * 支付处理的结果代码
     */
    @Size(max = 20, message = "处理结果码长度不能超过20字符")
    @Column(name = "result_code", length = 20)
    private String resultCode;
    
    /**
     * 处理结果描述
     * 支付处理结果的详细描述
     */
    @Size(max = 200, message = "处理结果描述长度不能超过200字符")
    @Column(name = "result_message", length = 200)
    private String resultMessage;
    
    /**
     * 错误代码
     * 支付失败时的错误代码
     */
    @Size(max = 50, message = "错误代码长度不能超过50字符")
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    /**
     * 错误描述
     * 支付失败时的错误描述
     */
    @Size(max = 200, message = "错误描述长度不能超过200字符")
    @Column(name = "error_message", length = 200)
    private String errorMessage;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 支付请求参数
     * JSON格式存储发送给第三方平台的请求参数
     */
    @Column(name = "request_params", columnDefinition = "JSON")
    private String requestParams;
    
    /**
     * 支付响应数据
     * JSON格式存储第三方平台返回的响应数据
     */
    @Column(name = "response_data", columnDefinition = "JSON")
    private String responseData;
    
    /**
     * 回调通知数据
     * JSON格式存储第三方平台的回调通知数据
     */
    @Column(name = "notify_data", columnDefinition = "JSON")
    private String notifyData;
    
    /**
     * 客户端IP地址
     * 发起支付的客户端IP地址
     */
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", 
             message = "IP地址格式不正确")
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    /**
     * 用户代理信息
     * 客户端的User-Agent信息
     */
    @Size(max = 500, message = "用户代理信息长度不能超过500字符")
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 备注信息
     * 支付相关的备注说明
     */
    @Size(max = 500, message = "备注信息长度不能超过500字符")
    @Column(name = "remark", length = 500)
    private String remark;
    
    /**
     * 扩展属性
     * JSON格式存储支付的额外属性
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    // ======================== 业务方法 ========================
    
    /**
     * 计算实际支付金额
     * 支付金额 + 手续费
     * 
     * @return 计算后的实际支付金额
     */
    public BigDecimal calculateActualAmount() {
        BigDecimal payment = this.paymentAmount != null ? this.paymentAmount : BigDecimal.ZERO;
        BigDecimal fee = this.feeAmount != null ? this.feeAmount : BigDecimal.ZERO;
        return payment.add(fee);
    }
    
    /**
     * 计算可退款金额
     * 
     * @return 可退款的金额
     */
    public BigDecimal getRefundableAmount() {
        return this.paymentAmount.subtract(this.refundedAmount);
    }
    
    /**
     * 判断支付是否成功
     * 
     * @return true表示支付成功，false表示支付未成功
     */
    public boolean isPaymentSuccess() {
        return this.status != null && this.status.isSuccess();
    }
    
    /**
     * 判断支付是否失败
     * 
     * @return true表示支付失败，false表示支付未失败
     */
    public boolean isPaymentFailed() {
        return this.status != null && this.status.isFailed();
    }
    
    /**
     * 判断支付是否可以重试
     * 
     * @return true表示可以重试，false表示不能重试
     */
    public boolean canRetry() {
        return this.status != null && this.status.canRetry();
    }
    
    /**
     * 判断支付是否可以退款
     * 
     * @return true表示可以退款，false表示不能退款
     */
    public boolean canRefund() {
        return this.status != null && this.status.isRefundable() && 
               this.refundedAmount.compareTo(this.paymentAmount) < 0;
    }
    
    /**
     * 判断支付是否已超时
     * 
     * @return true表示已超时，false表示未超时
     */
    public boolean isExpired() {
        return this.expireTime != null && LocalDateTime.now().isAfter(this.expireTime);
    }
    
    /**
     * 判断是否需要手续费
     * 
     * @return true表示需要手续费，false表示不需要手续费
     */
    public boolean requiresFee() {
        return this.paymentMethod != null && this.paymentMethod.requiresFee();
    }
    
    /**
     * 更新支付状态
     * 同时更新相关的时间字段和结果信息
     * 
     * @param newStatus 新状态
     * @param resultCode 结果代码
     * @param resultMessage 结果消息
     */
    public void updateStatus(PaymentStatus newStatus, String resultCode, String resultMessage) {
        if (newStatus == null || !this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("无效的状态流转：" + this.status + " -> " + newStatus);
        }
        
        this.status = newStatus;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        
        LocalDateTime now = LocalDateTime.now();
        
        // 根据状态更新对应的时间字段
        if (newStatus == PaymentStatus.SUCCESS) {
            this.paymentTime = now;
        }
    }
    
    /**
     * 设置支付成功
     * 
     * @param thirdPartyTransactionNo 第三方交易号
     * @param thirdPartyStatus 第三方状态
     */
    public void markAsSuccess(String thirdPartyTransactionNo, String thirdPartyStatus) {
        updateStatus(PaymentStatus.SUCCESS, "SUCCESS", "支付成功");
        this.thirdPartyTransactionNo = thirdPartyTransactionNo;
        this.thirdPartyStatus = thirdPartyStatus;
        this.paymentTime = LocalDateTime.now();
    }
    
    /**
     * 设置支付失败
     * 
     * @param errorCode 错误代码
     * @param errorMessage 错误描述
     */
    public void markAsFailed(String errorCode, String errorMessage) {
        updateStatus(PaymentStatus.FAILED, "FAILED", "支付失败");
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 处理退款
     * 
     * @param refundAmount 退款金额
     * @param isPartial 是否部分退款
     */
    public void processRefund(BigDecimal refundAmount, boolean isPartial) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }
        
        BigDecimal newRefundedAmount = this.refundedAmount.add(refundAmount);
        if (newRefundedAmount.compareTo(this.paymentAmount) > 0) {
            throw new IllegalArgumentException("退款金额超过支付金额");
        }
        
        this.refundedAmount = newRefundedAmount;
        
        if (isPartial) {
            updateStatus(PaymentStatus.PARTIAL_REFUNDED, "PARTIAL_REFUNDED", "部分退款完成");
        } else {
            updateStatus(PaymentStatus.REFUNDED, "REFUNDED", "退款完成");
        }
    }
    
    /**
     * 获取支付方式名称
     * 
     * @return 支付方式的中文名称
     */
    public String getPaymentMethodName() {
        return this.paymentMethod != null ? this.paymentMethod.getName() : "";
    }
    
    /**
     * 获取支付状态描述
     * 
     * @return 支付状态的中文描述
     */
    public String getStatusDescription() {
        return this.status != null ? this.status.getDescription() : "";
    }
}