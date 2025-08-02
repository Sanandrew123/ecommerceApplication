/*
文件级分析：
- 职责：用户数据访问层，提供用户实体的数据库操作接口
- 包结构考虑：位于repository包下，统一管理数据访问层
- 命名原因：UserRepository明确表示这是用户的数据仓库
- 调用关系：被Service层调用，继承JpaRepository获得基础CRUD功能

设计思路：
1. 继承JpaRepository，获得基础的CRUD操作
2. 定义自定义查询方法，支持多种查询条件
3. 使用@Query注解编写复杂查询，提高查询效率
4. 支持软删除查询，过滤已删除的数据
5. 提供统计和分析相关的查询方法
6. 考虑性能优化，使用索引和分页查询
*/
package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 * 
 * 提供用户实体的数据库操作接口，包括：
 * 1. 基础CRUD操作（继承自JpaRepository）
 * 2. 多种查询条件的用户查找
 * 3. 用户状态和统计相关查询
 * 4. 软删除支持
 * 5. 性能优化的批量操作
 * 
 * 查询性能考虑：
 * - 在username、email、phone字段上建立唯一索引
 * - 在status、created_at字段上建立普通索引
 * - 使用@Query注解优化复杂查询
 * - 支持分页查询避免数据量过大的问题
 * 
 * 软删除处理：
 * - 所有查询方法都会自动过滤deleted=1的记录  
 * - 提供包含已删除记录的查询方法用于管理后台
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ======================== 基础查询方法 ========================
    
    /**
     * 根据ID查找用户（排除已删除）
     * 
     * @param id 用户ID
     * @return 用户实体，如果不存在则返回空
     */
    Optional<User> findByIdAndDeletedFalse(Long id);
    
    /**
     * 检查用户名是否存在（排除已删除）
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsernameAndDeletedFalse(String username);
    
    /**
     * 检查邮箱是否存在（排除已删除）
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmailAndDeletedFalse(String email);
    
    /**
     * 检查手机号是否存在（排除已删除）
     * 
     * @param phone 手机号
     * @return 是否存在
     */
    boolean existsByPhoneAndDeletedFalse(String phone);
    
    /**
     * 根据用户名查找用户（排除已删除）
     * 
     * @param username 用户名
     * @return 用户实体，如果不存在则返回空
     */
    Optional<User> findByUsernameAndDeleted(String username, Integer deleted);
    
    /**
     * 根据用户名查找用户（排除已删除的便捷方法）
     * 
     * @param username 用户名
     * @return 用户实体，如果不存在则返回空
     */
    default Optional<User> findByUsernameAndDeletedFalse(String username) {
        return findByUsernameAndDeleted(username, 0);
    }
    
    /**
     * 根据邮箱查找用户（排除已删除）
     * 
     * @param email 邮箱地址
     * @return 用户实体，如果不存在则返回空
     */
    Optional<User> findByEmailAndDeleted(String email, Integer deleted);
    
    /**
     * 根据邮箱查找用户（排除已删除的便捷方法）
     * 
     * @param email 邮箱地址
     * @return 用户实体，如果不存在则返回空
     */
    default Optional<User> findByEmailAndDeletedFalse(String email) {
        return findByEmailAndDeleted(email, 0);
    }
    
    /**
     * 根据手机号查找用户（排除已删除）
     * 
     * @param phone 手机号码
     * @return 用户实体，如果不存在则返回空
     */
    Optional<User> findByPhoneAndDeleted(String phone, Integer deleted);
    
    /**
     * 根据手机号查找用户（排除已删除的便捷方法）
     * 
     * @param phone 手机号码
     * @return 用户实体，如果不存在则返回空
     */
    default Optional<User> findByPhoneAndDeletedFalse(String phone) {
        return findByPhoneAndDeleted(phone, 0);
    }
    
    // ======================== 多条件查询方法 ========================
    
    /**
     * 根据登录凭证查找用户（支持用户名、邮箱、手机号）
     * 使用自定义查询，一次查询支持多种登录方式
     * 
     * @param loginId 登录凭证
     * @return 用户实体，如果不存在则返回空
     */
    @Query("SELECT u FROM User u WHERE u.deleted = 0 AND " +
           "(u.username = :loginId OR u.email = :loginId OR u.phone = :loginId)")
    Optional<User> findByLoginId(@Param("loginId") String loginId);
    
    /**
     * 检查用户名是否已存在（排除指定用户ID）
     * 用于注册和更新时的唯一性校验
     * 
     * @param username 用户名
     * @param excludeId 排除的用户ID（更新时使用）
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.deleted = 0 AND " +
           "u.username = :username AND (:excludeId IS NULL OR u.id != :excludeId)")
    boolean existsByUsernameExcludingId(@Param("username") String username, 
                                       @Param("excludeId") Long excludeId);
    
    /**
     * 检查邮箱是否已存在（排除指定用户ID）
     * 
     * @param email 邮箱地址
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.deleted = 0 AND " +
           "u.email = :email AND (:excludeId IS NULL OR u.id != :excludeId)")
    boolean existsByEmailExcludingId(@Param("email") String email, 
                                    @Param("excludeId") Long excludeId);
    
    /**
     * 检查手机号是否已存在（排除指定用户ID）
     * 
     * @param phone 手机号码
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.deleted = 0 AND " +
           "u.phone = :phone AND (:excludeId IS NULL OR u.id != :excludeId)")
    boolean existsByPhoneExcludingId(@Param("phone") String phone, 
                                    @Param("excludeId") Long excludeId);
    
    // ======================== 状态相关查询 ========================
    
    /**
     * 根据用户状态查找用户列表（分页）
     * 
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户列表（分页）
     */
    Page<User> findByStatusAndDeletedOrderByCreatedAtDesc(UserStatus status, Integer deleted, Pageable pageable);
    
    /**
     * 根据用户状态查找用户列表（便捷方法）
     * 
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户列表（分页）
     */
    default Page<User> findByStatusOrderByCreatedAtDesc(UserStatus status, Pageable pageable) {
        return findByStatusAndDeletedOrderByCreatedAtDesc(status, 0, pageable);
    }
    
    /**
     * 查找锁定时间已过期的用户
     * 用于定时任务自动解锁过期的锁定账户
     * 
     * @param currentTime 当前时间
     * @return 需要解锁的用户列表
     */
    @Query("SELECT u FROM User u WHERE u.deleted = 0 AND u.status = 'LOCKED' AND " +
           "u.lockedUntil IS NOT NULL AND u.lockedUntil < :currentTime")
    List<User> findExpiredLockedUsers(@Param("currentTime") LocalDateTime currentTime);
    
    // ======================== 统计查询方法 ========================
    
    /**
     * 统计各状态的用户数量
     * 
     * @return 状态统计列表，每个元素包含[状态, 数量]
     */
    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.deleted = 0 GROUP BY u.status")
    List<Object[]> countByStatus();
    
    /**
     * 统计指定时间段内注册的用户数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = 0 AND " +
           "u.createdAt >= :startTime AND u.createdAt <= :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计活跃用户数量（指定时间段内有登录记录）
     * 
     * @param startTime 开始时间
     * @return 活跃用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = 0 AND " +
           "u.lastLoginAt >= :startTime")
    long countActiveUsers(@Param("startTime") LocalDateTime startTime);
    
    // ======================== 批量操作方法 ========================
    
    /**
     * 批量更新用户状态
     * 
     * @param userIds 用户ID列表
     * @param status 新状态
     * @param updatedBy 更新者ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status, u.updatedBy = :updatedBy, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.id IN :userIds AND u.deleted = 0")
    int updateStatusByIds(@Param("userIds") List<Long> userIds, 
                         @Param("status") UserStatus status,
                         @Param("updatedBy") Long updatedBy);
    
    /**
     * 软删除用户（批量）
     * 
     * @param userIds 用户ID列表
     * @param updatedBy 操作者ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = 1, u.updatedBy = :updatedBy, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.id IN :userIds AND u.deleted = 0")
    int softDeleteByIds(@Param("userIds") List<Long> userIds, 
                       @Param("updatedBy") Long updatedBy);
    
    /**
     * 重置登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.loginFailureCount = 0, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.id = :userId AND u.deleted = 0")
    int resetLoginFailureCount(@Param("userId") Long userId);
    
    // ======================== 管理后台查询方法 ========================
    
    /**
     * 管理后台分页查询用户（包含已删除）
     * 支持按用户名、邮箱、手机号模糊搜索
     * 
     * @param keyword 搜索关键字
     * @param status 用户状态（可为空）
     * @param pageable 分页参数
     * @return 用户列表（分页）
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phone LIKE %:keyword% OR u.nickname LIKE %:keyword%) AND " +
           "(:status IS NULL OR u.status = :status) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findForAdmin(@Param("keyword") String keyword, 
                           @Param("status") UserStatus status, 
                           Pageable pageable);
    
    /**
     * 查找所有未删除的用户（用于导出等场景）
     * 
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.deleted = 0 ORDER BY u.createdAt DESC")
    List<User> findAllActive();
}