package com.ecommerce.exception;

/*
 * 文件职责: 全局异常处理器，统一处理应用中的各种异常
 * 
 * 开发心理活动：
 * 1. 为什么需要全局异常处理？
 *    - 统一异常响应格式，避免前端解析不一致
 *    - 集中处理异常逻辑，提高代码复用性
 *    - 避免异常信息泄露，增强系统安全性
 *    - 便于异常监控和问题定位
 * 
 * 2. 为什么使用@ControllerAdvice？
 *    - AOP切面思想，与业务代码解耦
 *    - 全局生效，无需在每个Controller中重复处理
 *    - Spring提供的标准解决方案，稳定可靠
 * 
 * 3. 异常处理策略：
 *    - 业务异常：返回具体错误信息，引导用户操作
 *    - 系统异常：返回通用错误信息，记录详细日志
 *    - 参数异常：返回参数错误详情，帮助调试
 *    - 安全异常：返回安全提示，保护系统安全
 * 
 * 包结构设计思路:
 * - 放在exception包下，专门处理异常相关逻辑
 * - 与业务逻辑分离，保持关注点清晰
 * 
 * 命名原因:
 * - GlobalExceptionHandler表明这是全局异常处理器
 * - 命名清晰，职责明确
 * 
 * 依赖关系:
 * - 依赖ApiResponse进行统一响应格式化
 * - 被Spring框架自动调用，无需手动依赖
 */

import com.ecommerce.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局异常处理器
 * 
 * 功能说明：
 * 1. 统一处理应用中的各种异常
 * 2. 规范化异常响应格式
 * 3. 记录异常日志便于问题追踪
 * 4. 保护敏感信息不被泄露
 * 
 * 异常处理层次：
 * 1. 业务异常 - 用户操作错误，需要友好提示
 * 2. 参数异常 - 请求参数问题，需要详细说明
 * 3. 安全异常 - 认证授权问题，需要安全处理
 * 4. 系统异常 - 代码或环境问题，需要记录日志
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 业务异常处理 ==========

    /**
     * 处理自定义业务异常
     * 
     * 业务异常特点：
     * - 由业务逻辑主动抛出
     * - 错误信息对用户友好
     * - 无需记录ERROR级别日志
     * 
     * @param e 业务异常
     * @param request HTTP请求对象
     * @return 业务异常响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - 请求路径: {}, 错误信息: {}", request.getRequestURI(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // ========== 参数验证异常处理 ==========

    /**
     * 处理@RequestBody参数验证异常
     * 
     * 触发场景：
     * - @Valid注解验证失败
     * - JSON请求体参数不符合验证规则
     * 
     * @param e 方法参数验证异常
     * @return 参数错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数验证失败: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ApiResponse.badRequest("参数验证失败").data(errors);
    }

    /**
     * 处理@ModelAttribute参数验证异常
     * 
     * 触发场景：
     * - 表单提交参数验证失败
     * - URL参数绑定对象验证失败
     * 
     * @param e 绑定异常
     * @return 参数错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleBindException(BindException e) {
        log.warn("参数绑定异常: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ApiResponse.badRequest("参数绑定失败").data(errors);
    }

    /**
     * 处理@RequestParam和@PathVariable参数验证异常
     * 
     * 触发场景：
     * - 单个参数验证失败（如@NotNull、@Min等）
     * 
     * @param e 约束违反异常
     * @return 参数错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数约束违反: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        
        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        
        return ApiResponse.badRequest("参数约束违反").data(errors);
    }

    /**
     * 处理缺少请求参数异常
     * 
     * 触发场景：
     * - 必需的@RequestParam参数缺失
     * 
     * @param e 缺少请求参数异常  
     * @return 参数错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getMessage());
        String message = String.format("缺少必需参数: %s", e.getParameterName());
        return ApiResponse.badRequest(message);
    }

    /**
     * 处理参数类型不匹配异常
     * 
     * 触发场景：
     * - 请求参数类型与接口参数类型不匹配
     * - 如：接口需要Integer，传入了字符串
     * 
     * @param e 方法参数类型不匹配异常
     * @return 参数错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        String message = String.format("参数 %s 类型不正确，期望类型: %s", 
            e.getName(), e.getRequiredType().getSimpleName());
        return ApiResponse.badRequest(message);
    }

    // ========== 安全异常处理 ==========

    /**
     * 处理认证异常
     * 
     * 触发场景：
     * - 用户未登录
     * - Token无效或过期
     * - 认证信息格式错误
     * 
     * @param e 认证异常
     * @return 未授权响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ApiResponse.unauthorized("认证失败，请重新登录");
    }

    /**
     * 处理凭据错误异常
     * 
     * 触发场景：
     * - 用户名或密码错误
     * - 登录凭据不匹配
     * 
     * @param e 凭据错误异常
     * @return 未授权响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("登录凭据错误: {}", e.getMessage());
        return ApiResponse.unauthorized("用户名或密码错误");
    }

    /**
     * 处理访问拒绝异常
     * 
     * 触发场景：
     * - 用户权限不足
     * - 访问受保护的资源
     * 
     * @param e 访问拒绝异常
     * @return 禁止访问响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return ApiResponse.forbidden("权限不足，拒绝访问");
    }

    // ========== 系统异常处理 ==========

    /**
     * 处理空指针异常
     * 
     * 触发场景：
     * - 代码逻辑错误导致空指针
     * - 外部服务返回空值未处理
     * 
     * @param e 空指针异常
     * @param request HTTP请求对象
     * @return 服务器错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - 请求路径: {}, 错误信息: {}", request.getRequestURI(), e.getMessage(), e);
        return ApiResponse.error("系统内部错误，请稍后重试");
    }

    /**
     * 处理IllegalArgument异常
     * 
     * 触发场景：
     * - 方法参数不合法
     * - 业务逻辑参数校验失败
     * 
     * @param e 非法参数异常
     * @param request HTTP请求对象
     * @return 参数错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常 - 请求路径: {}, 错误信息: {}", request.getRequestURI(), e.getMessage());
        return ApiResponse.badRequest("参数不合法: " + e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     * 
     * 触发场景：
     * - 系统运行时异常
     * - 未预期的错误情况
     * 
     * 安全考虑：
     * - 不向前端暴露具体错误信息
     * - 详细异常信息记录到日志
     * 
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 通用错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 - 请求路径: {}, 异常类型: {}, 错误信息: {}", 
            request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        
        // 生产环境不暴露具体错误信息，开发环境可以显示
        String message = "系统繁忙，请稍后重试";
        
        return ApiResponse.error(message);
    }
}