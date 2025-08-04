package com.ecommerce.dto.request.user;

/*
 * 文件职责: 用户登录请求DTO，封装用户登录时的输入参数
 * 
 * 开发心理活动：
 * 1. 登录参数设计考虑：
 *    - 支持多种登录方式：用户名、邮箱、手机号
 *    - 必要的安全验证：验证码、记住我功能
 *    - 简化用户体验：统一登录凭据字段
 *    - 安全防护：防暴力破解、防机器登录
 * 
 * 2. 与注册DTO的区别：
 *    - 字段更少，只包含登录必需信息
 *    - 验证规则更宽松，兼容多种输入格式
 *    - 不包含业务字段，专注于认证过程
 *    - 安全性要求更高，防止恶意攻击
 * 
 * 3. 扩展性设计：
 *    - 支持第三方登录（OAuth、扫码等）
 *    - 支持多因子认证（短信、邮箱验证码）
 *    - 支持设备记住功能
 *    - 支持登录日志记录
 * 
 * 4. 安全考虑：
 *    - 登录凭据统一处理，避免枚举攻击
 *    - 验证码机制防止暴力破解
 *    - 记住我功能的安全实现
 *    - 敏感信息不记录日志
 * 
 * 包结构设计思路:
 * - 与UserRegisterRequest在同一包下，便于管理
 * - 专注于登录认证场景，职责单一
 * 
 * 命名原因:
 * - UserLoginRequest明确表达用户登录请求功能
 * - 与注册请求对应，命名一致性
 * 
 * 依赖关系:
 * - 被AuthController使用，处理登录请求
 * - 在AuthService中转换为认证对象
 * - 与Spring Security集成，进行身份验证
 */

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户登录请求DTO
 * 
 * 功能说明：
 * 1. 封装用户登录时的输入参数
 * 2. 支持多种登录方式（用户名/邮箱/手机号）
 * 3. 集成安全验证机制（验证码、记住我）
 * 4. 提供登录参数的统一验证规则
 * 
 * 登录方式支持：
 * 1. 用户名 + 密码
 * 2. 邮箱 + 密码
 * 3. 手机号 + 密码
 * 4. 手机号 + 短信验证码（免密登录）
 * 
 * 安全特性：
 * 1. 图形验证码防止机器登录
 * 2. 短信验证码支持免密登录
 * 3. 记住我功能延长会话
 * 4. 登录失败次数限制
 * 
 * 使用场景：
 * 1. Web端用户登录
 * 2. 移动端用户登录
 * 3. API接口认证
 * 4. 第三方系统集成
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {

    // ========== 登录凭据 ==========

    /**
     * 登录凭据（用户名/邮箱/手机号）
     * 
     * 设计思路：
     * - 统一字段，支持多种登录方式，提升用户体验
     * - 避免用户记忆多个字段名，降低使用成本
     * - 后端自动识别凭据类型，智能匹配
     * - 防止用户名枚举攻击
     * 
     * 验证规则：
     * - 不能为空
     * - 长度限制：3-100个字符
     * - 格式验证在Service层进行，这里保持灵活性
     */
    @NotBlank(message = "登录凭据不能为空")
    @Size(min = 3, max = 100, message = "登录凭据长度必须在3-100个字符之间")
    private String credential;

    /**
     * 密码
     * 
     * 验证规则：
     * - 当loginType为PASSWORD时必填
     * - 长度限制：6-20个字符
     * - 特殊情况：短信验证码登录时可以为空
     * 
     * 安全考虑：
     * - 前端传输应使用HTTPS
     * - 后端接收后立即进行加密比较
     * - 不在日志中记录明文密码
     */
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    // ========== 登录类型和验证 ==========

    /**
     * 登录类型
     * 
     * 支持的登录类型：
     * - PASSWORD: 密码登录（默认）
     * - SMS_CODE: 短信验证码登录
     * - EMAIL_CODE: 邮箱验证码登录
     * 
     * 业务逻辑：
     * - 不同登录类型有不同的验证要求
     * - 影响密码字段的必填性
     * - 决定验证码字段的使用
     */
    private LoginType loginType = LoginType.PASSWORD;

    /**
     * 图形验证码
     * 
     * 使用场景：
     * - 防止机器登录攻击
     * - 登录失败次数超限时强制要求
     * - 可配置是否启用
     * 
     * 验证规则：
     * - 根据系统配置决定是否必填
     * - 通常为4-6个字符
     * - 不区分大小写
     * - 有时效性限制
     */
    @Size(max = 10, message = "图形验证码长度不能超过10个字符")
    private String captcha;

    /**
     * 短信验证码
     * 
     * 使用场景：
     * - 短信验证码登录
     * - 密码登录的二次验证
     * - 敏感操作的确认
     * 
     * 验证规则：
     * - 当loginType为SMS_CODE时必填
     * - 4-6位数字
     * - 有时效性限制（通常5-10分钟）
     * - 与手机号绑定验证
     */
    @Pattern(regexp = "^$|^\\d{4,6}$", message = "短信验证码格式不正确")
    private String smsCode;

    /**
     * 邮箱验证码
     * 
     * 使用场景：
     * - 邮箱验证码登录
     * - 密码找回验证
     * - 账户安全验证
     * 
     * 验证规则：
     * - 当loginType为EMAIL_CODE时必填
     * - 4-8位字符（数字或字母）
     * - 有时效性限制
     * - 与邮箱地址绑定验证
     */
    @Size(max = 10, message = "邮箱验证码长度不能超过10个字符")
    private String emailCode;

    // ========== 登录选项 ==========

    /**
     * 记住我
     * 
     * 功能说明：
     * - 延长用户会话时间
     * - 下次访问自动登录
     * - 增强用户体验
     * 
     * 安全考虑：
     * - 使用独立的RememberMe Token
     * - 设置合理的过期时间
     * - 支持主动清除功能
     * - 限制并发会话数量
     */
    private Boolean rememberMe = false;

    /**
     * 客户端信息
     * 
     * 用途：
     * - 设备识别和管理
     * - 登录日志记录
     * - 安全策略应用
     * - 用户行为分析
     */
    @Size(max = 200, message = "客户端信息长度不能超过200个字符")
    private String clientInfo;

    /**
     * IP地址
     * 
     * 用途：
     * - 地理位置识别
     * - 异常登录检测
     * - 安全策略应用
     * - 访问日志记录
     * 
     * 注意：通常由Controller自动设置，用户无需填写
     */
    @Size(max = 50, message = "IP地址长度不能超过50个字符")
    private String ipAddress;

    // ========== 枚举定义 ==========

    /**
     * 登录类型枚举
     */
    public enum LoginType {
        /**
         * 密码登录
         */
        PASSWORD("密码登录"),
        
        /**
         * 短信验证码登录
         */
        SMS_CODE("短信验证码登录"),
        
        /**
         * 邮箱验证码登录
         */
        EMAIL_CODE("邮箱验证码登录");

        private final String displayName;

        LoginType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 判断是否为密码登录
     * 
     * @return true-密码登录，false-其他登录方式
     */
    public boolean isPasswordLogin() {
        return LoginType.PASSWORD.equals(loginType);
    }

    /**
     * 判断是否为短信验证码登录
     * 
     * @return true-短信验证码登录，false-其他登录方式
     */
    public boolean isSmsCodeLogin() {
        return LoginType.SMS_CODE.equals(loginType);
    }

    /**
     * 判断是否为邮箱验证码登录
     * 
     * @return true-邮箱验证码登录，false-其他登录方式
     */
    public boolean isEmailCodeLogin() {
        return LoginType.EMAIL_CODE.equals(loginType);
    }

    /**
     * 判断凭据是否为邮箱格式
     * 
     * @return true-邮箱格式，false-非邮箱格式
     */
    public boolean isEmailCredential() {
        if (credential == null || credential.trim().isEmpty()) {
            return false;
        }
        // 简单的邮箱格式检查
        return credential.contains("@") && credential.contains(".");
    }

    /**
     * 判断凭据是否为手机号格式
     * 
     * @return true-手机号格式，false-非手机号格式
     */
    public boolean isPhoneCredential() {
        if (credential == null || credential.trim().isEmpty()) {
            return false;
        }
        // 中国大陆手机号格式检查
        return credential.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 判断凭据是否为用户名格式
     * 
     * @return true-用户名格式，false-其他格式
     */
    public boolean isUsernameCredential() {
        return !isEmailCredential() && !isPhoneCredential();
    }

    /**
     * 检查是否需要图形验证码
     * 
     * 判断逻辑：
     * - 验证码字段不为空
     * - 或者根据业务规则需要验证码
     * 
     * @return true-需要验证码，false-不需要验证码
     */
    public boolean requiresCaptcha() {
        return captcha != null && !captcha.trim().isEmpty();
    }

    /**
     * 检查是否需要短信验证码
     * 
     * @return true-需要短信验证码，false-不需要短信验证码
     */
    public boolean requiresSmsCode() {
        return isSmsCodeLogin() || (smsCode != null && !smsCode.trim().isEmpty());
    }

    /**
     * 检查是否需要邮箱验证码
     * 
     * @return true-需要邮箱验证码，false-不需要邮箱验证码
     */
    public boolean requiresEmailCode() {
        return isEmailCodeLogin() || (emailCode != null && !emailCode.trim().isEmpty());
    }

    /**
     * 获取登录方式描述
     * 
     * @return 登录方式的友好描述
     */
    public String getLoginMethodDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (isEmailCredential()) {
            desc.append("邮箱");
        } else if (isPhoneCredential()) {
            desc.append("手机号");
        } else {
            desc.append("用户名");
        }
        
        desc.append(" + ").append(loginType.getDisplayName());
        
        return desc.toString();
    }

    /**
     * 验证请求参数的完整性
     * 
     * 业务级别的参数验证，补充注解验证的不足
     * 
     * @return 验证结果消息，null表示验证通过
     */
    public String validateRequest() {
        // 检查登录凭据
        if (credential == null || credential.trim().isEmpty()) {
            return "登录凭据不能为空";
        }
        
        // 根据登录类型验证必需字段
        switch (loginType) {
            case PASSWORD:
                if (password == null || password.trim().isEmpty()) {
                    return "密码登录方式下密码不能为空";
                }
                break;
                
            case SMS_CODE:
                if (!isPhoneCredential()) {
                    return "短信验证码登录必须使用手机号";
                }
                if (smsCode == null || smsCode.trim().isEmpty()) {
                    return "短信验证码不能为空";
                }
                break;
                
            case EMAIL_CODE:
                if (!isEmailCredential()) {
                    return "邮箱验证码登录必须使用邮箱";
                }
                if (emailCode == null || emailCode.trim().isEmpty()) {
                    return "邮箱验证码不能为空";
                }
                break;
        }
        
        return null; // 验证通过
    }

    /**
     * 清除敏感信息
     * 用于日志记录时保护用户隐私
     * 
     * @return 清除敏感信息后的对象
     */
    public UserLoginRequest sanitizeForLogging() {
        UserLoginRequest sanitized = new UserLoginRequest();
        
        // 登录凭据脱敏处理
        if (credential != null) {
            if (isEmailCredential()) {
                sanitized.credential = credential.replaceAll("(.{2}).*(@.*)", "$1***$2");
            } else if (isPhoneCredential()) {
                sanitized.credential = credential.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            } else {
                sanitized.credential = credential.substring(0, Math.min(3, credential.length())) + "***";
            }
        }
        
        // 不包含密码和验证码等敏感信息
        sanitized.loginType = this.loginType;
        sanitized.rememberMe = this.rememberMe;
        sanitized.clientInfo = this.clientInfo;
        sanitized.ipAddress = this.ipAddress;
        
        return sanitized;
    }
}