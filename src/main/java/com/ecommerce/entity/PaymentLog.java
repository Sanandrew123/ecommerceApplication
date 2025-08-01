/*
文件级分析：
- 职责：支付日志实体类，记录支付过程中的所有操作和状态变更
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：PaymentLog表示支付日志，用于追踪支付的完整链路
- 调用关系：多对一关联Payment支付记录，用于审计和问题排查

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 记录支付的每个操作节点和状态变化
3. 存储详细的请求响应数据，便于问题排查
4. 支持日志的分类和级别管理
5. 为性能监控和业务分析提供数据基础
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.ecommerce.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 支付日志实体类
 * 
 * 记录支付过程中的所有操作和状态变更，包括：
 * 1. 基本信息：关联支付记录、操作类型、日志级别等
 * 2. 状态信息：操作前后状态、处理结果等
 * 3. 详细数据：请求参数、响应数据、错误信息等
 * 4. 执行信息：执行时间、耗时、操作者等
 * 5. 扩展信息：业务标签、追踪ID等
 * 
 * 业务特点：
 * - 日志记录只增不改，确保审计完整性
 * - 支持按时间、状态、操作类型等维度查询
 * - 提供性能监控和问题排查的数据支持
 * - 记录完整的支付链路信息，便于业务分析
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"payment"})
@ToString(callSuper = true, exclude = {"payment"})
@Entity
@Table(name = "payment_logs", indexes = {
        @Index(name = "idx_payment_id", columnList = "payment_id"),
        @Index(name = "idx_operation_type", columnList = "operation_type"),
        @Index(name = "idx_log_level", columnList = "log_level"),
        @Index(name = "idx_result_status", columnList = "result_status"),
        @Index(name = "idx_execute_time", columnList = "execute_time DESC"),
        @Index(name = "idx_payment_operation", columnList = "payment_id, operation_type"),
        @Index(name = "idx_trace_id", columnList = "trace_id")
})
public class PaymentLog extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 关联支付记录
     * 多对一关联，每条日志属于一个支付记录
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_log_payment"))
    @NotNull(message = "关联支付记录不能为空")
    @JsonIgnore
    private Payment payment;
    
    /**
     * 操作类型
     * 记录具体的操作类型，如：CREATE、PAY、NOTIFY、REFUND等
     */
    @NotBlank(message = "操作类型不能为空")
    @Size(max = 50, message = "操作类型长度不能超过50字符")
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;
    
    /**
     * 日志级别
     * INFO-信息，WARN-警告，ERROR-错误，DEBUG-调试
     */
    @NotBlank(message = "日志级别不能为空")
    @Size(max = 10, message = "日志级别长度不能超过10字符")
    @Column(name = "log_level", nullable = false, length = 10)
    private String logLevel = "INFO";
    
    /**
     * 操作标题
     * 操作的简短描述，便于快速了解操作内容
     */
    @NotBlank(message = "操作标题不能为空")
    @Size(max = 200, message = "操作标题长度不能超过200字符")
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    /**
     * 操作描述
     * 操作的详细描述信息
     */
    @Size(max = 1000, message = "操作描述长度不能超过1000字符")
    @Column(name = "description", length = 1000)
    private String description;
    
    // ======================== 状态信息字段 ========================
    
    /**
     * 操作前状态
     * 执行操作前的支付状态
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "before_status")
    private PaymentStatus beforeStatus;
    
    /**
     * 操作后状态
     * 执行操作后的支付状态
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "after_status")
    private PaymentStatus afterStatus;
    
    /**
     * 处理结果状态
     * SUCCESS-成功，FAILED-失败，PROCESSING-处理中
     */
    @NotBlank(message = "处理结果状态不能为空")
    @Size(max = 20, message = "处理结果状态长度不能超过20字符")
    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus;
    
    /**
     * 结果代码
     * 操作的结果代码
     */
    @Size(max = 50, message = "结果代码长度不能超过50字符")
    @Column(name = "result_code", length = 50)
    private String resultCode;
    
    /**
     * 结果消息
     * 操作结果的详细消息
     */
    @Size(max = 500, message = "结果消息长度不能超过500字符")
    @Column(name = "result_message", length = 500)
    private String resultMessage;
    
    // ======================== 详细数据字段 ========================
    
    /**
     * 请求参数
     * JSON格式存储操作的请求参数
     */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    /**
     * 响应数据
     * JSON格式存储操作的响应数据
     */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    /**
     * 错误信息
     * 操作失败时的详细错误信息
     */
    @Column(name = "error_info", columnDefinition = "TEXT")
    private String errorInfo;
    
    /**
     * 异常堆栈
     * 发生异常时的完整堆栈信息
     */
    @Column(name = "exception_stack", columnDefinition = "TEXT")
    private String exceptionStack;
    
    // ======================== 执行信息字段 ========================
    
    /**
     * 执行时间
     * 操作开始执行的时间
     */
    @Column(name = "execute_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executeTime = LocalDateTime.now();
    
    /**
     * 完成时间
     * 操作完成的时间
     */
    @Column(name = "complete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;
    
    /**
     * 执行耗时
     * 操作执行的耗时（毫秒）
     */
    @Min(value = 0, message = "执行耗时不能为负数")
    @Column(name = "duration")
    private Long duration;
    
    /**
     * 操作者ID
     * 执行操作的用户ID，系统操作时为空
     */
    @Column(name = "operator_id")
    private Long operatorId;
    
    /**
     * 操作者名称
     * 执行操作的用户名称或系统标识
     */
    @Size(max = 100, message = "操作者名称长度不能超过100字符")
    @Column(name = "operator_name", length = 100)
    private String operatorName;
    
    // ======================== 技术信息字段 ========================
    
    /**
     * 追踪ID
     * 用于追踪完整的支付链路
     */
    @Size(max = 100, message = "追踪ID长度不能超过100字符")
    @Column(name = "trace_id", length = 100)
    private String traceId;
    
    /**
     * 会话ID
     * 用户会话标识
     */
    @Size(max = 100, message = "会话ID长度不能超过100字符")
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * 客户端IP地址
     * 发起操作的客户端IP地址
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
     * 服务器信息
     * 处理请求的服务器标识
     */
    @Size(max = 100, message = "服务器信息长度不能超过100字符")
    @Column(name = "server_info", length = 100)
    private String serverInfo;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 业务标签
     * 用于业务分类和统计的标签
     */
    @Size(max = 200, message = "业务标签长度不能超过200字符")
    @Column(name = "business_tags", length = 200)
    private String businessTags;
    
    /**
     * 扩展属性
     * JSON格式存储日志的额外属性
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    // ======================== 构造方法 ========================
    
    /**
     * 默认构造方法
     */
    public PaymentLog() {
        this.executeTime = LocalDateTime.now();
    }
    
    /**
     * 便捷构造方法
     * 
     * @param payment 支付记录
     * @param operationType 操作类型
     * @param title 操作标题
     */
    public PaymentLog(Payment payment, String operationType, String title) {
        this();
        this.payment = payment;
        this.operationType = operationType;
        this.title = title;
    }
    
    // ======================== 业务方法 ========================
    
    /**
     * 设置操作成功
     * 
     * @param resultCode 结果代码
     * @param resultMessage 结果消息
     */
    public void markAsSuccess(String resultCode, String resultMessage) {
        this.resultStatus = "SUCCESS";
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.completeTime = LocalDateTime.now();
        this.calculateDuration();
    }
    
    /**
     * 设置操作失败
     * 
     * @param resultCode 结果代码
     * @param resultMessage 结果消息
     * @param errorInfo 错误信息
     */
    public void markAsFailed(String resultCode, String resultMessage, String errorInfo) {
        this.resultStatus = "FAILED";
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.errorInfo = errorInfo;
        this.completeTime = LocalDateTime.now();
        this.calculateDuration();
    }
    
    /**
     * 设置操作处理中
     * 
     * @param resultMessage 处理消息
     */
    public void markAsProcessing(String resultMessage) {
        this.resultStatus = "PROCESSING";
        this.resultMessage = resultMessage;
    }
    
    /**
     * 计算执行耗时
     */
    private void calculateDuration() {
        if (this.executeTime != null && this.completeTime != null) {
            this.duration = java.time.Duration.between(this.executeTime, this.completeTime).toMillis();
        }
    }
    
    /**
     * 设置状态变更
     * 
     * @param beforeStatus 操作前状态
     * @param afterStatus 操作后状态
     */
    public void setStatusChange(PaymentStatus beforeStatus, PaymentStatus afterStatus) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
    }
    
    /**
     * 添加异常信息
     * 
     * @param exception 异常对象
     */
    public void addException(Exception exception) {
        if (exception != null) {
            this.errorInfo = exception.getMessage();
            this.exceptionStack = getStackTrace(exception);
        }
    }
    
    /**
     * 获取异常堆栈字符串
     * 
     * @param exception 异常对象
     * @return 堆栈字符串
     */
    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 判断操作是否成功
     * 
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.resultStatus);
    }
    
    /**
     * 判断操作是否失败
     * 
     * @return true表示失败，false表示成功
     */
    public boolean isFailed() {
        return "FAILED".equals(this.resultStatus);
    }
    
    /**
     * 判断操作是否处理中
     * 
     * @return true表示处理中，false表示已完成
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(this.resultStatus);
    }
    
    /**
     * 获取格式化的耗时信息
     * 
     * @return 格式化的耗时字符串
     */
    public String getFormattedDuration() {
        if (this.duration == null) {
            return "未知";
        }
        
        if (this.duration < 1000) {
            return this.duration + "ms";
        } else if (this.duration < 60000) {
            return String.format("%.2fs", this.duration / 1000.0);
        } else {
            long minutes = this.duration / 60000;
            long seconds = (this.duration % 60000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }
    
    /**
     * 获取日志级别的优先级
     * 用于日志过滤和展示
     * 
     * @return 优先级数值，越小优先级越高
     */
    public int getLogLevelPriority() {
        return switch (this.logLevel) {
            case "ERROR" -> 1;
            case "WARN" -> 2;
            case "INFO" -> 3;
            case "DEBUG" -> 4;
            default -> 5;
        };
    }
}