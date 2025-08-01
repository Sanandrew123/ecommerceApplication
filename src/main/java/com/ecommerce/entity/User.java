/*
文件级分析：
- 职责：用户实体类，映射数据库中的用户表，包含用户的基本信息和状态
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：User是领域模型中的核心概念，命名简洁明确
- 调用关系：被UserRepository操作，被UserService使用，与其他业务实体关联

设计思路：
1. 继承BaseEntity，获得通用的审计字段和软删除功能
2. 支持多种登录方式：用户名、邮箱、手机号
3. 密码使用加密存储，不直接暴露
4. 包含用户状态管理，支持账户的生命周期管理
5. 设计用户角色关联，为权限控制做准备
6. 考虑用户画像和扩展信息的存储
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.ecommerce.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体类
 * 
 * 系统中的核心实体，代表使用系统的用户。包含：
 * 1. 基本信息：用户名、邮箱、手机号、昵称等
 * 2. 认证信息：密码（加密存储）
 * 3. 状态信息：用户状态、最后登录时间等
 * 4. 扩展信息：头像、性别、生日、个人简介等
 * 
 * 设计考虑：
 * - 实现UserDetails接口，与Spring Security集成
 * - 支持多种登录方式，提高用户体验
 * - 密码字段使用@JsonIgnore，防止序列化泄露
 * - 包含丰富的用户画像信息，为个性化推荐提供基础
 * 
 * 数据库映射：
 * - 表名：users（避免与数据库关键字user冲突）
 * - 索引：username、email、phone上建立唯一索引
 * - 外键：后续可能关联用户角色、地址等表
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"password"}) // 排除密码字段，防止日志泄露
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_phone", columnList = "phone", unique = true),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class User extends BaseEntity implements UserDetails {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 用户名
     * 用于登录的唯一标识，4-20位字母数字下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名只能包含字母、数字和下划线")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 邮箱地址
     * 用于登录和接收通知，必须唯一
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 手机号码
     * 用于登录和接收短信通知，可选但建议填写
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(name = "phone", unique = true, length = 20)
    private String phone;
    
    /**
     * 用户昵称
     * 用于显示的友好名称，可以包含中文
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    /**
     * 真实姓名
     * 用于实名认证和订单配送
     */
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    @Column(name = "real_name", length = 50)
    private String realName;
    
    // ======================== 认证信息字段 ========================
    
    /**
     * 密码（加密存储）
     * 使用BCrypt等安全哈希算法加密，永远不直接存储明文密码
     * 使用@JsonIgnore防止序列化时泄露
     */
    @JsonIgnore
    @NotBlank(message = "密码不能为空")
    @Column(name = "password", nullable = false)
    private String password;
    
    /**
     * 用户状态
     * 使用枚举映射，便于状态管理和业务逻辑处理
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    
    /**
     * 邮箱是否已验证
     * 新注册用户需要验证邮箱后才能使用完整功能
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    /**
     * 手机号是否已验证
     * 手机号验证后可以使用短信找回密码等功能
     */
    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 头像URL
     * 存储用户头像的访问地址
     */
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    /**
     * 性别
     * 0-未知，1-男，2-女
     */
    @Column(name = "gender")
    private Integer gender = 0;
    
    /**
     * 生日
     * 用于年龄计算和生日营销
     */
    @Column(name = "birthday")
    private LocalDate birthday;
    
    /**
     * 个人简介
     * 用户自我介绍，支持较长文本
     */
    @Column(name = "bio", length = 500)
    private String bio;
    
    // ======================== 统计信息字段 ========================
    
    /**
     * 最后登录时间
     * 用于用户活跃度分析和安全审计
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 最后登录IP
     * 用于安全审计和异常登录检测
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
    
    /**
     * 登录失败次数
     * 用于账户安全策略，达到一定次数后锁定账户
     */
    @Column(name = "login_failure_count", nullable = false)
    private Integer loginFailureCount = 0;
    
    /**
     * 账户锁定时间
     * 如果因为安全原因被锁定，记录锁定的截止时间
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // ======================== UserDetails接口实现 ========================
    
    /**
     * 返回用户的权限集合
     * 当前简化实现，后续可以从用户角色中获取权限
     * 
     * @return 权限集合
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 简化实现，后续可以从用户角色表中查询具体权限
        return Collections.emptyList();
    }
    
    /**
     * 返回用户密码
     * 
     * @return 加密后的密码
     */
    @Override
    @JsonIgnore
    public String getPassword() {
        return this.password;
    }
    
    /**
     * 返回用户名
     * 
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return this.username;
    }
    
    /**
     * 账户是否未过期
     * 
     * @return true表示账户未过期
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true; // 简化实现，不考虑账户过期
    }
    
    /**
     * 账户是否未锁定
     * 
     * @return true表示账户未锁定
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        // 检查是否处于锁定状态，以及锁定时间是否已过
        if (this.status == UserStatus.LOCKED) {
            return this.lockedUntil == null || LocalDateTime.now().isAfter(this.lockedUntil);
        }
        return true;
    }
    
    /**
     * 凭证（密码）是否未过期
     * 
     * @return true表示凭证未过期
     */
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true; // 简化实现，不考虑密码过期
    }
    
    /**
     * 账户是否启用
     * 
     * @return true表示账户启用
     */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return this.status != null && this.status.canLogin();
    }
    
    // ======================== 业务方法 ========================
    
    /**
     * 更新最后登录信息
     * 
     * @param loginIp 登录IP地址
     */
    public void updateLastLogin(String loginIp) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = loginIp;
        this.loginFailureCount = 0; // 登录成功后重置失败次数
    }
    
    /**
     * 增加登录失败次数
     * 
     * @return 当前失败次数
     */
    public int incrementLoginFailureCount() {
        this.loginFailureCount = this.loginFailureCount == null ? 1 : this.loginFailureCount + 1;
        return this.loginFailureCount;
    }
    
    /**
     * 锁定账户
     * 
     * @param lockDuration 锁定时长（分钟）
     */
    public void lockAccount(int lockDuration) {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockDuration);
    }
    
    /**
     * 解锁账户
     */
    public void unlockAccount() {
        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
            this.lockedUntil = null;
            this.loginFailureCount = 0;
        }
    }
    
    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }
    
    /**
     * 验证手机号
     */
    public void verifyPhone() {
        this.phoneVerified = true;
    }
    
    /**
     * 获取显示名称
     * 优先使用昵称，其次使用真实姓名，最后使用用户名
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname.trim();
        }
        if (realName != null && !realName.trim().isEmpty()) {
            return realName.trim();
        }
        return username;
    }
    
    /**
     * 判断是否为新用户
     * 注册时间在7天内的用户视为新用户
     * 
     * @return true表示新用户
     */
    public boolean isNewUser() {
        return getCreatedAt() != null && 
               getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
    }
}