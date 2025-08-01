/*
文件级分析：
- 职责：全局异常处理器，统一处理应用中的各种异常
- 包结构考虑：位于exception包下，与异常类统一管理
- 命名原因：GlobalExceptionHandler明确表示这是全局异常处理器
- 调用关系：被Spring框架自动调用，拦截Controller层抛出的异常

设计思路：
1. 使用@ControllerAdvice注解，实现全局异常拦截
2. 针对不同类型的异常提供不同的处理逻辑
3. 将异常转换为统一的ApiResponse格式
4. 区分业务异常和系统异常，给予不同的处理策略
5. 记录异常日志，便于问题排查
*/
package com.ecommerce.exception;

import com.ecommerce.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * 统一处理应用程序中的各种异常，将异常转换为统一的API响应格式。
 * 主要处理以下类型的异常：
 * 1. 业务异常 - 可预期的业务逻辑异常
 * 2. 参数校验异常 - 请求参数不合法
 * 3. 安全异常 - 认证和授权异常
 * 4. 系统异常 - 不可预期的系统级异常
 * 
 * 异常处理原则：
 * - 业务异常：返回具体的错误信息给前端
 * - 系统异常：记录详细日志，返回通用错误信息
 * - 安全异常：记录安全日志，返回标准安全提示
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // ======================== 业务异常处理 ========================
    
    /**
     * 处理业务异常
     * 业务异常通常是可预期的，直接返回异常信息给前端
     * 
     * @param e 业务异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.failure(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // ======================== 参数校验异常处理 ========================
    
    /**
     * 处理方法参数校验异常（@Valid注解触发）
     * 
     * @param e 方法参数校验异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        log.warn("参数校验失败: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = buildValidationErrorMessage(e.getBindingResult());
        ApiResponse<Void> response = ApiResponse.validationFailed(errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理数据绑定异常
     * 
     * @param e 数据绑定异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("数据绑定异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = buildValidationErrorMessage(e.getBindingResult());
        ApiResponse<Void> response = ApiResponse.validationFailed(errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理约束违反异常（单个参数校验）
     * 
     * @param e 约束违反异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        log.warn("约束违反异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        ApiResponse<Void> response = ApiResponse.validationFailed(errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理缺少请求参数异常
     * 
     * @param e 缺少请求参数异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        
        log.warn("缺少请求参数: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = String.format("缺少必需的请求参数: %s", e.getParameterName());
        ApiResponse<Void> response = ApiResponse.validationFailed(errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理方法参数类型不匹配异常
     * 
     * @param e 方法参数类型不匹配异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        log.warn("参数类型不匹配: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = String.format("参数 %s 的值 %s 类型不正确", e.getName(), e.getValue());
        ApiResponse<Void> response = ApiResponse.validationFailed(errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理HTTP消息不可读异常（JSON格式错误等）
     * 
     * @param e HTTP消息不可读异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        
        log.warn("HTTP消息不可读: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.validationFailed("请求数据格式不正确");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // ======================== 安全异常处理 ========================
    
    /**
     * 处理认证异常
     * 
     * @param e 认证异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        
        log.warn("认证失败: {}, 请求路径: {}, IP: {}", 
                e.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ApiResponse<Void> response = ApiResponse.unauthorized();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理认证凭据错误异常
     * 
     * @param e 认证凭据错误异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        
        log.warn("认证凭据错误: {}, 请求路径: {}, IP: {}", 
                e.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ApiResponse<Void> response = ApiResponse.failure(401, "用户名或密码错误");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理访问拒绝异常
     * 
     * @param e 访问拒绝异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        log.warn("访问被拒绝: {}, 请求路径: {}, IP: {}", 
                e.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ApiResponse<Void> response = ApiResponse.forbidden();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    // ======================== 系统异常处理 ========================
    
    /**
     * 处理404异常
     * 
     * @param e 404异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        
        log.warn("请求路径不存在: {}", request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.notFound();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 处理数据完整性违反异常
     * 
     * @param e 数据完整性违反异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException e, HttpServletRequest request) {
        
        log.error("数据完整性违反: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Void> response = ApiResponse.failure("数据操作失败，请检查数据完整性");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理其他未捕获的异常
     * 
     * @param e 未知异常
     * @param request HTTP请求
     * @return 统一响应格式
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Void> response = ApiResponse.internalError();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    // ======================== 工具方法 ========================
    
    /**
     * 构建参数校验错误消息
     * 
     * @param bindingResult 绑定结果
     * @return 错误消息字符串
     */
    private String buildValidationErrorMessage(BindingResult bindingResult) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (fieldErrors.isEmpty()) {
            return "参数校验失败";
        }
        
        return fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}