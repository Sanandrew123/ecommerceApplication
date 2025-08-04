package com.ecommerce.repository.jpa;

/*
 * 文件职责: 用户数据访问接口，定义用户相关的数据操作方法
 * 
 * 开发心理活动：
 * 1. Repository层设计原则：
 *    - 继承JpaRepository获得基础CRUD操作
 *    - 定义业务特定的查询方法
 *    - 使用Spring Data JPA的方法命名规范
 *    - 复杂查询使用@Query注解
 * 
 * 2. 查询方法设计考虑：
 *    - 登录相关：根据用户名/邮箱/手机号查找用户
 *    - 验证相关：检查用户名/邮箱/手机号是否存在
 *    - 状态查询：根据状态、角色等筛选用户
 *    - 分页查询：支持用户列表的分页展示
 * 
 * 3. 性能优化考虑：
 *    - 为常用查询字段添加数据库索引
 *    - 使用Optional处理可能为空的查询结果
 *    - 避免N+1查询问题
 *    - 合理使用JPA懒加载和急加载
 * 
 * 4. 安全考虑：
 *    - 软删除支持，不物理删除用户数据
 *    - 敏感查询添加权限控制
 *    - 防止SQL注入（使用参数化查询）
 *    - 查询结果脱敏处理
 * 
 * 包结构设计思路:
 * - 放在repository.jpa包下，专门处理JPA数据访问
 * - 与elasticsearch、redis等其他数据访问方式分离
 * 
 * 命名原因:
 * - UserRepository明确表达用户数据访问功能
 * - 符合Repository后缀的领域驱动设计规范
 * 
 * 依赖关系:
 * - 继承JpaRepository，获得标准CRUD操作
 * - 被Service层调用，进行数据访问
 * - 操作User实体，与数据库交互
 */

