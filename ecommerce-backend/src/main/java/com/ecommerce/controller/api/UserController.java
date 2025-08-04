package com.ecommerce.controller.api;

/*
 * 文件职责: 用户控制器，提供用户相关的RESTful API接口
 * 
 * 开发心理活动：
 * 1. Controller层设计原则：
 *    - 接收HTTP请求，调用Service处理业务
 *    - 统一返回格式，使用ApiResponse包装
 *    - 参数校验和异常处理，保证接口稳定性
 *    - RESTful风格设计，符合行业标准
 * 
 * 2. API设计考虑：
 *    - 用户认证：注册、登录、登出接口
 *    - 用户管理：信息查询、更新、状态管理
 *    - 数据验证：唯一性检查、格式验证
 *    - 权限控制：基于角色的访问控制
 * 
 * 3. 安全考虑：
 *    - 敏感操作需要认证和权限验证
 *    - 参数校验防止恶意输入
 *    - 日志记录便于安全审计
 *    - 限流和防刷机制
 * 
 * 4. 性能优化：
 *    - 分页查询避免大数据量返回
 *    - 缓存策略提升响应速度
 *    - 异步处理耗时操作
 *    - 合理的数据传输格式
 * 
 * 包结构设计思路:
 * - 放在controller.api包下，专门处理API接口
 * - 与web页面控制器分离，职责清晰
 * 
 * 命名原因:
 * - UserController明确表达用户接口控制功能
 * - 符合Controller后缀的MVC命名规范
 * 
 * 依赖关系:
 * - 依赖UserService进行业务处理
 * - 使用DTO对象进行数据传输
 * - 返回ApiResponse统一响应格式
 */

import com.ecommerce.dto.common.ApiResponse;
import com.ecommerce.dto.request.user.UserLoginRequest;
import com.ecommerce.dto.request.user.UserRegisterRequest;
import com.ecommerce.dto.response.user.UserInfoResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 用户控制器
 * 
 * 功能说明：
 * 1. 提供用户相关的RESTful API接口
 * 2. 处理用户认证（注册、登录、登出）
 * 3. 管理用户信息（查询、更新、删除）
 * 4. 数据验证和权限控制
 * 
 * 接口设计：
 * 1. 认证接口：POST /api/users/register, /login, /logout
 * 2. 信息接口：GET /api/users/{id}, /current, /search
 * 3. 管理接口：PUT /api/users/{id}, DELETE /api/users/{id}
 * 4. 验证接口：GET /api/users/check/username, /email, /phone
 * 
 * 权限控制：
 * 1. 公开接口：注册、登录、基础信息查询
 * 2. 认证接口：个人信息管理、密码修改
 * 3. 管理接口：用户管理、状态修改（管理员）
 * 4. 系统接口：统计信息、批量操作（超级管理员）
 * 
 * 响应格式：
 * 1. 成功响应：ApiResponse.success(data)
 * 2. 错误响应：ApiResponse.error(message)
 * 3. 分页响应：包含分页元数据
 * 4. 业务异常：统一异常处理器处理
 * 
 * 性能特性：
 * 1. 分页查询：避免大数据量返回
 * 2. 参数校验：提前验证，减少无效处理
 * 3. 缓存支持：配合Service层缓存策略
 * 4. 异步处理：耗时操作异步执行
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户相关的API接口")
public class UserController {

    // ========== 依赖注入 ==========

    /**
     * 用户服务接口
     */
    private final UserService userService;

    // ========== 用户认证接口 ==========

