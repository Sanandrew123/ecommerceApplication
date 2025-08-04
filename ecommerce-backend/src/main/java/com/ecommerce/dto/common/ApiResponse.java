package com.ecommerce.dto.common;

/*
 * 文件职责: 统一API响应格式包装类，规范化所有接口的返回数据结构
 * 
 * 开发心理活动：
 * 1. 为什么需要统一响应格式？
 *    - 前端处理统一，减少解析复杂度
 *    - 便于全局错误处理和状态码管理
 *    - 提升接口规范性和可维护性
 * 
 * 2. 为什么选择泛型设计？
 *    - 支持任意类型的数据返回，增强代码复用性
 *    - 编译时类型检查，避免运行时类型错误
 *    - 配合IDE智能提示，提高开发效率
 * 
 * 3. 为什么提供静态工厂方法？
 *    - 简化对象创建，提高代码可读性
 *    - 隐藏构造细节，提供语义化的创建方式
 *    - 便于后期扩展和维护
 * 
 * 包结构设计思路:
 * - 放在dto.common包下，表明这是通用的数据传输对象
 * - 与业务DTO分离，避免混淆
 * 
 * 命名原因:
 * - ApiResponse明确表达这是API响应对象
 * - 简洁明了，符合RESTful设计规范
 * 
 * 依赖关系:
 * - 不依赖任何业务逻辑，保持高内聚低耦合
 * - 被所有Controller使用，作为基础响应格式
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一API响应结果包装类
 * 
 * 设计原则：
 * 1. 统一性：所有API接口返回格式保持一致
 * 2. 完整性：包含状态码、消息、数据、时间戳等完整信息
 * 3. 扩展性：支持泛型，适应不同类型的响应数据
 * 4. 易用性：提供静态工厂方法，简化使用
 * 
 * 响应格式示例：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": {...},
 *   "timestamp": "2024-08-04T10:30:00"
 * }
 * 
 * @param <T> 响应数据的类型
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     * 200: 成功
     * 400: 客户端错误
     * 401: 未授权
     * 403: 禁止访问
     * 404: 资源不存在
     * 500: 服务器内部错误
     */
    private Integer code;
    
    /**
     * 响应消息
     * 成功时显示操作成功信息
     * 失败时显示具体错误原因
     */
    private String message;
    
    /**
     * 响应数据
     * 成功时包含实际业务数据
     * 失败时可能为null或错误详情
     */
    private T data;
    
    /**
     * 响应时间戳
     * 便于前端调试和问题追踪
     */
    private LocalDateTime timestamp;
    
    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     * 
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    private ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    // ========== 成功响应静态工厂方法 ==========
    
    /**
     * 创建成功响应（无数据）
     * 适用场景：删除操作、更新操作等不需要返回数据的场景
     * 
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }
    
    /**
     * 创建成功响应（带数据）
     * 适用场景：查询操作、创建操作等需要返回数据的场景
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    /**
     * 创建成功响应（自定义消息）
     * 适用场景：需要特定成功提示信息的场景
     * 
     * @param message 自定义成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    // ========== 失败响应静态工厂方法 ==========
    
    /**
     * 创建失败响应（通用错误）
     * 适用场景：一般性业务错误
     * 
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
    
    /**
     * 创建失败响应（自定义状态码）
     * 适用场景：需要特定HTTP状态码的错误场景
     * 
     * @param code 错误状态码
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 创建失败响应（包含错误数据）
     * 适用场景：参数验证失败，需要返回具体错误字段信息
     * 
     * @param code 错误状态码
     * @param message 错误消息
     * @param data 错误数据
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
    
    // ========== 特定场景响应方法 ==========
    
    /**
     * 创建参数错误响应
     * 适用场景：请求参数验证失败
     * 
     * @param message 参数错误消息
     * @return 参数错误响应对象
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }
    
    /**
     * 创建未授权响应
     * 适用场景：用户未登录或token无效
     * 
     * @param message 未授权消息
     * @return 未授权响应对象
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }
    
    /**
     * 创建禁止访问响应
     * 适用场景：用户权限不足
     * 
     * @param message 禁止访问消息
     * @return 禁止访问响应对象
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message, null);
    }
    
    /**
     * 创建资源不存在响应
     * 适用场景：查询的资源不存在
     * 
     * @param message 资源不存在消息
     * @return 资源不存在响应对象
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 判断响应是否成功
     * 
     * @return true-成功，false-失败
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
    
    /**
     * 判断响应是否失败
     * 
     * @return true-失败，false-成功
     */
    public boolean isError() {
        return !isSuccess();
    }
}