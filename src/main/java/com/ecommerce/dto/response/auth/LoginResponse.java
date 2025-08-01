/*
文件级分析：
- 职责：用户登录响应DTO，封装登录成功后返回给前端的数据
- 包结构考虑：位于dto.response.auth包下，与请求DTO对应组织
- 命名原因：LoginResponse明确表示这是登录响应的数据传输对象
- 调用关系：被Controller返回给前端，包含认证令牌和用户基本信息

设计思路：
1. 包含JWT访问令牌和刷新令牌，支持无状态认证
2. 返回用户基本信息，减少前端的额外请求
3. 包含令牌过期时间，便于前端进行自动刷新
4. 过滤敏感信息，只返回必要的用户数据
5. 支持权限信息返回，便于前端进行页面控制
*/
package com.ecommerce.dto.response.auth;

import com.ecommerce.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户登录响应DTO
 * 
 * 封装用户登录成功后返回的数据，包括：
 * 1. JWT认证令牌信息
 * 2. 用户基本信息（脱敏处理）
 * 3. 权限和角色信息
 * 4. 登录相关的元数据
 * 
 * 响应示例：
 * ```json
 * {
 *   "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400,
 *   "userInfo": {
 *     "id": 1,
 *     "username": "john_doe",
 *     "nickname": "John",
 *     "email": "john@example.com",
 *     "avatarUrl": "https://example.com/avatar.jpg",
 *     "status": "ACTIVE"
 *   },
 *   "permissions": ["user:read", "order:create"],
 *   "loginTime": "2024-01-01 12:00:00"
 * }
 * ```
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class LoginResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 令牌信息 ========================
    
    /**
     * 访问令牌
     * JWT格式的访问令牌，用于API请求的身份验证
     * 前端需要在每个请求的Authorization头中携带此令牌
     */
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;
    
    /**
     * 刷新令牌
     * 用于在访问令牌过期时获取新的访问令牌
     * 通常有效期比访问令牌更长
     */
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
    
    /**
     * 令牌类型
     * 通常为"Bearer"，表示承载者令牌
     */
    @Schema(description = "令牌类型", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 访问令牌过期时间（秒）
     * 前端可以根据此值进行令牌自动刷新
     */
    @Schema(description = "访问令牌过期时间（秒）", example = "86400")
    private Long expiresIn;
    
    /**
     * 刷新令牌过期时间（秒）
     * 刷新令牌的有效期，过期后需要重新登录
     */
    @Schema(description = "刷新令牌过期时间（秒）", example = "604800")
    private Long refreshExpiresIn;
    
    // ======================== 用户信息 ========================
    
    /**
     * 用户基本信息
     * 嵌套对象，包含用户的基本信息
     */
    @Schema(description = "用户基本信息")
    private UserInfo userInfo;
    
    /**
     * 用户权限列表
     * 用户拥有的权限标识列表，前端可以根据此信息进行页面控制
     */
    @Schema(description = "用户权限列表", example = "[\"user:read\", \"order:create\"]")
    private List<String> permissions;
    
    /**
     * 用户角色列表
     * 用户拥有的角色名称列表
     */
    @Schema(description = "用户角色列表", example = "[\"USER\", \"VIP\"]")
    private List<String> roles;
    
    // ======================== 登录元数据 ========================
    
    /**
     * 登录时间
     * 本次登录的时间戳
     */
    @Schema(description = "登录时间", example = "2024-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
    
    /**
     * 登录IP地址
     * 记录本次登录的IP地址，用于安全审计
     */
    @Schema(description = "登录IP地址", example = "192.168.1.100")
    private String loginIp;
    
    /**
     * 是否首次登录
     * 标识用户是否为首次登录，可以用于引导流程
     */
    @Schema(description = "是否首次登录", example = "false")
    @Builder.Default
    private Boolean firstLogin = false;
    
    /**
     * 是否需要修改密码
     * 某些情况下（如管理员重置密码）需要用户强制修改密码
     */
    @Schema(description = "是否需要修改密码", example = "false")
    @Builder.Default
    private Boolean needChangePassword = false;
    
    // ======================== 嵌套类：用户信息 ========================
    
    /**
     * 用户基本信息内嵌类
     * 只包含安全的、可以公开的用户信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户基本信息")
    public static class UserInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 用户ID
         */
        @Schema(description = "用户ID", example = "1")
        private Long id;
        
        /**
         * 用户名
         */
        @Schema(description = "用户名", example = "john_doe")
        private String username;
        
        /**
         * 昵称
         */
        @Schema(description = "昵称", example = "John")
        private String nickname;
        
        /**
         * 邮箱地址
         */
        @Schema(description = "邮箱地址", example = "john@example.com")
        private String email;
        
        /**
         * 手机号码（脱敏处理）
         */
        @Schema(description = "手机号码", example = "138****5678")
        private String phone;
        
        /**
         * 头像URL
         */
        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        private String avatarUrl;
        
        /**
         * 用户状态
         */
        @Schema(description = "用户状态", example = "ACTIVE")
        private UserStatus status;
        
        /**
         * 性别
         * 0-未知，1-男，2-女
         */
        @Schema(description = "性别", example = "1")
        private Integer gender;
        
        /**
         * 邮箱是否已验证
         */
        @Schema(description = "邮箱是否已验证", example = "true")
        private Boolean emailVerified;
        
        /**
         * 手机号是否已验证
         */
        @Schema(description = "手机号是否已验证", example = "true")
        private Boolean phoneVerified;
        
        /**
         * 注册时间
         */
        @Schema(description = "注册时间", example = "2024-01-01 10:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        /**
         * 最后登录时间
         */
        @Schema(description = "最后登录时间", example = "2024-01-01 11:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastLoginAt;
    }
    
    // ======================== 便捷构造方法 ========================
    
    /**
     * 创建成功的登录响应
     * 
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期时间
     * @param userInfo 用户信息
     * @return 登录响应对象
     */
    public static LoginResponse success(String accessToken, String refreshToken, 
                                      Long expiresIn, UserInfo userInfo) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userInfo(userInfo)
                .loginTime(LocalDateTime.now())
                .firstLogin(false)
                .needChangePassword(false)
                .build();
    }
}