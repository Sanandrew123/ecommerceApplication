/*
文件级分析：
- 职责：定义业务异常类，封装业务逻辑中的异常情况
- 包结构考虑：位于exception包下，统一管理所有异常类
- 命名原因：BusinessException明确表示这是业务层面的异常
- 调用关系：在Service层抛出，被GlobalExceptionHandler捕获处理

设计思路：
1. 继承RuntimeException，属于非检查异常，简化调用方代码
2. 包含错误码和错误消息，便于前端进行不同的处理
3. 提供多种构造方法，适应不同的使用场景
4. 支持异常链，保留原始异常信息便于排查问题
*/
package com.ecommerce.exception;

import com.ecommerce.constants.CommonConstants;
import lombok.Getter;

/**
 * 业务异常类
 * 
 * 用于封装业务逻辑中的异常情况，如：
 * - 用户不存在
 * - 商品库存不足
 * - 订单状态不正确
 * - 权限不足等
 * 
 * 相比于系统异常，业务异常通常是可预期的，可以给用户友好的提示信息。
 * 
 * 使用示例：
 * throw new BusinessException("用户不存在");
 * throw new BusinessException(404, "商品未找到");
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private final Integer code;
    
    /** 错误消息 */
    private final String message;
    
    /**
     * 构造方法 - 仅包含错误消息
     * 使用默认的业务错误码
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = CommonConstants.BUSINESS_ERROR_CODE;
        this.message = message;
    }
    
    /**
     * 构造方法 - 包含错误码和错误消息
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 构造方法 - 包含错误消息和原始异常
     * 保留异常链，便于问题排查
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = CommonConstants.BUSINESS_ERROR_CODE;
        this.message = message;
    }
    
    /**
     * 构造方法 - 包含完整参数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    // ======================== 常用业务异常静态工厂方法 ========================
    
    /**
     * 参数无效异常
     * 
     * @param message 具体的参数错误信息
     * @return BusinessException实例
     */
    public static BusinessException invalidParameter(String message) {
        return new BusinessException(message);
    }
    
    /**
     * 资源不存在异常
     * 
     * @param resource 资源名称
     * @return BusinessException实例
     */
    public static BusinessException resourceNotFound(String resource) {
        return new BusinessException(CommonConstants.NOT_FOUND_CODE, resource + "不存在");
    }
    
    /**
     * 权限不足异常
     * 
     * @return BusinessException实例
     */
    public static BusinessException accessDenied() {
        return new BusinessException(CommonConstants.FORBIDDEN_CODE, "权限不足");
    }
    
    /**
     * 操作失败异常
     * 
     * @param operation 操作名称
     * @return BusinessException实例
     */
    public static BusinessException operationFailed(String operation) {
        return new BusinessException(operation + "失败");
    }
    
    /**
     * 状态不正确异常
     * 
     * @param currentStatus 当前状态
     * @param operation 尝试执行的操作
     * @return BusinessException实例
     */
    public static BusinessException invalidStatus(String currentStatus, String operation) {
        return new BusinessException(String.format("当前状态为%s，无法执行%s操作", currentStatus, operation));
    }
    
    /**
     * 重复操作异常
     * 
     * @param operation 操作名称
     * @return BusinessException实例
     */
    public static BusinessException duplicateOperation(String operation) {
        return new BusinessException("重复" + operation);
    }
    
    /**
     * 数据冲突异常
     * 
     * @param message 冲突描述
     * @return BusinessException实例
     */
    public static BusinessException dataConflict(String message) {
        return new BusinessException(CommonConstants.BUSINESS_ERROR_CODE, message);
    }
    
    /**
     * 请求参数错误异常
     * 
     * @param message 错误描述
     * @return BusinessException实例
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
    
    /**
     * 未授权异常
     * 
     * @param message 错误描述
     * @return BusinessException实例
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }
    
    /**
     * 禁止访问异常
     * 
     * @param message 错误描述
     * @return BusinessException实例
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
    
    /**
     * 资源未找到异常
     * 
     * @param message 错误描述
     * @return BusinessException实例
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
    
    /**
     * 冲突异常
     * 
     * @param message 错误描述
     * @return BusinessException实例
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }
}