package com.ecommerce.dto.request.user;

/*
 * 文件职责: 用户注册请求DTO，封装用户注册时的输入参数
 * 
 * 开发心理活动：
 * 1. 为什么需要请求DTO？
 *    - 前后端数据传输的标准格式，与实体类解耦
 *    - 可以只包含注册需要的字段，避免不必要的数据传输
 *    - 独立的参数验证逻辑，不影响实体类的业务验证
 *    - 提高接口安全性，防止不当的字段操作
 * 
 * 2. 注册字段选择考虑：
 *    - 必填字段：用户名、密码、确认密码、邮箱
 *    - 可选字段：手机号、昵称（可以后续完善）
 *    - 不包含系统字段：ID、创建时间、状态等
 *    - 安全考虑：密码确认、防止恶意注册
 * 
 * 3. 验证注解设计：
 *    - @NotBlank：非空验证，适用于字符串
 *    - @Email：邮箱格式验证
 *    - @Size：长度限制验证
 *    - @Pattern：正则表达式验证
 *    - 自定义验证：密码确认一致性验证
 * 
 * 4. 扩展性设计：
 *    - 支持多种注册方式（用户名、邮箱、手机号）
 *    - 预留验证码字段，支持人机验证
 *    - 支持邀请码注册
 *    - 预留同意条款字段
 * 
 * 包结构设计思路:
 * - 放在dto.request.user包下，专门处理用户相关请求
 * - 与response DTO分离，职责清晰
 * 
 * 命名原因:
 * - UserRegisterRequest明确表达这是用户注册请求对象
 * - 符合RESTful API的DTO命名规范
 * 
 * 依赖关系:
 * - 被Controller接收，进行参数绑定和验证
 * - 在Service层转换为Entity对象
 * - 独立于Entity，便于API演进
 */

import com.ecommerce.validator.annotation.FieldMatch;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户注册请求DTO
 * 
 * 功能说明：
 * 1. 封装用户注册时的输入参数
 * 2. 提供完整的参数验证规则
 * 3. 支持多种注册方式和扩展功能
 * 4. 保护系统安全，防止恶意注册
 * 
 * 验证规则：
 * 1. 用户名：3-20字符，只能包含字母数字下划线
 * 2. 密码：6-20字符，包含字母和数字
 * 3. 邮箱：标准邮箱格式
 * 4. 手机号：中国大陆手机号格式（可选）
 * 5. 密码确认：必须与密码一致
 * 6. 验证码：防止机器注册（可选）
 * 
 * 使用场景：
 * 1. 用户注册接口参数接收
 * 2. 注册表单数据验证
 * 3. 批量注册数据处理
 * 
 * 安全考虑：
 * 1. 密码长度和复杂度要求
 * 2. 用户名格式限制，防止特殊字符
 * 3. 邮箱格式验证，确保通信有效性
 * 4. 验证码机制，防止暴力注册
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(first = "password", second = "confirmPassword", message = "密码和确认密码不一致")
public class UserRegisterRequest {

    // ========== 必填字段 ==========

    /**
     * 用户名
     * 
     * 验证规则：
     * - 不能为空
     * - 长度：3-20个字符
     * - 格式：只能包含字母、数字、下划线
     * - 必须以字母开头
     * 
     * 业务规则：
     * - 全局唯一性在Service层验证
     * - 不允许使用系统保留词
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名必须以字母开头，只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     * 
     * 验证规则：
     * - 不能为空
     * - 长度：6-20个字符
     * - 复杂度：至少包含字母和数字
     * 
     * 安全考虑：
     * - 前端传输时应该已经进行客户端验证
     * - 后端存储前需要进行加密处理
     * - 不在日志中记录明文密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]*$", 
             message = "密码必须包含至少一个字母和一个数字")
    private String password;

    /**
     * 确认密码
     * 
     * 验证规则：
     * - 不能为空
     * - 必须与密码字段一致
     * 
     * 实现方式：
     * - 使用自定义@FieldMatch注解进行验证
     * - 在类级别进行验证，确保两个字段一致性
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 邮箱地址
     * 
     * 验证规则：
     * - 不能为空
     * - 必须符合邮箱格式
     * - 长度不超过100个字符
     * 
     * 业务规则：
     * - 全局唯一性在Service层验证
     * - 注册后需要进行邮箱验证
     * - 可作为登录凭据使用
     */
    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    // ========== 可选字段 ==========

