/*
文件级分析：
- 职责：统一API响应格式，包装所有接口的返回结果
- 包结构考虑：放在dto.common包下，作为数据传输的通用对象
- 命名原因：ApiResponse清晰表明这是API响应类
- 调用关系：被所有Controller层返回，确保API响应格式统一

设计思路：
1. 采用泛型设计，支持不同类型的数据返回
2. 包含code（状态码）、message（消息）、data（数据）三个核心字段
3. 提供静态工厂方法，简化创建过程
4. 支持链式调用，提高代码可读性
5. 实现序列化接口，支持缓存和网络传输
*/
package com.ecommerce.dto.common;

import com.ecommerce.constants.CommonConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一API响应结果封装类
 * 
 * 该类用于统一封装所有API接口的响应结果，确保前后端数据交互的一致性。
 * 采用泛型设计，支持返回不同类型的业务数据。
 * 
 * 响应格式：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": {...},
 *   "timestamp": "2024-01-01 12:00:00"
 * }
 * 
 * @param <T> 响应数据的泛型类型
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 忽略null字段
public class ApiResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 响应状态码 */
    private Integer code;
    
    /** 响应消息 */
    private String message;
    
    /** 响应数据 */
    private T data;
    
    /** 响应时间戳 */
    @JsonFormat(pattern = CommonConstants.DEFAULT_DATETIME_FORMAT)
    private LocalDateTime timestamp;
    
    /**
     * 全参构造方法
     * 
     * @param code 状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 无数据构造方法
     * 
     * @param code 状态码
     * @param message 响应消息
     */
    public ApiResponse(Integer code, String message) {
        this(code, message, null);
    }
    
    // ======================== 成功响应静态工厂方法 ========================
    
    /**
     * 成功响应（带数据）
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(CommonConstants.SUCCESS_CODE, CommonConstants.SUCCESS_MESSAGE, data);
    }
    
    /**
     * 成功响应（无数据）
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(CommonConstants.SUCCESS_CODE, CommonConstants.SUCCESS_MESSAGE);
    }
    
    /**
     * 成功响应（自定义消息）
     * 
     * @param message 自定义消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(CommonConstants.SUCCESS_CODE, message);
    }
    
    /**
     * 成功响应（自定义消息和数据）
     * 
     * @param message 自定义消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(CommonConstants.SUCCESS_CODE, message, data);
    }
    
    // ======================== 失败响应静态工厂方法 ========================
    
    /**
     * 失败响应（默认消息）
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> failure() {
        return new ApiResponse<>(CommonConstants.BUSINESS_ERROR_CODE, CommonConstants.FAILURE_MESSAGE);
    }
    
    /**
     * 失败响应（自定义消息）
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(CommonConstants.BUSINESS_ERROR_CODE, message);
    }
    
    /**
     * 失败响应（自定义状态码和消息）
     * 
     * @param code 状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> failure(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }
    
    /**
     * 失败响应（自定义状态码、消息和数据）
     * 
     * @param code 状态码
     * @param message 错误消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> failure(Integer code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
    
    // ======================== 特定场景响应方法 ========================
    
    /**
     * 参数校验失败响应
     * 
     * @param message 校验失败消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> validationFailed(String message) {
        return new ApiResponse<>(CommonConstants.BUSINESS_ERROR_CODE, message);
    }
    
    /**
     * 未授权响应
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(CommonConstants.UNAUTHORIZED_CODE, CommonConstants.UNAUTHORIZED_MESSAGE);
    }
    
    /**
     * 禁止访问响应
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(CommonConstants.FORBIDDEN_CODE, CommonConstants.FORBIDDEN_MESSAGE);
    }
    
    /**
     * 资源不存在响应
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> notFound() {
        return new ApiResponse<>(CommonConstants.NOT_FOUND_CODE, CommonConstants.RESOURCE_NOT_FOUND_MESSAGE);
    }
    
    /**
     * 服务器内部错误响应
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> internalError() {
        return new ApiResponse<>(CommonConstants.INTERNAL_ERROR_CODE, CommonConstants.INTERNAL_ERROR_MESSAGE);
    }
    
    // ======================== 链式调用方法 ========================
    
    /**
     * 设置响应数据（支持链式调用）
     * 
     * @param data 响应数据
     * @return 当前ApiResponse实例
     */
    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
    
    /**
     * 设置响应消息（支持链式调用）
     * 
     * @param message 响应消息
     * @return 当前ApiResponse实例
     */
    public ApiResponse<T> setMessage(String message) {
        this.message = message;
        return this;
    }
    
    // ======================== 便捷判断方法 ========================
    
    /**
     * 判断是否成功响应
     * 
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return Integer.valueOf(CommonConstants.SUCCESS_CODE).equals(this.code);
    }
    
    /**
     * 判断是否失败响应
     * 
     * @return true表示失败，false表示成功
     */
    public boolean isFailure() {
        return !isSuccess();
    }
}