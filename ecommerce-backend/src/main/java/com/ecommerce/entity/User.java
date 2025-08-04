package com.ecommerce.entity;

/*
 * 文件职责: 用户实体类，定义用户的数据模型和数据库映射
 * 
 * 开发心理活动：
 * 1. 为什么先开发实体类？
 *    - 实体类是业务的核心数据模型，定义了数据结构基础
 *    - 其他层(Repository、Service、Controller)都依赖实体类
 *    - 先定义数据模型，有助于理清业务逻辑和数据关系
 * 
 * 2. 用户实体设计考虑：
 *    - 基础信息：用户名、邮箱、手机号、密码
 *    - 个人信息：昵称、头像、性别、生日
 *    - 系统信息：状态、角色、创建时间、更新时间
 *    - 安全考虑：密码加密存储、敏感信息保护
 * 
 * 3. JPA注解选择：
 *    - @Entity：标识为JPA实体
 *    - @Table：指定表名和索引
 *    - @Id、@GeneratedValue：主键策略
 *    - @Column：字段映射和约束
 *    - @Enumerated：枚举类型映射
 *    - @CreationTimestamp、@UpdateTimestamp：审计字段
 * 
 * 4. 扩展性设计：
 *    - 预留扩展字段，支持后续功能增加
 *    - 支持软删除，保护历史数据
 *    - 支持多角色系统，便于权限管理
 * 
 * 包结构设计思路:
 * - 放在entity包下，与其他实体类统一管理
 * - 作为用户模块的核心数据模型
 * 
 * 命名原因:
 * - User简洁明了，符合领域模型命名
 * - 避免与Java关键字冲突
 * 
 * 依赖关系:
 * - 被Repository层引用，进行数据访问
 * - 被Service层使用，进行业务逻辑处理
 * - 通过DTO转换，向Controller层提供数据
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体类
 * 
 * 业务模型说明：
 * 1. 用户基础信息管理
 * 2. 认证授权数据存储
 * 3. 个人资料维护
 * 4. 用户状态跟踪
 * 
 * 数据库设计考虑：
 * 1. 主键策略：使用自增ID，简单高效
 * 2. 唯一约束：用户名、邮箱、手机号保证唯一性
 * 3. 索引优化：常用查询字段添加索引
 * 4. 字段长度：根据实际业务需求设置合理长度
 * 5. 默认值：状态字段设置合理默认值
 * 
 * 安全设计：
 * 1. 密码字段标记@JsonIgnore，防止序列化泄露
 * 2. 实现UserDetails接口，集成Spring Security
 * 3. 支持账户状态管理(锁定、过期等)
 * 4. 预留安全相关字段(最后登录时间等)
 * 
 * 扩展性设计：
 * 1. 软删除支持，保护历史数据
 * 2. 审计字段，跟踪数据变更
 * 3. 角色枚举，支持权限扩展
 * 4. 预留JSON字段，支持动态属性
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "users", 
       indexes = {
           @Index(name = "idx_username", columnList = "username"),
           @Index(name = "idx_email", columnList = "email"),
           @Index(name = "idx_phone", columnList = "phone"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class User implements UserDetails {

    // ========== 主键和基础信息 ==========
    
    /**
     * 用户ID - 主键
     * 使用自增策略，简单高效
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 用户名
     * 用于登录和显示，全局唯一
     * 长度限制：3-20个字符
     * 字符限制：字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    /**
     * 密码
     * 加密存储，不参与JSON序列化
     * 原始密码长度：6-20个字符
     * 存储加密后的密码，长度可达255
     */
    @JsonIgnore
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    /**
     * 邮箱地址
     * 用于登录、找回密码、接收通知
     * 全局唯一，必须符合邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    /**
     * 手机号码
     * 用于登录、短信验证、订单通知
     * 全局唯一，支持中国大陆手机号格式
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    // ========== 个人信息 ==========

    /**
     * 昵称/显示名称
     * 用于前端显示，可以重复
     * 如果为空，使用用户名作为显示名称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 真实姓名
     * 用于实名认证、订单配送等
     * 可选字段，涉及隐私保护
     */
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 头像URL
     * 存储头像图片的访问路径
     * 可以是相对路径或完整URL
     */
    @Column(name = "avatar", length = 500)
    private String avatar;

    /**
     * 性别
     * 使用枚举类型，提供固定选项
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender = Gender.UNKNOWN;

    /**
     * 生日
     * 用于年龄计算、生日提醒等功能
     */
    @Column(name = "birthday")
    private LocalDate birthday;

    /**
     * 个人简介
     * 用户自我介绍，支持较长文本
     */
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    @Column(name = "bio", length = 500)
    private String bio;

    // ========== 系统信息 ==========

    /**
     * 用户状态
     * 控制用户的系统访问权限
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 用户角色
     * 控制用户的功能权限
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    /**
     * 邮箱验证状态
     * 标识用户邮箱是否已验证
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * 手机验证状态
     * 标识用户手机号是否已验证
     */
    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    // ========== 审计信息 ==========

    /**
     * 创建时间
     * 自动设置，记录用户注册时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 自动更新，记录最后修改时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     * 手动设置，用于用户活跃度分析
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 软删除标记
     * true表示已删除，false表示正常
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    // ========== Spring Security UserDetails 接口实现 ==========

    /**
     * 获取用户权限集合
     * 基于用户角色返回权限列表
     * 
     * @return 权限集合
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    /**
     * 获取用户名（用于Spring Security认证）
     * 
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账户是否未过期
     * 
     * @return true-未过期，false-已过期
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return status != UserStatus.EXPIRED;
    }

    /**
     * 账户是否未锁定
     * 
     * @return true-未锁定，false-已锁定
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    /**
     * 凭据是否未过期
     * 
     * @return true-未过期，false-已过期
     */
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return status != UserStatus.PASSWORD_EXPIRED;
    }

    /**
     * 账户是否启用
     * 
     * @return true-启用，false-禁用
     */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && !deleted;
    }

    // ========== 枚举定义 ==========

    /**
     * 性别枚举
     */
    public enum Gender {
        /**
         * 男性
         */
        MALE("男"),
        
        /**
         * 女性
         */
        FEMALE("女"),
        
        /**
         * 未知/不愿透露
         */
        UNKNOWN("未知");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        /**
         * 正常状态
         */
        ACTIVE("正常"),
        
        /**
         * 未激活状态
         */
        INACTIVE("未激活"),
        
        /**
         * 锁定状态
         */
        LOCKED("已锁定"),
        
        /**
         * 已过期
         */
        EXPIRED("已过期"),
        
        /**
         * 密码过期
         */
        PASSWORD_EXPIRED("密码过期");

        private final String displayName;

        UserStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        /**
         * 超级管理员
         */
        SUPER_ADMIN("超级管理员"),
        
        /**
         * 管理员
         */
        ADMIN("管理员"),
        
        /**
         * 商家
         */
        MERCHANT("商家"),
        
        /**
         * 普通客户
         */
        CUSTOMER("普通客户");

        private final String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 获取显示名称
     * 优先使用昵称，没有昵称则使用用户名
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }

    /**
     * 是否为管理员
     * 
     * @return true-是管理员，false-不是管理员
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    /**
     * 是否为商家
     * 
     * @return true-是商家，false-不是商家
     */
    public boolean isMerchant() {
        return role == UserRole.MERCHANT;
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }
}