package com.ecommerce.service.impl;

/*
 * 文件职责: 用户服务实现类，实现用户相关的核心业务逻辑
 * 
 * 开发心理活动：
 * 1. Service实现的核心职责：
 *    - 业务逻辑的具体实现，连接Controller和Repository
 *    - 数据转换和验证，确保业务规则执行
 *    - 事务管理和异常处理，保证数据一致性
 *    - 缓存策略和性能优化，提升系统响应速度
 * 
 * 2. 依赖注入设计：
 *    - Repository层：数据访问和持久化
 *    - 密码编码器：安全的密码处理
 *    - 缓存组件：提升查询性能
 *    - 消息服务：异步通知和验证
 * 
 * 3. 事务策略：
 *    - 查询方法使用只读事务，提升性能
 *    - 修改方法使用读写事务，保证一致性
 *    - 复杂业务使用声明式事务管理
 *    - 异常时自动回滚，保护数据完整性
 * 
 * 4. 异常处理：
 *    - 使用自定义BusinessException表达业务错误
 *    - 统一的错误消息和错误码管理
 *    - 详细的日志记录便于问题追踪
 *    - 敏感信息不暴露给前端
 * 
 * 包结构设计思路:
 * - 放在service.impl包下，与接口分离
 * - 实现具体的业务逻辑和数据处理
 * 
 * 命名原因:
 * - UserServiceImpl表明这是UserService的实现类
 * - 符合Impl后缀的实现类命名规范
 * 
 * 依赖关系:
 * - 实现UserService接口，提供业务逻辑
 * - 依赖UserRepository进行数据访问
 * - 依赖Spring Security进行密码处理
 * - 被Controller层注入使用
 */

