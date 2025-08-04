package com.ecommerce.service;

/*
 * 文件职责: 用户服务接口，定义用户相关的业务逻辑方法
 * 
 * 开发心理活动：
 * 1. Service接口设计原则：
 *    - 面向业务的方法定义，而不是数据操作
 *    - 接口与实现分离，便于测试和扩展
 *    - 方法命名体现业务语义，提高可读性
 *    - 统一异常处理和返回值格式
 * 
 * 2. 业务方法分类：
 *    - 用户认证：注册、登录、登出
 *    - 用户管理：信息查询、更新、状态管理
 *    - 数据验证：唯一性检查、格式验证
 *    - 安全功能：密码修改、账户锁定
 * 
 * 3. 事务考虑：
 *    - 复杂业务操作需要事务支持
 *    - 读写分离的查询优化
 *    - 批量操作的事务边界
 *    - 异常回滚策略
 * 
 * 4. 扩展性设计：
 *    - 预留缓存接口方法
 *    - 支持多种认证方式
 *    - 兼容第三方登录
 *    - 支持用户数据导入导出
 * 
 * 包结构设计思路:
 * - 放在service包下，作为业务逻辑层的顶层接口
 * - 与impl实现包分离，遵循接口隔离原则
 * 
 * 命名原因:
 * - UserService明确表达用户业务服务功能
 * - 符合Service后缀的服务层命名规范
 * 
 * 依赖关系:
 * - 被Controller层调用，处理业务请求
 * - 实现类依赖Repository层进行数据访问
 * - 与DTO对象交互，处理数据转换
 */

import com.ecommerce.dto.request.user.UserLoginRequest;
import com.ecommerce.dto.request.user.UserRegisterRequest;
import com.ecommerce.dto.response.user.UserInfoResponse;
import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 
 * 功能说明：
 * 1. 定义用户相关的核心业务逻辑
 * 2. 提供统一的业务方法签名
 * 3. 规范异常处理和返回值格式
 * 4. 支持业务逻辑的扩展和演进
 * 
 * 服务分类：
 * 1. 认证服务：注册、登录、登出
 * 2. 用户管理：查询、更新、删除
 * 3. 验证服务：唯一性、格式验证
 * 4. 安全服务：密码、权限管理
 * 5. 统计服务：用户数据分析
 * 
 * 设计模式：
 * 1. 接口隔离：按功能模块分组方法
 * 2. 单一职责：每个方法专注单一业务
 * 3. 依赖倒置：面向接口编程
 * 4. 开闭原则：易扩展，少修改
 * 
 * 事务策略：
 * 1. 查询方法：只读事务，提升性能
 * 2. 修改方法：读写事务，保证一致性
 * 3. 批量操作：合理的事务边界
 * 4. 异常回滚：业务异常自动回滚
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
public interface UserService {

    // ========== 用户认证服务 ==========

    /**
     * 用户注册
     * 
     * 业务流程：
     * 1. 参数验证（用户名、邮箱、手机号唯一性）
     * 2. 密码加密处理
     * 3. 创建用户记录
     * 4. 发送验证邮件（异步）
     * 5. 返回用户信息
     * 
     * 异常情况：
     * - 用户名已存在
     * - 邮箱已被注册
     * - 手机号已被使用
     * - 邀请码无效
     * - 验证码错误
     * 
     * @param request 注册请求参数
     * @return 新注册用户信息
     * @throws com.ecommerce.exception.BusinessException 业务异常
     */
    UserInfoResponse register(UserRegisterRequest request);

    /**
     * 用户登录
     * 
     * 业务流程：
     * 1. 根据登录凭据查找用户
     * 2. 验证密码或验证码
     * 3. 检查账户状态和权限
     * 4. 更新最后登录时间
     * 5. 生成访问令牌
     * 6. 记录登录日志
     * 
     * 支持登录方式：
     * - 用户名 + 密码
     * - 邮箱 + 密码
     * - 手机号 + 密码
     * - 手机号 + 短信验证码
     * 
     * @param request 登录请求参数
     * @return 登录成功的用户信息和令牌
     * @throws com.ecommerce.exception.BusinessException 登录失败异常
     */
    UserInfoResponse login(UserLoginRequest request);

    /**
     * 用户登出
     * 
     * 业务流程：
     * 1. 验证当前用户身份
     * 2. 清除访问令牌
     * 3. 清除RememberMe信息
     * 4. 记录登出日志
     * 
     * @param userId 用户ID
     * @param token 访问令牌
     * @return 操作结果
     */
    boolean logout(Long userId, String token);

