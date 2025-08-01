/*
文件级分析：
- 职责：提供所有业务实体的公共字段和行为，实现统一的数据管理
- 包结构考虑：放在entity.base包下，作为实体类的基础抽象
- 命名原因：BaseEntity明确表示这是实体类的基类
- 调用关系：被所有业务实体类继承，提供统一的数据库审计功能

设计思路：
1. 包含所有实体的公共字段：id、创建时间、更新时间、删除标记等
2. 使用JPA审计功能自动管理时间字段
3. 实现软删除机制，提高数据安全性
4. 提供统一的equals和hashCode实现
5. 使用@MappedSuperclass注解，表示这是一个映射的父类
*/
package com.ecommerce.entity.base;

import com.ecommerce.constants.CommonConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 * 
 * 所有业务实体类的基类，提供通用的数据库字段和行为：
 * 1. 主键ID：使用雪花算法生成全局唯一ID
 * 2. 审计字段：创建时间、更新时间（由JPA自动管理）
 * 3. 软删除：删除标记，逻辑删除而非物理删除
 * 4. 版本控制：乐观锁字段，防止并发更新冲突
 * 
 * 设计原则：
 * - 所有实体都应该继承此基类，确保数据管理的一致性
 * - 使用逻辑删除，保证数据的完整性和可恢复性
 * - 启用JPA审计，自动记录数据变更时间
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     * 使用数据库自增策略，在MySQL等数据库中会自动生成
     * 在分布式环境下可以考虑使用雪花算法等分布式ID生成策略
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    
    /**
     * 创建时间
     * 使用JPA审计功能自动填充，实体首次持久化时设置
     * 一旦设置后不可更改（updatable = false）
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = CommonConstants.DEFAULT_DATETIME_FORMAT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * 使用JPA审计功能自动维护，每次实体更新时都会自动更新此字段
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = CommonConstants.DEFAULT_DATETIME_FORMAT)
    private LocalDateTime updatedAt;
    
    /**
     * 删除标记
     * 实现软删除机制：
     * - 0: 未删除（默认值）
     * - 1: 已删除
     * 
     * 软删除的优势：
     * 1. 数据安全：避免误删除造成的数据丢失
     * 2. 数据恢复：可以轻松恢复被删除的数据
     * 3. 审计跟踪：保留完整的数据变更历史
     * 4. 外键完整性：避免物理删除导致的外键约束问题
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted = CommonConstants.DELETED_FLAG_FALSE;
    
    /**
     * 版本号
     * 用于乐观锁控制，防止并发更新时的数据冲突
     * JPA会在每次更新时自动递增此字段
     * 
     * 乐观锁机制：
     * 1. 读取数据时获取版本号
     * 2. 更新时检查版本号是否一致
     * 3. 如果版本号不一致，说明数据已被其他线程修改，抛出异常
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    /**
     * 创建者ID
     * 记录是谁创建了这条记录，便于审计和权限控制
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    /**
     * 更新者ID
     * 记录是谁最后更新了这条记录
     */
    @Column(name = "updated_by")
    private Long updatedBy;
    
    // ======================== 业务方法 ========================
    
    /**
     * 标记为已删除
     * 执行软删除操作，将deleted字段设置为1
     */
    public void markAsDeleted() {
        this.deleted = CommonConstants.DELETED_FLAG_TRUE;
    }
    
    /**
     * 恢复删除状态
     * 将deleted字段设置为0，恢复数据
     */
    public void unmarkDeleted() {
        this.deleted = CommonConstants.DELETED_FLAG_FALSE;
    }
    
    /**
     * 判断是否已删除
     * 
     * @return true表示已删除，false表示未删除
     */
    public boolean isDeleted() {
        return Integer.valueOf(CommonConstants.DELETED_FLAG_TRUE).equals(this.deleted);
    }
    
    /**
     * 判断是否为新实体
     * 通过ID是否为null来判断
     * 
     * @return true表示新实体，false表示已持久化的实体
     */
    public boolean isNew() {
        return this.id == null;
    }
    
    // ======================== JPA生命周期回调 ========================
    
    /**
     * 持久化前的回调方法
     * 在实体被保存到数据库之前执行
     */
    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.deleted == null) {
            this.deleted = CommonConstants.DELETED_FLAG_FALSE;
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }
    
    /**
     * 更新前的回调方法
     * 在实体被更新之前执行
     */
    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}