    /**
     * 用户注册
     * 
     * 业务流程：
     * 1. 参数校验（Bean Validation + 自定义校验）
     * 2. 调用用户服务进行注册处理
     * 3. 返回注册成功的用户信息
     * 4. 异步发送验证邮件
     * 
     * 安全措施：
     * - 参数校验防止恶意输入
     * - 唯一性检查防止重复注册
     * - 密码强度验证
     * - 注册频率限制（后续实现）
     * 
     * @param request 注册请求参数
     * @return 注册结果和用户信息
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册，创建用户账户")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> register(
            @Valid @RequestBody UserRegisterRequest request) {
        
        log.info("用户注册请求，用户名: {}", request.getUsername());
        
        try {
            UserInfoResponse userInfo = userService.register(request);
            
            log.info("用户注册成功，用户ID: {}, 用户名: {}", userInfo.getId(), userInfo.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(userInfo, "注册成功"));
                    
        } catch (Exception e) {
            log.error("用户注册失败，用户名: {}", request.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("注册失败：" + e.getMessage()));
        }
    }

    /**
     * 用户登录
     * 
     * 业务流程：
     * 1. 验证登录凭据（用户名/邮箱/手机号）
     * 2. 校验密码或验证码
     * 3. 生成访问令牌（后续实现JWT）
     * 4. 更新最后登录时间
     * 5. 返回用户信息和令牌
     * 
     * 支持的登录方式：
     * - 用户名 + 密码
     * - 邮箱 + 密码
     * - 手机号 + 密码
     * - 手机号 + 短信验证码（后续实现）
     * 
     * @param request 登录请求参数
     * @return 登录结果和用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录验证，支持多种登录方式")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "认证失败"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "账户被锁定")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> login(
            @Valid @RequestBody UserLoginRequest request) {
        
        log.info("用户登录请求，凭据: {}", request.getCredential());
        
        try {
            UserInfoResponse userInfo = userService.login(request);
            
            log.info("用户登录成功，用户ID: {}, 用户名: {}", userInfo.getId(), userInfo.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "登录成功"));
                    
        } catch (Exception e) {
            log.warn("用户登录失败，凭据: {}, 原因: {}", request.getCredential(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("登录失败：" + e.getMessage()));
        }
    }

    /**
     * 用户登出
     * 
     * 业务流程：
     * 1. 验证当前用户身份
     * 2. 清除访问令牌
     * 3. 清除RememberMe信息
     * 4. 记录登出日志
     * 
     * 安全措施：
     * - 确保只能登出自己的会话
     * - 清除所有相关的认证信息
     * - 防止令牌重放攻击
     * 
     * @param currentUser 当前登录用户（通过JWT获取）
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，清除会话信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Object currentUser,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        // 注意：这里的currentUser在实际JWT实现中会是UserDetails对象
        // 暂时用Object类型，后续实现JWT时调整
        Long userId = 1L; // 临时写死，后续从JWT中获取
        
        log.info("用户登出请求，用户ID: {}", userId);
        
        try {
            boolean success = userService.logout(userId, authToken);
            
            if (success) {
                log.info("用户登出成功，用户ID: {}", userId);
                return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("登出失败"));
            }
                    
        } catch (Exception e) {
            log.error("用户登出异常，用户ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("登出异常：" + e.getMessage()));
        }
    }

    // ========== 用户信息查询接口 ==========

    /**
     * 获取当前用户信息
     * 
     * 用途：
     * - 用户个人中心信息展示
     * - 前端用户状态维护
     * - 权限验证和菜单渲染
     * 
     * 权限：需要用户登录认证
     * 
     * @param currentUser 当前登录用户
     * @return 当前用户详细信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal Object currentUser) {
        
        Long userId = 1L; // 临时写死，后续从JWT中获取
        
        log.debug("获取当前用户信息，用户ID: {}", userId);
        
        try {
            UserInfoResponse userInfo = userService.getCurrentUserInfo(userId);
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
                    
        } catch (Exception e) {
            log.error("获取当前用户信息失败，用户ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户信息失败：" + e.getMessage()));
        }
    }

    /**
     * 根据用户ID获取用户信息
     * 
     * 权限控制：
     * - 用户可以查看自己的完整信息
     * - 管理员可以查看所有用户信息
     * - 其他用户只能查看公开信息
     * 
     * @param userId 用户ID
     * @param currentUser 当前登录用户
     * @return 用户信息（根据权限过滤）
     */
    @GetMapping("/{userId}")
    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户信息，权限控制")
    @Parameter(name = "userId", description = "用户ID", required = true)
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal Object currentUser) {
        
        Long currentUserId = currentUser != null ? 1L : null; // 临时处理，后续从JWT获取
        
        log.debug("查询用户信息，用户ID: {}, 当前用户ID: {}", userId, currentUserId);
        
        try {
            Optional<UserInfoResponse> userInfoOpt = userService.getUserById(userId, currentUserId);
            
            if (userInfoOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(userInfoOpt.get()));
            } else {
                return ResponseEntity.notFound()
                        .build();
            }
                    
        } catch (Exception e) {
            log.error("查询用户信息失败，用户ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询用户信息失败：" + e.getMessage()));
        }
    }

    /**
     * 根据用户名获取用户信息
     * 
     * 用途：
     * - @提及功能的用户查找
     * - 用户搜索功能
     * - 第三方集成的用户查询
     * 
     * @param username 用户名
     * @param currentUser 当前登录用户
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户信息", description = "根据用户名查询用户信息")
    @Parameter(name = "username", description = "用户名", required = true)
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal Object currentUser) {
        
        Long currentUserId = currentUser != null ? 1L : null; // 临时处理
        
        log.debug("根据用户名查询用户信息，用户名: {}", username);
        
        try {
            Optional<UserInfoResponse> userInfoOpt = userService.getUserByUsername(username, currentUserId);
            
            if (userInfoOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(userInfoOpt.get()));
            } else {
                return ResponseEntity.notFound()
                        .build();
            }
                    
        } catch (Exception e) {
            log.error("根据用户名查询用户信息失败，用户名: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询用户信息失败：" + e.getMessage()));
        }
    }

    // ========== 用户搜索接口 ==========

    /**
     * 搜索用户列表
     * 
     * 功能特性：
     * - 支持关键词模糊搜索
     * - 支持状态和角色筛选
     * - 分页展示避免大数据量
     * - 权限控制信息展示
     * 
     * @param keyword 搜索关键词
     * @param status 用户状态筛选
     * @param role 用户角色筛选
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sort 排序字段
     * @param currentUser 当前登录用户
     * @return 用户分页列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户列表", description = "根据条件搜索用户，支持分页和筛选")
    public ResponseEntity<ApiResponse<Page<UserInfoResponse>>> searchUsers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态筛选") @RequestParam(required = false) User.UserStatus status,
            @Parameter(description = "用户角色筛选") @RequestParam(required = false) User.UserRole role,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal Object currentUser) {
        
        Long currentUserId = currentUser != null ? 1L : null; // 临时处理
        
        log.debug("搜索用户列表，关键词: {}, 状态: {}, 角色: {}, 页码: {}, 大小: {}", 
                 keyword, status, role, page, size);
        
        try {
            // 创建分页参数
            Sort.Direction direction = Sort.Direction.DESC; // 默认降序
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
            
            Page<UserInfoResponse> userPage = userService.searchUsers(
                    keyword, status, role, pageable, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(userPage));
                    
        } catch (Exception e) {
            log.error("搜索用户列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索用户失败：" + e.getMessage()));
        }
    }

    /**
     * 获取活跃用户列表
     * 
     * 业务定义：
     * - 最近30天有登录记录的用户
     * - 排除锁定和禁用的用户
     * - 按最后登录时间降序排列
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 活跃用户分页列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃用户列表", description = "获取最近活跃的用户列表")
    public ResponseEntity<ApiResponse<Page<UserInfoResponse>>> getActiveUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        log.debug("获取活跃用户列表，页码: {}, 大小: {}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, 
                    Sort.by(Sort.Direction.DESC, "lastLoginAt"));
            
            Page<UserInfoResponse> activeUsers = userService.getActiveUsers(pageable);
            
            return ResponseEntity.ok(ApiResponse.success(activeUsers));
                    
        } catch (Exception e) {
            log.error("获取活跃用户列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取活跃用户失败：" + e.getMessage()));
        }
    }

    // ========== 用户信息更新接口 ==========

    /**
     * 更新用户基本信息
     * 
     * 可更新字段：
     * - 昵称、头像、性别、生日
     * - 个人简介
     * 
     * 安全限制：
     * - 只能更新自己的信息
     * - 敏感字段需要单独接口
     * - 管理员可以更新任何用户信息
     * 
     * @param userId 用户ID
     * @param userInfo 更新信息
     * @param currentUser 当前登录用户
     * @return 更新后的用户信息
     */
    @PutMapping("/{userId}")
    @Operation(summary = "更新用户基本信息", description = "更新用户的基本信息，如昵称、头像等")
    @PreAuthorize("isAuthenticated() and (#userId == authentication.principal.id or hasRole('ADMIN'))")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUserInfo(
            @PathVariable Long userId,
            @Valid @RequestBody UserInfoResponse userInfo,
            @AuthenticationPrincipal Object currentUser) {
        
        log.info("更新用户信息，用户ID: {}", userId);
        
        try {
            UserInfoResponse updatedUserInfo = userService.updateUserInfo(userId, userInfo);
            
            log.info("用户信息更新成功，用户ID: {}", userId);
            
            return ResponseEntity.ok(ApiResponse.success(updatedUserInfo, "信息更新成功"));
                    
        } catch (Exception e) {
            log.error("更新用户信息失败，用户ID: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新失败：" + e.getMessage()));
        }
    }

    /**
     * 更新用户头像
     * 
     * 处理流程：
     * 1. 验证图片格式和大小
     * 2. 上传图片到文件存储
     * 3. 更新用户头像URL
     * 4. 删除旧头像文件
     * 
     * @param userId 用户ID
     * @param avatarUrl 新头像URL
     * @param currentUser 当前登录用户
     * @return 更新结果
     */
    @PutMapping("/{userId}/avatar")
    @Operation(summary = "更新用户头像", description = "更新用户头像，支持图片上传")
    @PreAuthorize("isAuthenticated() and (#userId == authentication.principal.id or hasRole('ADMIN'))")
    public ResponseEntity<ApiResponse<Void>> updateUserAvatar(
            @PathVariable Long userId,
            @Parameter(description = "新头像URL") @RequestParam String avatarUrl,
            @AuthenticationPrincipal Object currentUser) {
        
        log.info("更新用户头像，用户ID: {}", userId);
        
        try {
            boolean success = userService.updateUserAvatar(userId, avatarUrl);
            
            if (success) {
                log.info("用户头像更新成功，用户ID: {}", userId);
                return ResponseEntity.ok(ApiResponse.success(null, "头像更新成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("头像更新失败"));
            }
                    
        } catch (Exception e) {
            log.error("更新用户头像失败，用户ID: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("头像更新失败：" + e.getMessage()));
        }
    }

    // ========== 数据验证接口 ==========

    /**
     * 检查用户名是否可用
     * 
     * 用途：
     * - 注册时的实时验证
     * - 用户名修改时的重复检查
     * - 前端表单验证提示
     * 
     * @param username 用户名
     * @return 可用性检查结果
     */
    @GetMapping("/check/username")
    @Operation(summary = "检查用户名可用性", description = "检查指定用户名是否可用")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailable(
            @Parameter(description = "用户名", required = true) @RequestParam String username) {
        
        log.debug("检查用户名可用性，用户名: {}", username);
        
        try {
            boolean available = userService.isUsernameAvailable(username);
            
            String message = available ? "用户名可用" : "用户名已被使用";
            return ResponseEntity.ok(ApiResponse.success(available, message));
                    
        } catch (Exception e) {
            log.error("检查用户名可用性失败，用户名: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败：" + e.getMessage()));
        }
    }

    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱地址
     * @return 可用性检查结果
     */
    @GetMapping("/check/email")
    @Operation(summary = "检查邮箱可用性", description = "检查指定邮箱是否可用")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailable(
            @Parameter(description = "邮箱地址", required = true) @RequestParam String email) {
        
        log.debug("检查邮箱可用性，邮箱: {}", email);
        
        try {
            boolean available = userService.isEmailAvailable(email);
            
            String message = available ? "邮箱可用" : "邮箱已被使用";
            return ResponseEntity.ok(ApiResponse.success(available, message));
                    
        } catch (Exception e) {
            log.error("检查邮箱可用性失败，邮箱: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败：" + e.getMessage()));
        }
    }

    /**
     * 检查手机号是否可用
     * 
     * @param phone 手机号
     * @return 可用性检查结果
     */
    @GetMapping("/check/phone")
    @Operation(summary = "检查手机号可用性", description = "检查指定手机号是否可用")
    public ResponseEntity<ApiResponse<Boolean>> checkPhoneAvailable(
            @Parameter(description = "手机号", required = true) @RequestParam String phone) {
        
        log.debug("检查手机号可用性，手机号: {}", phone);
        
        try {
            boolean available = userService.isPhoneAvailable(phone);
            
            String message = available ? "手机号可用" : "手机号已被使用";
            return ResponseEntity.ok(ApiResponse.success(available, message));
                    
        } catch (Exception e) {
            log.error("检查手机号可用性失败，手机号: {}", phone, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败：" + e.getMessage()));
        }
    }

    /**
     * 验证密码强度
     * 
     * 检查项目：
     * - 长度要求（至少8位）
     * - 复杂度要求（大小写、数字、特殊字符）
     * - 常见密码检查
     * - 与用户信息相关性检查
     * 
     * @param password 密码
     * @param currentUser 当前用户（可选，用于相关性检查）
     * @return 密码强度评估结果
     */
    @PostMapping("/validate/password")
    @Operation(summary = "验证密码强度", description = "验证密码强度，提供改进建议")
    public ResponseEntity<ApiResponse<UserService.PasswordStrengthResult>> validatePasswordStrength(
            @Parameter(description = "待验证密码", required = true) @RequestParam String password,
            @AuthenticationPrincipal Object currentUser) {
        
        Long userId = currentUser != null ? 1L : null; // 临时处理
        
        log.debug("验证密码强度，用户ID: {}", userId);
        
        try {
            UserService.PasswordStrengthResult result = userService.validatePasswordStrength(password, userId);
            
            return ResponseEntity.ok(ApiResponse.success(result));
                    
        } catch (Exception e) {
            log.error("验证密码强度失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("验证失败：" + e.getMessage()));
        }
    }

    // ========== 统计信息接口（管理员） ==========

    /**
     * 获取用户统计信息
     * 
     * 统计维度：
     * - 总用户数、活跃用户数
     * - 各状态用户统计
     * - 各角色用户统计
     * - 注册趋势分析
     * 
     * 权限：仅管理员可访问
     * 
     * @return 用户统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计信息", description = "获取用户相关的统计数据，仅管理员可访问")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserService.UserStatistics>> getUserStatistics() {
        
        log.debug("获取用户统计信息");
        
        try {
            UserService.UserStatistics statistics = userService.getUserStatistics();
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
                    
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取统计信息失败：" + e.getMessage()));
        }
    }

    // ========== 异常处理说明 ==========
    /*
     * 异常处理策略：
     * 1. Controller层主要捕获和记录异常
     * 2. 业务异常(BusinessException)由GlobalExceptionHandler统一处理
     * 3. 系统异常返回500状态码和通用错误信息
     * 4. 参数校验异常返回400状态码和具体错误信息
     * 
     * 日志记录原则：
     * 1. 用户操作记录为info级别
     * 2. 业务异常记录为warn级别
     * 3. 系统异常记录为error级别
     * 4. 敏感信息不记录到日志中
     * 
     * 返回格式统一：
     * 1. 成功：ApiResponse.success(data, message)
     * 2. 失败：ApiResponse.error(message)
     * 3. HTTP状态码与业务状态码保持一致
     * 4. 错误信息对用户友好，对开发者有用
     */
}