import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * 功能说明：
 * 1. 继承JpaRepository获得标准CRUD操作
 * 2. 继承JpaSpecificationExecutor支持动态查询
 * 3. 定义业务特定的查询方法
 * 4. 提供高性能的数据访问操作
 * 
 * 查询能力：
 * 1. 基础查询：根据ID、用户名等单一条件查询
 * 2. 复合查询：多条件组合查询
 * 3. 模糊查询：用户名、昵称等模糊匹配
 * 4. 分页查询：支持大数据量的分页展示
 * 5. 统计查询：用户数量、活跃度等统计
 * 
 * 性能特性：
 * 1. 基于索引的高效查询
 * 2. 延迟加载和急加载优化
 * 3. 查询结果缓存支持
 * 4. 批量操作支持
 * 
 * 安全特性：
 * 1. 软删除支持，保护历史数据
 * 2. 参数化查询，防止SQL注入
 * 3. 权限相关的查询过滤
 * 4. 敏感信息访问控制
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // ========== 基础查询方法 ==========

    /**
     * 根据用户名查找用户
     * 
     * 用途：
     * - 用户登录验证
     * - 用户名唯一性检查
     * - 用户信息查询
     * 
     * 性能：基于username索引，查询效率高
     * 安全：自动过滤软删除的用户
     * 
     * @param username 用户名
     * @return 用户对象（可能为空）
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    /**
     * 根据邮箱查找用户
     * 
     * 用途：
     * - 邮箱登录验证
     * - 邮箱唯一性检查
     * - 密码找回功能
     * 
     * @param email 邮箱地址
     * @return 用户对象（可能为空）
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * 根据手机号查找用户
     * 
     * 用途：
     * - 手机号登录验证
     * - 手机号唯一性检查
     * - 短信验证功能
     * 
     * @param phone 手机号
     * @return 用户对象（可能为空）
     */
    Optional<User> findByPhoneAndDeletedFalse(String phone);

    /**
     * 根据登录凭据查找用户
     * 支持用户名、邮箱、手机号任意一种方式登录
     * 
     * 业务价值：
     * - 统一登录入口，提升用户体验
     * - 减少用户记忆负担
     * - 降低登录门槛
     * 
     * 实现方式：
     * - 使用OR条件组合三种查询方式
     * - 基于多个索引字段查询
     * - 自动过滤软删除用户
     * 
     * @param credential 登录凭据（用户名/邮箱/手机号）
     * @return 用户对象（可能为空）
     */
    @Query("SELECT u FROM User u WHERE (u.username = :credential OR u.email = :credential OR u.phone = :credential) AND u.deleted = false")
    Optional<User> findByCredentialAndDeletedFalse(@Param("credential") String credential);

    // ========== 存在性检查方法 ==========

    /**
     * 检查用户名是否已存在
     * 
     * 用途：
     * - 用户注册时的唯一性验证
     * - 用户名修改时的重复检查
     * - 批量导入时的数据验证
     * 
     * 性能优化：
     * - 只查询存在性，不返回完整对象
     * - 基于用户名索引，查询速度快
     * 
     * @param username 用户名
     * @return true-已存在，false-不存在
     */
    boolean existsByUsernameAndDeletedFalse(String username);

    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱地址
     * @return true-已存在，false-不存在
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * 检查手机号是否已存在
     * 
     * @param phone 手机号
     * @return true-已存在，false-不存在
     */
    boolean existsByPhoneAndDeletedFalse(String phone);

    /**
     * 检查除了指定用户外，用户名是否已存在
     * 用于用户信息修改时的唯一性检查
     * 
     * @param username 用户名
     * @param userId 排除的用户ID
     * @return true-已存在，false-不存在
     */
    boolean existsByUsernameAndIdNotAndDeletedFalse(String username, Long userId);

    /**
     * 检查除了指定用户外，邮箱是否已存在
     * 
     * @param email 邮箱地址
     * @param userId 排除的用户ID
     * @return true-已存在，false-不存在
     */
    boolean existsByEmailAndIdNotAndDeletedFalse(String email, Long userId);

    /**
     * 检查除了指定用户外，手机号是否已存在
     * 
     * @param phone 手机号
     * @param userId 排除的用户ID
     * @return true-已存在，false-不存在
     */
    boolean existsByPhoneAndIdNotAndDeletedFalse(String phone, Long userId);

    // ========== 状态和角色查询 ==========

    /**
     * 根据用户状态查找用户列表
     * 
     * 用途：
     * - 管理员查看不同状态的用户
     * - 批量操作特定状态用户
     * - 用户状态统计分析
     * 
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByStatusAndDeletedFalse(User.UserStatus status, Pageable pageable);

    /**
     * 根据用户角色查找用户列表
     * 
     * 用途：
     * - 角色权限管理
     * - 用户分类展示
     * - 权限审计功能
     * 
     * @param role 用户角色
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByRoleAndDeletedFalse(User.UserRole role, Pageable pageable);

    /**
     * 查找启用的用户列表
     * 
     * 业务逻辑：
     * - 状态为ACTIVE且未被软删除的用户
     * - 用于正常业务功能的用户筛选
     * 
     * @param pageable 分页参数
     * @return 启用用户分页结果
     */
    Page<User> findByStatusAndDeletedFalse(Pageable pageable);

    // ========== 搜索查询方法 ==========

    /**
     * 根据用户名或昵称模糊搜索用户
     * 
     * 用途：
     * - 用户搜索功能
     * - @提及用户功能
     * - 好友添加搜索
     * 
     * 实现：
     * - 使用LIKE操作符进行模糊匹配
     * - 同时搜索用户名和昵称字段
     * - 忽略大小写差异
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配用户分页结果
     */
    @Query("SELECT u FROM User u WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND u.deleted = false")
    Page<User> searchByUsernameOrNickname(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 高级用户搜索
     * 支持多字段组合搜索
     * 
     * 搜索字段：
     * - 用户名（精确和模糊）
     * - 昵称（模糊）
     * - 邮箱（模糊）
     * - 真实姓名（模糊）
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配用户分页结果
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.realName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.deleted = false")
    Page<User> advancedSearch(@Param("keyword") String keyword, Pageable pageable);

    // ========== 时间范围查询 ==========

    /**
     * 查找指定时间范围内注册的用户
     * 
     * 用途：
     * - 新用户统计分析
     * - 注册趋势分析
     * - 运营活动效果评估
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByCreatedAtBetweenAndDeletedFalse(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查找最近登录的活跃用户
     * 
     * 用途：
     * - 活跃用户分析
     * - 推荐系统用户筛选
     * - 营销活动目标用户
     * 
     * @param since 最后登录时间门槛
     * @param pageable 分页参数
     * @return 活跃用户分页结果
     */
    Page<User> findByLastLoginAtAfterAndDeletedFalse(LocalDateTime since, Pageable pageable);

    /**
     * 查找长期未登录的用户
     * 
     * 用途：
     * - 用户流失分析
     * - 召回活动目标用户
     * - 账户清理策略
     * 
     * @param before 最后登录时间门槛
     * @param pageable 分页参数
     * @return 不活跃用户分页结果
     */
    Page<User> findByLastLoginAtBeforeAndDeletedFalse(LocalDateTime before, Pageable pageable);

    // ========== 统计查询方法 ==========

    /**
     * 统计总用户数（排除软删除）
     * 
     * @return 用户总数
     */
    long countByDeletedFalse();

    /**
     * 统计指定状态的用户数量
     * 
     * @param status 用户状态
     * @return 用户数量
     */
    long countByStatusAndDeletedFalse(User.UserStatus status);

    /**
     * 统计指定角色的用户数量
     * 
     * @param role 用户角色
     * @return 用户数量
     */
    long countByRoleAndDeletedFalse(User.UserRole role);

    /**
     * 统计今日新注册用户数
     * 
     * @param startOfDay 当日开始时间
     * @return 新注册用户数
     */
    long countByCreatedAtAfterAndDeletedFalse(LocalDateTime startOfDay);

    /**
     * 统计指定时间范围内的注册用户数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 注册用户数
     */
    long countByCreatedAtBetweenAndDeletedFalse(LocalDateTime startTime, LocalDateTime endTime);

    // ========== 更新操作方法 ==========

    /**
     * 批量更新用户最后登录时间
     * 
     * 用途：
     * - 用户登录时更新登录时间
     * - 活跃度统计的数据基础
     * 
     * 性能考虑：
     * - 使用原生SQL提高更新效率
     * - 减少JPA的额外开销
     * 
     * @param userId 用户ID
     * @param lastLoginAt 最后登录时间
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    int updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 批量更新用户状态
     * 
     * 用途：
     * - 批量用户管理操作
     * - 系统维护和清理
     * 
     * @param userIds 用户ID列表
     * @param status 新状态
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :userIds")
    int batchUpdateStatus(@Param("userIds") List<Long> userIds, @Param("status") User.UserStatus status);

    /**
     * 软删除用户
     * 
     * 用途：
     * - 用户注销功能
     * - 管理员删除用户
     * - 数据保护和恢复
     * 
     * 实现：
     * - 设置deleted标志为true
     * - 保留历史数据不物理删除
     * 
     * @param userId 用户ID
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = true WHERE u.id = :userId")
    int softDeleteUser(@Param("userId") Long userId);

    /**
     * 恢复软删除的用户
     * 
     * 用途：
     * - 用户数据恢复
     * - 误删除操作撤销
     * 
     * @param userId 用户ID
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = false WHERE u.id = :userId")
    int restoreUser(@Param("userId") Long userId);

    // ========== 验证状态相关 ==========

    /**
     * 查找未验证邮箱的用户
     * 
     * 用途：
     * - 邮箱验证提醒
     * - 用户完整性统计
     * 
     * @param pageable 分页参数
     * @return 未验证邮箱用户分页结果
     */
    Page<User> findByEmailVerifiedFalseAndDeletedFalse(Pageable pageable);

    /**
     * 查找未验证手机号的用户
     * 
     * @param pageable 分页参数
     * @return 未验证手机号用户分页结果
     */
    Page<User> findByPhoneVerifiedFalseAndDeletedFalse(Pageable pageable);

    /**
     * 统计邮箱验证用户数量
     * 
     * @return 已验证邮箱用户数
     */
    long countByEmailVerifiedTrueAndDeletedFalse();

    /**
     * 统计手机验证用户数量
     * 
     * @return 已验证手机用户数
     */
    long countByPhoneVerifiedTrueAndDeletedFalse();
}