    /**
     * 刷新访问令牌
     * 
     * 用途：
     * - 延长用户会话时间
     * - 无感知的令牌续期
     * - 提升用户体验
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌信息
     */
    String refreshToken(String refreshToken);

    // ========== 用户信息管理 ==========

    /**
     * 根据用户ID获取用户信息
     * 
     * 权限控制：
     * - 用户只能查看自己的完整信息
     * - 管理员可以查看所有用户信息
     * - 其他用户只能查看公开信息
     * 
     * @param userId 用户ID
     * @param currentUserId 当前访问用户ID
     * @return 用户信息（根据权限过滤）
     */
    Optional<UserInfoResponse> getUserById(Long userId, Long currentUserId);

    /**
     * 根据用户名获取用户信息
     * 
     * @param username 用户名
     * @param currentUserId 当前访问用户ID
     * @return 用户信息
     */
    Optional<UserInfoResponse> getUserByUsername(String username, Long currentUserId);

    /**
     * 获取当前登录用户的完整信息
     * 
     * @param userId 当前用户ID
     * @return 当前用户完整信息
     */
    UserInfoResponse getCurrentUserInfo(Long userId);

    /**
     * 更新用户基本信息
     * 
     * 可更新字段：
     * - 昵称、头像、性别、生日
     * - 个人简介
     * - 隐私设置
     * 
     * 不可更新字段：
     * - 用户名、邮箱、手机号（需要单独验证）
     * - 状态、角色（需要管理员权限）
     * 
     * @param userId 用户ID
     * @param userInfo 更新信息
     * @return 更新后的用户信息
     */
    UserInfoResponse updateUserInfo(Long userId, UserInfoResponse userInfo);

    /**
     * 更新用户头像
     * 
     * 业务流程：
     * 1. 验证图片格式和大小
     * 2. 上传图片到文件存储
     * 3. 生成访问URL
     * 4. 更新用户头像字段
     * 5. 删除旧头像文件
     * 
     * @param userId 用户ID
     * @param avatarUrl 新头像URL
     * @return 更新结果
     */
    boolean updateUserAvatar(Long userId, String avatarUrl);

    // ========== 联系方式管理 ==========

    /**
     * 更新用户邮箱
     * 
     * 安全流程：
     * 1. 验证当前密码
     * 2. 检查新邮箱唯一性
     * 3. 发送验证码到新邮箱
     * 4. 验证码确认后更新
     * 5. 重置邮箱验证状态
     * 
     * @param userId 用户ID
     * @param newEmail 新邮箱地址
     * @param password 当前密码
     * @param verificationCode 邮箱验证码
     * @return 更新结果
     */
    boolean updateUserEmail(Long userId, String newEmail, String password, String verificationCode);

    /**
     * 更新用户手机号
     * 
     * @param userId 用户ID
     * @param newPhone 新手机号
     * @param password 当前密码
     * @param verificationCode 短信验证码
     * @return 更新结果
     */
    boolean updateUserPhone(Long userId, String newPhone, String password, String verificationCode);

    /**
     * 验证用户邮箱
     * 
     * @param userId 用户ID
     * @param verificationCode 邮箱验证码
     * @return 验证结果
     */
    boolean verifyEmail(Long userId, String verificationCode);

    /**
     * 验证用户手机号
     * 
     * @param userId 用户ID
     * @param verificationCode 短信验证码
     * @return 验证结果
     */
    boolean verifyPhone(Long userId, String verificationCode);

    // ========== 密码和安全管理 ==========

    /**
     * 修改用户密码
     * 
     * 安全验证：
     * 1. 验证当前密码
     * 2. 新密码强度检查
     * 3. 新旧密码不能相同
     * 4. 密码加密存储
     * 5. 清除所有登录会话
     * 
     * @param userId 用户ID
     * @param oldPassword 当前密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置用户密码（忘记密码）
     * 
     * 验证流程：
     * 1. 通过邮箱或手机号找回
     * 2. 发送重置验证码
     * 3. 验证码确认身份
     * 4. 设置新密码
     * 5. 清除所有登录会话
     * 
     * @param credential 找回凭据（邮箱或手机号）
     * @param verificationCode 验证码
     * @param newPassword 新密码
     * @return 重置结果
     */
    boolean resetPassword(String credential, String verificationCode, String newPassword);

