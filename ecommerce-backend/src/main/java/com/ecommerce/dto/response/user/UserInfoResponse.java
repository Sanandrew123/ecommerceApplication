package com.ecommerce.dto.response.user;

/*
 * 文件职责: 用户信息响应DTO，封装返回给前端的用户信息
 * 
 * 开发心理活动：
 * 1. 响应DTO设计原则：
 *    - 数据安全：过滤敏感信息，不暴露密码等
 *    - 前端友好：提供前端需要的格式化数据
 *    - 性能优化：只包含必要字段，减少传输量
 *    - 版本兼容：预留扩展字段，支持API演进
 * 
 * 2. 与实体类的区别：
 *    - 不包含敏感字段（密码、内部状态等）
 *    - 包含计算字段（显示名称、状态描述等）
 *    - 时间格式友好化处理
 *    - 枚举值转换为可读描述
 * 
 * 3. 数据转换考虑：
 *    - 从Entity到Response的映射
 *    - 敏感信息的安全过滤
 *    - 业务数据的格式化处理
 *    - 关联数据的按需加载
 * 
 * 4. 扩展性设计：
 *    - 支持不同详细级别的响应
 *    - 预留个性化配置字段
 *    - 支持多语言显示
 *    - 预留统计信息字段
 * 
 * 包结构设计思路:
 * - 放在dto.response.user包下，专门处理用户响应数据
 * - 与request DTO分离，输入输出职责清晰
 * 
 * 命名原因:
 * - UserInfoResponse明确表达用户信息响应功能
 * - 符合Response后缀的命名规范
 * 
 * 依赖关系:
 * - 由Service层创建，基于Entity转换
 * - 被Controller返回，作为接口响应数据
 * - 独立于Entity，便于接口演进
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 * 
 * 功能说明：
 * 1. 封装返回给前端的用户信息
 * 2. 过滤敏感信息，保护用户隐私
 * 3. 提供前端友好的数据格式
 * 4. 支持不同场景的信息展示
 * 
 * 数据安全：
 * 1. 不包含密码等敏感信息
 * 2. 手机号和邮箱根据权限脱敏
 * 3. 内部状态码转换为用户友好描述
 * 4. 系统字段按需展示
 * 
 * 使用场景：
 * 1. 用户个人信息查询
 * 2. 用户列表展示
 * 3. 用户资料编辑回显
 * 4. 第三方系统用户信息同步
 * 
 * 响应级别：
 * 1. 基础信息：用户名、昵称、头像
 * 2. 详细信息：个人资料、联系方式
 * 3. 完整信息：权限、状态、统计数据
 * 4. 管理信息：审计字段、系统状态
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {

    // ========== 基础标识信息 ==========

    /**
     * 用户ID
     * 
     * 用途：
     * - 前端进行用户相关操作的标识
     * - 关联其他业务数据
     * - 缓存和状态管理
     */
    private Long id;

    /**
     * 用户名
     * 
     * 展示规则：
     * - 总是显示，作为用户唯一标识
     * - 用于登录和@提及功能
     * - 不可修改，保持稳定性
     */
    private String username;

    /**
     * 昵称
     * 
     * 展示规则：
     * - 优先显示昵称，提升用户体验
     * - 可以为空，空时使用用户名
     * - 支持用户自定义修改
     */
    private String nickname;

    /**
     * 显示名称
     * 
     * 计算字段：
     * - 有昵称显示昵称，无昵称显示用户名
     * - 前端直接使用，无需再次判断
     * - 统一显示逻辑，提升一致性
     */
    private String displayName;

    // ========== 个人信息 ==========

    /**
     * 真实姓名
     * 
     * 隐私保护：
     * - 仅在特定场景下返回（如实名认证后）
     * - 根据权限决定是否返回
     * - 可能进行脱敏处理
     */
    private String realName;

    /**
     * 头像URL
     * 
     * 处理逻辑：
     * - 返回完整的可访问URL
     * - 支持默认头像机制
     * - 考虑CDN加速和图片压缩
     */
    private String avatar;

    /**
     * 性别
     * 
     * 显示处理：
     * - 返回枚举值的显示名称
     * - 支持国际化显示
     * - 保护用户隐私选择
     */
    private String gender;

    /**
     * 性别显示名称
     * 
     * 计算字段：
     * - 前端友好的性别显示
     * - 支持多语言
     * - 处理未知状态
     */
    private String genderDisplay;

    /**
     * 生日
     * 
     * 隐私考虑：
     * - 根据用户隐私设置决定返回
     * - 可能只返回年月，隐藏具体日期
     * - 用于年龄计算和生日提醒
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 年龄
     * 
     * 计算字段：
     * - 基于生日自动计算
     * - 避免前端重复计算
     * - 考虑隐私保护
     */
    private Integer age;

    /**
     * 个人简介
     * 
     * 展示处理：
     * - 限制长度，避免过长显示
     * - 过滤敏感词和格式化
     * - 支持富文本内容
     */
    private String bio;

    // ========== 联系方式 ==========

    /**
     * 邮箱地址
     * 
     * 脱敏处理：
     * - 根据权限决定是否脱敏
     * - 格式：user***@domain.com
     * - 保护用户隐私
     */
    private String email;

    /**
     * 邮箱验证状态
     * 
     * 业务价值：
     * - 前端显示验证标识
     * - 决定是否需要验证提醒
     * - 影响功能可用性
     */
    private Boolean emailVerified;

    /**
     * 手机号码
     * 
     * 脱敏处理：
     * - 格式：138****8888
     * - 根据权限决定脱敏级别
     * - 保护用户隐私
     */
    private String phone;

    /**
     * 手机验证状态
     * 
     * 业务价值：
     * - 影响安全级别显示
     * - 决定功能权限
     * - 验证提醒逻辑
     */
    private Boolean phoneVerified;

    // ========== 账户状态 ==========

    /**
     * 用户状态
     * 
     * 显示处理：
     * - 返回状态码，前端进行逻辑判断
     * - 配合statusDisplay提供友好显示
     * - 影响前端功能可用性
     */
    private String status;

    /**
     * 状态显示名称
     * 
     * 计算字段：
     * - 状态的友好显示文本
     * - 支持国际化
     * - 便于前端直接展示
     */
    private String statusDisplay;

    /**
     * 用户角色
     * 
     * 权限标识：
     * - 前端根据角色显示不同功能
     * - 影响菜单和操作权限
     * - 用于权限控制逻辑
     */
    private String role;

    /**
     * 角色显示名称
     * 
     * 计算字段：
     * - 角色的友好显示文本
     * - 支持国际化
     * - 便于前端权限说明
     */
    private String roleDisplay;

    /**
     * 账户是否启用
     * 
     * 业务逻辑：
     * - 综合状态判断结果
     * - 前端功能可用性判断
     * - 登录权限控制
     */
    private Boolean enabled;

    // ========== 时间信息 ==========

    /**
     * 注册时间
     * 
     * 展示用途：
     * - 用户资料完整性展示
     * - 用户等级和权益计算
     * - 数据分析和统计
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     * 
     * 技术用途：
     * - 数据版本控制
     * - 缓存失效判断
     * - 同步状态跟踪
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     * 
     * 业务价值：
     * - 用户活跃度展示
     * - 安全提醒（异常登录时间）
     * - 数据分析依据
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    // ========== 扩展信息 ==========

    /**
     * 用户等级
     * 
     * 计算字段：
     * - 基于注册时间、活跃度等计算
     * - 影响用户权益和展示
     * - gamification元素
     */
    private Integer level;

    /**
     * 积分余额
     * 
     * 业务字段：
     * - 用户积分系统
     * - 影响权益和功能
     * - 需要权限控制
     */
    private Long points;

    /**
     * VIP状态
     * 
     * 会员体系：
     * - VIP等级标识
     * - 影响功能权限
     * - 前端差异化展示
     */
    private Boolean vipStatus;

    /**
     * VIP过期时间
     * 
     * 会员管理：
     * - VIP服务到期时间
     * - 续费提醒逻辑
     * - 权益有效期判断
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime vipExpireAt;

    // ========== 统计信息（管理员可见） ==========

    /**
     * 登录次数
     * 
     * 统计数据：
     * - 用户活跃度指标
     * - 仅管理员可见
     * - 数据分析用途
     */
    private Long loginCount;

    /**
     * 订单数量
     * 
     * 业务统计：
     * - 用户消费行为指标
     * - 用户等级计算依据
     * - 个性化推荐参考
     */
    private Long orderCount;

    /**
     * 消费金额
     * 
     * 重要指标：
     * - 用户价值评估
     * - VIP等级判断
     * - 权限和优惠依据
     */
    private String totalAmount;

    // ========== 业务方法 ==========

    /**
     * 是否为管理员
     * 
     * @return true-管理员，false-普通用户
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * 是否为VIP用户
     * 
     * @return true-VIP用户，false-普通用户
     */
    public boolean isVip() {
        return Boolean.TRUE.equals(vipStatus) && 
               (vipExpireAt == null || vipExpireAt.isAfter(LocalDateTime.now()));
    }

    /**
     * 是否为活跃用户
     * 判断标准：30天内有登录记录
     * 
     * @return true-活跃用户，false-非活跃用户
     */
    public boolean isActiveUser() {
        if (lastLoginAt == null) {
            return false;
        }
        return lastLoginAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * 获取用户标签
     * 综合用户各种状态生成标签列表
     * 
     * @return 用户标签数组
     */
    public String[] getUserTags() {
        java.util.List<String> tags = new java.util.ArrayList<>();
        
        if (isAdmin()) {
            tags.add("管理员");
        }
        if (isVip()) {
            tags.add("VIP");
        }
        if (Boolean.TRUE.equals(emailVerified)) {
            tags.add("邮箱已验证");
        }
        if (Boolean.TRUE.equals(phoneVerified)) {
            tags.add("手机已验证");
        }
        if (!isActiveUser()) {
            tags.add("不活跃");
        }
        if (level != null && level >= 5) {
            tags.add("高级用户");
        }
        
        return tags.toArray(new String[0]);
    }

    /**
     * 创建基础信息响应
     * 只包含基本展示信息，用于列表显示
     * 
     * @return 基础信息对象
     */
    public UserInfoResponse toBasicInfo() {
        return UserInfoResponse.builder()
                .id(this.id)
                .username(this.username)
                .nickname(this.nickname)
                .displayName(this.displayName)
                .avatar(this.avatar)
                .role(this.role)
                .roleDisplay(this.roleDisplay)
                .enabled(this.enabled)
                .build();
    }

    /**
     * 创建公开信息响应
     * 过滤敏感信息，用于公开展示
     * 
     * @return 公开信息对象
     */
    public UserInfoResponse toPublicInfo() {
        return UserInfoResponse.builder()
                .id(this.id)
                .username(this.username)
                .nickname(this.nickname)
                .displayName(this.displayName)
                .avatar(this.avatar)
                .gender(this.gender)
                .genderDisplay(this.genderDisplay)
                .bio(this.bio)
                .level(this.level)
                .vipStatus(this.vipStatus)
                .createdAt(this.createdAt)
                .build();
    }
}