import com.ecommerce.dto.request.user.UserLoginRequest;
import com.ecommerce.dto.request.user.UserRegisterRequest;
import com.ecommerce.dto.response.user.UserInfoResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.jpa.UserRepository;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 * 
 * 实现说明：
 * 1. 实现UserService接口的所有业务方法
 * 2. 集成Spring Security进行密码处理
 * 3. 使用声明式事务管理数据一致性
 * 4. 集成缓存提升查询性能
 * 5. 统一异常处理和日志记录
 * 
 * 技术特点：
 * 1. 依赖注入：使用@RequiredArgsConstructor自动注入
 * 2. 事务管理：@Transactional注解控制事务边界
 * 3. 缓存集成：@Cacheable等注解实现查询缓存
 * 4. 异常处理：BusinessException统一业务异常
 * 5. 日志记录：详细的操作日志便于问题追踪
 * 
 * 性能优化：
 * 1. 查询缓存：常用查询结果缓存
 * 2. 批量操作：减少数据库交互次数
 * 3. 懒加载：按需加载关联数据
 * 4. 索引优化：基于数据库索引的高效查询
 * 
 * 安全考虑：
 * 1. 密码加密：使用Spring Security的密码编码器
 * 2. 权限控制：基于用户角色的访问控制
 * 3. 数据脱敏：敏感信息的安全处理
 * 4. 参数验证：防止恶意输入和注入攻击
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    // ========== 依赖注入 ==========

    /**
     * 用户数据访问接口
     */
    private final UserRepository userRepository;

    /**
     * 密码编码器
     * Spring Security提供的密码加密工具
     */
    private final PasswordEncoder passwordEncoder;

    // ========== 用户认证服务实现 ==========

    /**
     * 用户注册实现
     * 
     * 实现逻辑：
     * 1. 参数验证和唯一性检查
     * 2. 密码加密处理
     * 3. 创建用户实体并保存
     * 4. 转换为响应DTO返回
     * 5. 异步发送验证邮件
     * 
     * 事务控制：
     * - 使用读写事务确保数据一致性
     * - 业务异常自动回滚
     * 
     * 性能考虑：
     * - 唯一性检查使用数据库约束
     * - 密码加密使用高效算法
     * 
     * @param request 注册请求参数
     * @return 新注册用户信息
     * @throws BusinessException 业务异常
     */
    @Override
    public UserInfoResponse register(UserRegisterRequest request) {
        log.info("开始用户注册，请求参数: {}", request.sanitizeForLogging());
        
        try {
            // 1. 验证参数完整性
            validateRegisterRequest(request);
            
            // 2. 检查用户名唯一性
            if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
                throw BusinessException.alreadyExists("用户名 '" + request.getUsername() + "'");
            }
            
            // 3. 检查邮箱唯一性
            if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw BusinessException.alreadyExists("邮箱地址 '" + request.getEmail() + "'");
            }
            
            // 4. 检查手机号唯一性（如果提供）
            if (StringUtils.hasText(request.getPhone()) && 
                userRepository.existsByPhoneAndDeletedFalse(request.getPhone())) {
                throw BusinessException.alreadyExists("手机号 '" + request.getPhone() + "'");
            }
            
            // 5. 创建用户实体
            User user = createUserFromRegisterRequest(request);
            
            // 6. 保存用户到数据库
            User savedUser = userRepository.save(user);
            
            // 7. 转换为响应DTO
            UserInfoResponse response = convertToUserInfoResponse(savedUser, savedUser.getId());
            
            log.info("用户注册成功，用户ID: {}, 用户名: {}", savedUser.getId(), savedUser.getUsername());
            
            // 8. 异步发送验证邮件（这里暂时跳过，后续实现）
            // emailService.sendVerificationEmail(savedUser);
            
            return response;
            
        } catch (BusinessException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户注册异常", e);
            throw new BusinessException("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录实现
     * 
     * 实现逻辑：
     * 1. 根据登录凭据查找用户
     * 2. 验证密码或验证码
     * 3. 检查账户状态
     * 4. 更新最后登录时间
     * 5. 返回用户信息
     * 
     * @param request 登录请求参数
     * @return 登录用户信息
     * @throws BusinessException 登录失败异常
     */
    @Override
    public UserInfoResponse login(UserLoginRequest request) {
        log.info("开始用户登录，登录方式: {}", request.getLoginMethodDescription());
        
        try {
            // 1. 验证请求参数
            String validationError = request.validateRequest();
            if (validationError != null) {
                throw new BusinessException(validationError);
            }
            
            // 2. 根据登录凭据查找用户
            Optional<User> userOpt = userRepository.findByCredentialAndDeletedFalse(request.getCredential());
            if (userOpt.isEmpty()) {
                throw new BusinessException("用户名、邮箱或手机号不存在");
            }
            
            User user = userOpt.get();
            
            // 3. 检查账户状态
            if (!user.isEnabled()) {
                throw BusinessException.invalidState(user.getStatus().getDisplayName(), "登录");
            }
            
            // 4. 验证密码或验证码
            if (request.isPasswordLogin()) {
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    throw new BusinessException("密码错误");
                }
            } else if (request.isSmsCodeLogin()) {
                // 短信验证码验证逻辑（这里暂时跳过）
                validateSmsCode(user.getPhone(), request.getSmsCode());
            } else if (request.isEmailCodeLogin()) {
                // 邮箱验证码验证逻辑（这里暂时跳过）
                validateEmailCode(user.getEmail(), request.getEmailCode());
            }
            
            // 5. 更新最后登录时间
            user.updateLastLoginTime();
            userRepository.save(user);
            
            // 6. 转换为响应DTO
            UserInfoResponse response = convertToUserInfoResponse(user, user.getId());
            
            log.info("用户登录成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
            
            return response;
            
        } catch (BusinessException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常", e);
            throw new BusinessException("登录失败，请稍后重试");
        }
    }

    /**
     * 用户登出实现
     * 
     * @param userId 用户ID
     * @param token 访问令牌
     * @return 操作结果
     */
    @Override
    public boolean logout(Long userId, String token) {
        log.info("用户登出，用户ID: {}", userId);
        
        try {
            // 1. 验证用户存在性
            if (!userRepository.existsById(userId)) {
                return false;
            }
            
            // 2. 清除访问令牌（这里暂时跳过，后续实现JWT管理）
            // tokenService.invalidateToken(token);
            
            // 3. 清除RememberMe信息（如果有）
            // rememberMeService.clearRememberMe(userId);
            
            log.info("用户登出成功，用户ID: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("用户登出异常，用户ID: {}", userId, e);
            return false;
        }
    }

    /**
     * 刷新访问令牌实现
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @Override
    public String refreshToken(String refreshToken) {
        // JWT令牌刷新逻辑（后续实现）
        throw new BusinessException("令牌刷新功能暂未实现");
    }

    // ========== 用户信息管理实现 ==========

    /**
     * 根据用户ID获取用户信息
     * 
     * @param userId 用户ID
     * @param currentUserId 当前访问用户ID
     * @return 用户信息
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#userId + '_' + #currentUserId")
    public Optional<UserInfoResponse> getUserById(Long userId, Long currentUserId) {
        log.debug("查询用户信息，用户ID: {}, 当前用户ID: {}", userId, currentUserId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        User user = userOpt.get();
        UserInfoResponse response = convertToUserInfoResponse(user, currentUserId);
        
        return Optional.of(response);
    }

    /**
     * 根据用户名获取用户信息
     * 
     * @param username 用户名
     * @param currentUserId 当前访问用户ID
     * @return 用户信息
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "'username_' + #username + '_' + #currentUserId")
    public Optional<UserInfoResponse> getUserByUsername(String username, Long currentUserId) {
        log.debug("根据用户名查询用户信息，用户名: {}", username);
        
        Optional<User> userOpt = userRepository.findByUsernameAndDeletedFalse(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        User user = userOpt.get();
        UserInfoResponse response = convertToUserInfoResponse(user, currentUserId);
        
        return Optional.of(response);
    }

    /**
     * 获取当前登录用户的完整信息
     * 
     * @param userId 当前用户ID
     * @return 当前用户完整信息
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "currentUser", key = "#userId")
    public UserInfoResponse getCurrentUserInfo(Long userId) {
        log.debug("获取当前用户信息，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));
        
        return convertToUserInfoResponse(user, userId);
    }

    /**
     * 更新用户基本信息
     * 
     * @param userId 用户ID
     * @param userInfo 更新信息
     * @return 更新后的用户信息
     */
    @Override
    @CacheEvict(value = {"user", "currentUser"}, allEntries = true)
    public UserInfoResponse updateUserInfo(Long userId, UserInfoResponse userInfo) {
        log.info("更新用户信息，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));
        
        // 更新可修改的字段
        if (StringUtils.hasText(userInfo.getNickname())) {
            user.setNickname(userInfo.getNickname());
        }
        if (StringUtils.hasText(userInfo.getAvatar())) {
            user.setAvatar(userInfo.getAvatar());
        }
        if (StringUtils.hasText(userInfo.getGender())) {
            user.setGender(User.Gender.valueOf(userInfo.getGender()));
        }
        if (userInfo.getBirthday() != null) {
            user.setBirthday(userInfo.getBirthday());
        }
        if (StringUtils.hasText(userInfo.getBio())) {
            user.setBio(userInfo.getBio());
        }
        
        User savedUser = userRepository.save(user);
        
        log.info("用户信息更新成功，用户ID: {}", userId);
        
        return convertToUserInfoResponse(savedUser, userId);
    }

    /**
     * 更新用户头像
     * 
     * @param userId 用户ID
     * @param avatarUrl 新头像URL
     * @return 更新结果
     */
    @Override
    @CacheEvict(value = {"user", "currentUser"}, allEntries = true)
    public boolean updateUserAvatar(Long userId, String avatarUrl) {
        log.info("更新用户头像，用户ID: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> BusinessException.notFound("用户"));
            
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            
            log.info("用户头像更新成功，用户ID: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("用户头像更新失败，用户ID: {}", userId, e);
            return false;
        }
    }

    // ========== 数据验证服务实现 ==========

    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return true-可用，false-已被使用
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameAndDeletedFalse(username);
    }

    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱地址
     * @return true-可用，false-已被使用
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndDeletedFalse(email);
    }

    /**
     * 检查手机号是否可用
     * 
     * @param phone 手机号
     * @return true-可用，false-已被使用
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhoneAndDeletedFalse(phone);
    }

    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @param userId 用户ID
     * @return 密码强度结果
     */
    @Override
    @Transactional(readOnly = true)
    public PasswordStrengthResult validatePasswordStrength(String password, Long userId) {
        if (!StringUtils.hasText(password)) {
            return new PasswordStrengthResult(0, "无效", List.of("密码不能为空"));
        }
        
        List<String> suggestions = new ArrayList<>();
        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) {
            score += 1;
        } else {
            suggestions.add("密码长度至少8个字符");
        }
        
        // 复杂度检查
        if (password.matches(".*[a-z].*")) score += 1;
        else suggestions.add("包含小写字母");
        
        if (password.matches(".*[A-Z].*")) score += 1;
        else suggestions.add("包含大写字母");
        
        if (password.matches(".*\\d.*")) score += 1;
        else suggestions.add("包含数字");
        
        if (password.matches(".*[!@#$%^&*()].*")) score += 1;
        else suggestions.add("包含特殊字符");
        
        // 常见密码检查
        List<String> commonPasswords = Arrays.asList("123456", "password", "123456789", "12345678");
        if (commonPasswords.contains(password.toLowerCase())) {
            score = Math.max(0, score - 2);
            suggestions.add("避免使用常见密码");
        }
        
        String level = switch (score) {
            case 0, 1 -> "弱";
            case 2, 3 -> "中";
            case 4, 5 -> "强";
            default -> "无效";
        };
        
        return new PasswordStrengthResult(score, level, suggestions);
    }

    // ========== 用户查询和搜索实现 ==========

    /**
     * 分页查询用户列表
     * 
     * @param keyword 搜索关键词
     * @param status 用户状态筛选
     * @param role 用户角色筛选
     * @param pageable 分页参数
     * @param currentUserId 当前用户ID
     * @return 用户分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserInfoResponse> searchUsers(String keyword, User.UserStatus status, 
                                            User.UserRole role, Pageable pageable, Long currentUserId) {
        log.debug("搜索用户，关键词: {}, 状态: {}, 角色: {}", keyword, status, role);
        
        Page<User> userPage;
        
        if (StringUtils.hasText(keyword)) {
            userPage = userRepository.advancedSearch(keyword, pageable);
        } else if (status != null && role != null) {
            // 复合条件查询（需要自定义实现或使用Specification）
            userPage = userRepository.findByStatusAndRoleAndDeletedFalse(status, role, pageable);
        } else if (status != null) {
            userPage = userRepository.findByStatusAndDeletedFalse(status, pageable);
        } else if (role != null) {
            userPage = userRepository.findByRoleAndDeletedFalse(role, pageable);
        } else {
            userPage = userRepository.findByDeletedFalse(pageable);
        }
        
        return userPage.map(user -> convertToUserInfoResponse(user, currentUserId));
    }

    /**
     * 获取用户统计信息
     * 
     * @return 用户统计信息
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userStatistics", key = "'all'")
    public UserStatistics getUserStatistics() {
        log.debug("获取用户统计信息");
        
        long totalUsers = userRepository.countByDeletedFalse();
        long activeUsers = userRepository.countByLastLoginAtAfterAndDeletedFalse(
                LocalDateTime.now().minusDays(30));
        long newUsersToday = userRepository.countByCreatedAtAfterAndDeletedFalse(
                LocalDateTime.now().toLocalDate().atStartOfDay());
        long verifiedUsers = userRepository.countByEmailVerifiedTrueAndDeletedFalse();
        
        // 状态统计
        List<StatusCount> statusCounts = Arrays.stream(User.UserStatus.values())
                .map(status -> new StatusCount(status, 
                        userRepository.countByStatusAndDeletedFalse(status)))
                .toList();
        
        // 角色统计
        List<RoleCount> roleCounts = Arrays.stream(User.UserRole.values())
                .map(role -> new RoleCount(role, 
                        userRepository.countByRoleAndDeletedFalse(role)))
                .toList();
        
        return new UserStatistics(totalUsers, activeUsers, newUsersToday, 
                verifiedUsers, statusCounts, roleCounts);
    }

    /**
     * 获取活跃用户列表
     * 
     * @param pageable 分页参数
     * @return 活跃用户列表
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserInfoResponse> getActiveUsers(Pageable pageable) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Page<User> userPage = userRepository.findByLastLoginAtAfterAndDeletedFalse(thirtyDaysAgo, pageable);
        
        return userPage.map(user -> convertToUserInfoResponse(user, null));
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证注册请求参数
     * 
     * @param request 注册请求
     */
    private void validateRegisterRequest(UserRegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("密码和确认密码不一致");
        }
    }

    /**
     * 根据注册请求创建用户实体
     * 
     * @param request 注册请求
     * @return 用户实体
     */
    private User createUserFromRegisterRequest(UserRegisterRequest request) {
        User user = new User();
        
        // 基本信息
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        
        // 状态信息
        user.setStatus(User.UserStatus.ACTIVE);
        user.setRole(User.UserRole.CUSTOMER);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setDeleted(false);
        
        return user;
    }

    /**
     * 将用户实体转换为响应DTO
     * 
     * @param user 用户实体
     * @param currentUserId 当前用户ID
     * @return 用户信息响应DTO
     */
    private UserInfoResponse convertToUserInfoResponse(User user, Long currentUserId) {
        UserInfoResponse.UserInfoResponseBuilder builder = UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .genderDisplay(user.getGender() != null ? user.getGender().getDisplayName() : null)
                .birthday(user.getBirthday())
                .bio(user.getBio())
                .status(user.getStatus().name())
                .statusDisplay(user.getStatus().getDisplayName())
                .role(user.getRole().name())
                .roleDisplay(user.getRole().getDisplayName())
                .enabled(user.isEnabled())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt());
        
        // 计算年龄
        if (user.getBirthday() != null) {
            builder.age(Period.between(user.getBirthday(), LocalDate.now()).getYears());
        }
        
        // 根据权限决定是否返回敏感信息
        boolean isSelf = currentUserId != null && currentUserId.equals(user.getId());
        boolean isAdmin = currentUserId != null && isCurrentUserAdmin(currentUserId);
        
        if (isSelf || isAdmin) {
            // 返回完整信息（可能需要脱敏）
            builder.email(maskEmail(user.getEmail()))
                   .phone(maskPhone(user.getPhone()));
        }
        
        return builder.build();
    }

    /**
     * 检查当前用户是否为管理员
     * 
     * @param userId 用户ID
     * @return 是否为管理员
     */
    private boolean isCurrentUserAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
    }

    /**
     * 邮箱脱敏处理
     * 
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;
        }
        
        String prefix = email.substring(0, 2);
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }

    /**
     * 手机号脱敏处理
     * 
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        
        String prefix = phone.substring(0, 3);
        String suffix = phone.substring(phone.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * 验证短信验证码
     * 
     * @param phone 手机号
     * @param smsCode 短信验证码
     */
    private void validateSmsCode(String phone, String smsCode) {
        // 短信验证码验证逻辑（后续实现）
        log.debug("验证短信验证码，手机号: {}", phone);
        // 暂时抛出异常，提示功能未实现
        throw new BusinessException("短信验证码功能暂未实现");
    }

    /**
     * 验证邮箱验证码
     * 
     * @param email 邮箱地址
     * @param emailCode 邮箱验证码
     */
    private void validateEmailCode(String email, String emailCode) {
        // 邮箱验证码验证逻辑（后续实现）
        log.debug("验证邮箱验证码，邮箱: {}", email);
        // 暂时抛出异常，提示功能未实现
        throw new BusinessException("邮箱验证码功能暂未实现");
    }

    // ========== 暂未完整实现的方法 ==========
    // 以下方法在当前阶段暂时抛出未实现异常，将在后续开发中完善

    @Override
    public boolean updateUserEmail(Long userId, String newEmail, String password, String verificationCode) {
        throw new BusinessException("邮箱更新功能暂未实现");
    }

    @Override
    public boolean updateUserPhone(Long userId, String newPhone, String password, String verificationCode) {
        throw new BusinessException("手机号更新功能暂未实现");
    }

    @Override
    public boolean verifyEmail(Long userId, String verificationCode) {
        throw new BusinessException("邮箱验证功能暂未实现");
    }

    @Override
    public boolean verifyPhone(Long userId, String verificationCode) {
        throw new BusinessException("手机验证功能暂未实现");
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        throw new BusinessException("密码修改功能暂未实现");
    }

    @Override
    public boolean resetPassword(String credential, String verificationCode, String newPassword) {
        throw new BusinessException("密码重置功能暂未实现");
    }

    @Override
    public boolean lockUser(Long userId, String reason, Long operatorId) {
        throw new BusinessException("用户锁定功能暂未实现");
    }

    @Override
    public boolean unlockUser(Long userId, Long operatorId) {
        throw new BusinessException("用户解锁功能暂未实现");
    }

    @Override
    public boolean batchUpdateUserStatus(List<Long> userIds, User.UserStatus status, Long operatorId) {
        throw new BusinessException("批量状态更新功能暂未实现");
    }

    @Override
    public boolean deleteUser(Long userId, Long operatorId) {
        throw new BusinessException("用户删除功能暂未实现");
    }

    @Override
    public boolean restoreUser(Long userId, Long operatorId) {
        throw new BusinessException("用户恢复功能暂未实现");
    }
}