    /**
     * 手机号码
     * 
     * 验证规则：
     * - 可以为空（可选字段）
     * - 如果填写，必须符合中国大陆手机号格式
     * 
     * 业务规则：
     * - 全局唯一性在Service层验证
     * - 可作为登录凭据使用
     * - 用于短信通知和验证
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 昵称
     * 
     * 验证规则：
     * - 可以为空（可选字段）
     * - 如果填写，长度不超过50个字符
     * - 不能包含特殊字符
     * 
     * 业务规则：
     * - 可以重复，不要求唯一性
     * - 用于前端显示和社交功能
     * - 如果为空，系统使用用户名作为显示名称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5a-zA-Z0-9_\\s]*$", message = "昵称只能包含中文、字母、数字、下划线和空格")
    private String nickname;

    // ========== 安全和扩展字段 ==========

    /**
     * 图形验证码
     * 
     * 验证规则：
     * - 可以为空（根据系统配置决定是否必填）
     * - 长度通常为4-6个字符
     * - 不区分大小写
     * 
     * 业务规则：
     * - 防止机器批量注册
     * - 验证后需要刷新验证码
     * - 有时效性限制
     */
    @Size(max = 10, message = "验证码长度不能超过10个字符")
    private String captcha;

    /**
     * 短信验证码
     * 
     * 验证规则：
     * - 当手机号不为空时，可能需要短信验证码
     * - 通常为4-6位数字
     * 
     * 业务规则：
     * - 与手机号绑定验证
     * - 有时效性限制（通常5-10分钟）
     * - 每个手机号有发送频率限制
     */
    @Pattern(regexp = "^$|^\\d{4,6}$", message = "短信验证码格式不正确")
    private String smsCode;

    /**
     * 邀请码
     * 
     * 验证规则：
     * - 可以为空（可选功能）
     * - 长度和格式根据业务需求定义
     * 
     * 业务规则：
     * - 用于邀请注册功能
     * - 可能关联奖励机制
     * - 需要验证邀请码的有效性
     */
    @Size(max = 20, message = "邀请码长度不能超过20个字符")
    @Pattern(regexp = "^$|^[A-Z0-9]*$", message = "邀请码只能包含大写字母和数字")
    private String inviteCode;

    /**
     * 同意用户协议
     * 
     * 验证规则：
     * - 必须为true（用户必须同意才能注册）
     * 
     * 业务规则：
     * - 法律合规要求
     * - 用户权益保护
     * - 服务条款确认
     */
    @NotNull(message = "必须同意用户协议")
    @AssertTrue(message = "必须同意用户协议才能注册")
    private Boolean agreeTerms;

    /**
     * 同意接收营销信息
     * 
     * 验证规则：
     * - 可以为空，默认为false
     * 
     * 业务规则：
     * - 用户营销策略
     * - 隐私保护合规
     * - 可以后续修改
     */
    private Boolean agreeMarketing = false;

    // ========== 业务方法 ==========

    /**
     * 验证密码强度
     * 
     * 自定义业务验证方法，可以在Service层调用
     * 提供比注解验证更灵活的验证逻辑
     * 
     * @return true-密码强度足够，false-密码强度不够
     */
    public boolean isPasswordStrong() {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // 检查长度
        if (password.length() < 8) {
            return false;
        }
        
        // 检查复杂度：包含大写字母、小写字母、数字、特殊字符中的至少3种
        int complexity = 0;
        if (password.matches(".*[a-z].*")) complexity++;
        if (password.matches(".*[A-Z].*")) complexity++;
        if (password.matches(".*\\d.*")) complexity++;
        if (password.matches(".*[!@#$%^&*()].*")) complexity++;
        
        return complexity >= 3;
    }

    /**
     * 检查是否需要手机验证
     * 
     * @return true-需要手机验证，false-不需要手机验证
     */
    public boolean requiresPhoneVerification() {
        return phone != null && !phone.trim().isEmpty();
    }

    /**
     * 检查是否有邀请码
     * 
     * @return true-有邀请码，false-无邀请码
     */
    public boolean hasInviteCode() {
        return inviteCode != null && !inviteCode.trim().isEmpty();
    }

    /**
     * 获取显示用的用户名
     * 如果有昵称就用昵称，否则用用户名
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return (nickname != null && !nickname.trim().isEmpty()) ? nickname : username;
    }

    /**
     * 清除敏感信息
     * 用于日志记录时保护用户隐私
     * 
     * @return 清除敏感信息后的对象
     */
    public UserRegisterRequest sanitizeForLogging() {
        UserRegisterRequest sanitized = new UserRegisterRequest();
        sanitized.username = this.username;
        sanitized.email = this.email != null ? this.email.replaceAll("(.{2}).*(@.*)", "$1***$2") : null;
        sanitized.phone = this.phone != null ? this.phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : null;
        sanitized.nickname = this.nickname;
        sanitized.agreeTerms = this.agreeTerms;
        sanitized.agreeMarketing = this.agreeMarketing;
        // 不包含密码、验证码等敏感信息
        return sanitized;
    }
}