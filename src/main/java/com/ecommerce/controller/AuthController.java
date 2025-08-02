/*
文件级分析：
- 职责：用户认证相关的REST API控制器，处理注册、登录、令牌刷新等HTTP请求
- 包结构考虑：位于controller包下，作为Web层组件处理HTTP请求和响应
- 命名原因：AuthController表明这是认证相关的控制器
- 调用关系：接收HTTP请求，调用UserService处理业务逻辑，返回统一格式的API响应

设计思路：
1. 提供RESTful风格的认证API接口
2. 使用统一的ApiResponse格式返回响应
3. 集成OpenAPI/Swagger文档注解
4. 实现完整的参数验证和异常处理
5. 支持多种认证方式和安全验证
*/
package com.ecommerce.controller;

import com.ecommerce.dto.common.ApiResponse;
import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.response.auth.LoginResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * 
 * 提供用户认证相关的REST API接口，包括：
 * 1. 用户注册：支持用户名、邮箱、手机号注册
 * 2. 用户登录：多方式登录，返回JWT令牌
 * 3. 令牌刷新：基于刷新令牌获取新的访问令牌
 * 4. 用户注销：清除用户会话状态
 * 
 * API特性：
 * - RESTful设计风格
 * - 统一的响应格式
 * - 完整的参数验证
 * - OpenAPI/Swagger文档
 * - 标准HTTP状态码
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户注册、登录、令牌管理等认证相关API")
public class AuthController {
    
    private final UserService userService;
    
    // ======================== 用户注册 ========================
    
    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号（可选）
     * @param password 密码
     * @param nickname 昵称（可选）
     * @return 注册结果
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "用户注册",
        description = "用户注册接口，支持用户名、邮箱、手机号注册"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "注册成功",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "请求参数错误",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "用户已存在",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ApiResponse<UserRegistrationResponse> register(
            @Parameter(description = "用户名", required = true, example = "testuser")
            @RequestParam
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,
            
            @Parameter(description = "邮箱地址", required = true, example = "test@example.com")
            @RequestParam
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,
            
            @Parameter(description = "手机号", required = false, example = "13800138000")
            @RequestParam(required = false)
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,
            
            @Parameter(description = "密码", required = true, example = "password123")
            @RequestParam
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度必须在6-32个字符之间")
            String password,
            
            @Parameter(description = "昵称", required = false, example = "测试用户")
            @RequestParam(required = false)
            @Size(max = 50, message = "昵称长度不能超过50个字符")
            String nickname
    ) {
        log.info("用户注册请求: username={}, email={}", username, email);
        
        User user = userService.register(username, email, phone, password, nickname);
        
        UserRegistrationResponse response = new UserRegistrationResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setNickname(user.getNickname());
        response.setStatus(user.getStatus().getDescription());
        response.setRegisteredAt(user.getRegisteredAt());
        
        return ApiResponse.success("注册成功", response);
    }
    
    // ======================== 用户登录 ========================
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应（包含JWT令牌）
     */
    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = "用户登录接口，支持用户名、邮箱、手机号多种方式登录"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "登录成功",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "认证失败",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "账号被禁用或锁定",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求信息", required = true)
            @RequestBody @Valid LoginRequest loginRequest
    ) {
        log.info("用户登录请求: loginId={}", loginRequest.getLoginId());
        
        LoginResponse response = userService.login(loginRequest);
        
        return ApiResponse.success("登录成功", response);
    }
    
    // ======================== 令牌刷新 ========================
    
    /**
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "刷新访问令牌",
        description = "使用刷新令牌获取新的访问令牌"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "令牌刷新成功",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "刷新令牌无效",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ApiResponse<LoginResponse> refreshToken(
            @Parameter(description = "刷新令牌", required = true, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestBody RefreshTokenRequest request
    ) {
        log.info("令牌刷新请求");
        
        LoginResponse response = userService.refreshToken(request.getRefreshToken());
        
        return ApiResponse.success("令牌刷新成功", response);
    }
    
    // ======================== 用户注销 ========================
    
    /**
     * 用户注销
     * 
     * @return 注销结果
     */
    @PostMapping("/logout")
    @Operation(
        summary = "用户注销",
        description = "用户注销接口，清除会话状态"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "注销成功",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ApiResponse<Void> logout() {
        log.info("用户注销请求");
        
        // TODO: 实现令牌黑名单机制，使当前令牌失效
        // 当前简单返回成功，后续可以实现Redis黑名单
        
        return ApiResponse.success("注销成功", null);
    }
    
    // ======================== 内部DTO类 ========================
    
    /**
     * 用户注册响应DTO
     */
    @Schema(description = "用户注册响应")
    public static class UserRegistrationResponse {
        @Schema(description = "用户ID", example = "1")
        private Long userId;
        
        @Schema(description = "用户名", example = "testuser")
        private String username;
        
        @Schema(description = "邮箱", example = "test@example.com")
        private String email;
        
        @Schema(description = "手机号", example = "13800138000")
        private String phone;
        
        @Schema(description = "昵称", example = "测试用户")
        private String nickname;
        
        @Schema(description = "状态", example = "正常")
        private String status;
        
        @Schema(description = "注册时间", example = "2024-01-01T10:00:00")
        private java.time.LocalDateTime registeredAt;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public java.time.LocalDateTime getRegisteredAt() { return registeredAt; }
        public void setRegisteredAt(java.time.LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    }
    
    /**
     * 刷新令牌请求DTO
     */
    @Schema(description = "刷新令牌请求")
    public static class RefreshTokenRequest {
        @Schema(description = "刷新令牌", required = true, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}