package com.ecommerce.exception;

/*
 * 文件职责: 自定义业务异常类，用于处理业务逻辑中的可预期错误
 * 
 * 开发心理活动：
 * 1. 为什么需要自定义异常？
 *    - Java标准异常类不能很好地表达业务含义
 *    - 需要携带错误码和错误消息，便于前端处理
 *    - 区分业务异常和系统异常，采用不同的处理策略
 * 
 * 2. 为什么继承RuntimeException？
 *    - 运行时异常，不需要强制try-catch处理
 *    - 符合Spring事务回滚机制（只有RuntimeException才会回滚）
 *    - 简化代码，避免方法签名中的throws声明
 * 
 * 3. 异常设计原则：
 *    - 异常信息对用户友好，可直接展示给前端
 *    - 支持错误码，便于国际化和前端处理
 *    - 支持异常链，保留原始异常信息
 * 
 * 包结构设计思路:
 * - 放在exception包下，与其他异常处理类在一起
 * - 作为业务层的基础异常类
 * 
 * 命名原因:
 * - BusinessException明确表达这是业务异常
 * - 符合异常命名约定，以Exception结尾
 * 
 * 依赖关系:
 * - 继承RuntimeException，利用Java异常机制
 * - 被业务Service层使用，抛出业务错误
 * - 被GlobalExceptionHandler捕获处理
 */

import lombok.Getter;

/**
 * 自定义业务异常类
 * 
 * 使用场景：
 * 1. 业务规则验证失败 - 如：商品库存不足
 * 2. 业务状态不允许 - 如：订单已取消不能支付
 * 3. 业务数据不存在 - 如：用户不存在
 * 4. 业务权限不足 - 如：不能删除他人订单
 * 
 * 设计特点：
 * 1. 继承RuntimeException，无需强制处理
 * 2. 包含错误码和错误消息，便于统一处理
 * 3. 支持异常链，保留原始异常信息
 * 4. 提供多种构造方法，适应不同使用场景
 * 
 * 使用示例：
 * ```java
 * // 抛出业务异常
 * throw new BusinessException("用户不存在");
 * throw new BusinessException(404, "商品不存在");
 * 
 * // 在Service层使用
 * public User getUserById(Long id) {
 *     User user = userRepository.findById(id);
 *     if (user == null) {
 *         throw new BusinessException("用户不存在");
 *     }
 *     return user;
 * }
 * ```
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     * 用于前端进行特定处理或国际化
     */
    private final Integer code;

    /**
     * 默认构造方法
     * 使用通用错误码400
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 带错误码的构造方法
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 带原因异常的构造方法
     * 用于包装其他异常，保留异常链
     * 
     * @param message 错误消息
     * @param cause 原因异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    /**
     * 完整构造方法
     * 支持所有参数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原因异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ========== 常用业务异常静态工厂方法 ==========

    /**
     * 创建资源不存在异常
     * 
     * @param resourceName 资源名称
     * @return 业务异常
     */
    public static BusinessException notFound(String resourceName) {
        return new BusinessException(404, resourceName + "不存在");
    }

    /**
     * 创建资源已存在异常
     * 
     * @param resourceName 资源名称
     * @return 业务异常
     */
    public static BusinessException alreadyExists(String resourceName) {
        return new BusinessException(409, resourceName + "已存在");
    }

    /**
     * 创建权限不足异常
     * 
     * @param operation 操作名称
     * @return 业务异常
     */
    public static BusinessException accessDenied(String operation) {
        return new BusinessException(403, "没有权限进行" + operation + "操作");
    }

    /**
     * 创建业务状态错误异常
     * 
     * @param currentState 当前状态
     * @param operation 操作名称
     * @return 业务异常
     */
    public static BusinessException invalidState(String currentState, String operation) {
        return new BusinessException(400, 
            String.format("当前状态[%s]不允许进行[%s]操作", currentState, operation));
    }

    /**
     * 创建参数无效异常
     * 
     * @param paramName 参数名称
     * @param reason 无效原因
     * @return 业务异常
     */
    public static BusinessException invalidParam(String paramName, String reason) {
        return new BusinessException(400, 
            String.format("参数[%s]无效: %s", paramName, reason));
    }

    /**
     * 创建业务规则违反异常
     * 
     * @param rule 业务规则描述
     * @return 业务异常
     */
    public static BusinessException ruleViolation(String rule) {
        return new BusinessException(400, "违反业务规则: " + rule);
    }

    /**
     * 创建外部服务异常
     * 
     * @param serviceName 服务名称
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException externalServiceError(String serviceName, String message) {
        return new BusinessException(502, 
            String.format("外部服务[%s]异常: %s", serviceName, message));
    }

    /**
     * 创建库存不足异常
     * 
     * @param productName 商品名称
     * @param availableStock 可用库存
     * @param requestedQuantity 请求数量
     * @return 业务异常
     */
    public static BusinessException insufficientStock(String productName, int availableStock, int requestedQuantity) {
        return new BusinessException(400, 
            String.format("商品[%s]库存不足，可用库存: %d，请求数量: %d", 
                productName, availableStock, requestedQuantity));
    }

    /**
     * 创建账户余额不足异常
     * 
     * @param availableBalance 可用余额
     * @param requiredAmount 所需金额
     * @return 业务异常
     */
    public static BusinessException insufficientBalance(String availableBalance, String requiredAmount) {
        return new BusinessException(400, 
            String.format("账户余额不足，可用余额: %s，所需金额: %s", 
                availableBalance, requiredAmount));
    }

    @Override
    public String toString() {
        return String.format("BusinessException{code=%d, message='%s'}", code, getMessage());
    }
}