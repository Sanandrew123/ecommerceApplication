/*
文件级分析：
- 职责：定义用户状态枚举，规范用户在系统中的各种状态
- 包结构考虑：位于enums包下，与其他业务枚举统一管理
- 命名原因：UserStatus清晰表明这是用户状态枚举
- 调用关系：被User实体类使用，在业务逻辑中进行状态判断和流转

设计思路：
1. 使用枚举而不是常量，提供类型安全性
2. 每个状态包含code和description，便于存储和显示
3. 提供便捷的查询方法，简化业务代码
4. 考虑用户生命周期中的各种状态
*/
package com.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 用户状态枚举
 * 
 * 定义用户在系统中的各种状态，包括：
 * - ACTIVE: 活跃状态，正常使用系统功能
 * - INACTIVE: 非活跃状态，可能长时间未登录
 * - DISABLED: 禁用状态，管理员手动禁用
 * - LOCKED: 锁定状态，可能因为安全原因被锁定
 * - PENDING: 待审核状态，注册后等待审核
 * 
 * 状态流转规则：
 * PENDING -> ACTIVE (审核通过)
 * ACTIVE -> INACTIVE (长时间未活跃)
 * ACTIVE -> LOCKED (安全策略触发)
 * ACTIVE -> DISABLED (管理员操作)
 * LOCKED -> ACTIVE (解锁操作)
 * DISABLED -> ACTIVE (管理员重新启用)
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    
    /**
     * 活跃状态
     * 用户正常使用系统，可以进行所有业务操作
     */
    ACTIVE(1, "活跃"),
    
    /**
     * 非活跃状态
     * 用户长时间未登录或使用系统，但账户仍然有效
     * 可以正常登录，但可能受到一些功能限制
     */
    INACTIVE(2, "非活跃"),
    
    /**
     * 禁用状态
     * 管理员手动禁用的用户，无法登录系统
     * 通常用于违规用户或需要暂停服务的情况
     */
    DISABLED(3, "禁用"),
    
    /**
     * 锁定状态
     * 因安全原因被系统自动锁定，如多次登录失败
     * 需要特定的解锁操作才能恢复正常
     */
    LOCKED(4, "锁定"),
    
    /**
     * 待审核状态
     * 新注册用户的初始状态，需要管理员审核后才能正常使用
     * 适用于需要实名认证或人工审核的业务场景
     */
    PENDING(5, "待审核");
    
    /** 状态码 */
    private final Integer code;
    
    /** 状态描述 */
    private final String description;
    
    /**
     * 根据状态码获取用户状态枚举
     * 
     * @param code 状态码
     * @return 对应的用户状态枚举，如果找不到则返回null
     */
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(UserStatus.values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据状态描述获取用户状态枚举
     * 
     * @param description 状态描述
     * @return 对应的用户状态枚举，如果找不到则返回null
     */
    public static UserStatus fromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(UserStatus.values())
                .filter(status -> status.getDescription().equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 判断是否为活跃状态
     * 
     * @return true表示活跃，false表示非活跃
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * 判断是否为可登录状态
     * 活跃和非活跃状态都可以登录
     * 
     * @return true表示可以登录，false表示不能登录
     */
    public boolean canLogin() {
        return this == ACTIVE || this == INACTIVE;
    }
    
    /**
     * 判断是否为被禁用状态
     * 包括禁用和锁定状态
     * 
     * @return true表示被禁用，false表示未被禁用
     */
    public boolean isDisabled() {
        return this == DISABLED || this == LOCKED;
    }
    
    /**
     * 判断是否需要审核
     * 
     * @return true表示需要审核，false表示不需要审核
     */
    public boolean isPending() {
        return this == PENDING;
    }
}