    /**
     * 锁定用户账户
     * 
     * 使用场景：
     * - 安全风险账户
     * - 违规行为处罚
     * - 临时冻结账户
     * 
     * @param userId 用户ID
     * @param reason 锁定原因
     * @param operatorId 操作员ID
     * @return 锁定结果
     */
    boolean lockUser(Long userId, String reason, Long operatorId);

    /**
     * 解锁用户账户
     * 
     * @param userId 用户ID
     * @param operatorId 操作员ID
     * @return 解锁结果
     */
    boolean unlockUser(Long userId, Long operatorId);

    // ========== 数据验证服务 ==========

    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return true-可用，false-已被使用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱地址
     * @return true-可用，false-已被使用
     */
    boolean isEmailAvailable(String email);

    /**
     * 检查手机号是否可用
     * 
     * @param phone 手机号
     * @return true-可用，false-已被使用
     */
    boolean isPhoneAvailable(String phone);

    /**
     * 验证密码强度
     * 
     * 检查项目：
     * - 长度要求
     * - 复杂度要求
     * - 常见密码检查
     * - 与用户信息相关性检查
     * 
     * @param password 密码
     * @param userId 用户ID（可选，用于相关性检查）
     * @return 密码强度评分和建议
     */
    PasswordStrengthResult validatePasswordStrength(String password, Long userId);

    // ========== 用户查询和搜索 ==========

    /**
     * 分页查询用户列表
     * 
     * 权限控制：
     * - 普通用户只能搜索公开信息
     * - 管理员可以查看完整信息
     * - 支持多条件筛选
     * 
     * @param keyword 搜索关键词
     * @param status 用户状态筛选
     * @param role 用户角色筛选
     * @param pageable 分页参数
     * @param currentUserId 当前用户ID
     * @return 用户分页结果
     */
    Page<UserInfoResponse> searchUsers(String keyword, User.UserStatus status, 
                                     User.UserRole role, Pageable pageable, Long currentUserId);

    /**
     * 获取用户统计信息
     * 
     * 统计维度：
     * - 总用户数
     * - 各状态用户数
     * - 各角色用户数
     * - 注册趋势
     * - 活跃度统计
     * 
     * @return 用户统计信息
     */
    UserStatistics getUserStatistics();

    /**
     * 获取活跃用户列表
     * 
     * 活跃标准：
     * - 最近30天有登录记录
     * - 排除锁定和禁用用户
     * 
     * @param pageable 分页参数
     * @return 活跃用户列表
     */
    Page<UserInfoResponse> getActiveUsers(Pageable pageable);

    // ========== 管理员功能 ==========

    /**
     * 批量更新用户状态
     * 
     * 权限要求：管理员
     * 
     * @param userIds 用户ID列表
     * @param status 新状态
     * @param operatorId 操作员ID
     * @return 更新结果
     */
    boolean batchUpdateUserStatus(List<Long> userIds, User.UserStatus status, Long operatorId);

    /**
     * 软删除用户
     * 
     * 删除策略：
     * - 标记为已删除，不物理删除
     * - 保留历史数据用于审计
     * - 清除敏感信息
     * 
     * @param userId 用户ID
     * @param operatorId 操作员ID
     * @return 删除结果
     */
    boolean deleteUser(Long userId, Long operatorId);

    /**
     * 恢复已删除用户
     * 
     * @param userId 用户ID
     * @param operatorId 操作员ID
     * @return 恢复结果
     */
    boolean restoreUser(Long userId, Long operatorId);

    // ========== 数据传输对象定义 ==========

    /**
     * 密码强度验证结果
     */
    record PasswordStrengthResult(
        int score,              // 强度评分(1-5)
        String level,           // 强度等级(弱/中/强)
        List<String> suggestions // 改进建议
    ) {}

    /**
     * 用户统计信息
     */
    record UserStatistics(
        long totalUsers,        // 总用户数
        long activeUsers,       // 活跃用户数
        long newUsersToday,     // 今日新增
        long verifiedUsers,     // 已验证用户数
        List<StatusCount> statusCounts,  // 各状态统计
        List<RoleCount> roleCounts       // 各角色统计
    ) {}

    /**
     * 状态统计
     */
    record StatusCount(
        User.UserStatus status,
        long count
    ) {}

    /**
     * 角色统计
     */
    record RoleCount(
        User.UserRole role,
        long count
    ) {}
}