/*
文件级分析：
- 职责：用户登录请求DTO，封装登录时前端传递的参数
- 包结构考虑：位于dto.request.auth包下，按功能模块和数据流向组织
- 命名原因：LoginRequest明确表示这是登录请求的数据传输对象
- 调用关系：被Controller接收，传递给Service进行业务处理

设计思路：
1. 支持多种登录方式：用户名、邮箱、手机号
2. 使用JSR-303注解进行参数校验
3. 包含记住我功能，提升用户体验
4. 密码字段使用@JsonProperty(access = WRITE_ONLY)，只允许写入不允许读取
5. 提供便捷的登录方式判断方法
*/
package com.ecommerce.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求DTO
 * 
 * 封装用户登录时提交的参数，支持多种登录方式：
 * 1. 用户名 + 密码
 * 2. 邮箱 + 密码  
 * 3. 手机号 + 密码
 * 
 * 系统会自动识别登录凭证的类型（用户名/邮箱/手机号），
 * 无需前端明确指定登录方式。
 * 
 * 请求示例：
 * ```json
 * {
 *   "loginId": "john_doe",
 *   "password": "your_password",
 *   "rememberMe": true
 * }
 * ```
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Schema(description = "用户登录请求")
public class LoginRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 登录凭证
     * 可以是用户名、邮箱地址或手机号码
     * 系统会自动识别类型并进行相应的验证
     */
    @Schema(description = "登录凭证（用户名/邮箱/手机号）", example = "john_doe")
    @NotBlank(message = "登录凭证不能为空")
    @Size(min = 4, max = 100, message = "登录凭证长度必须在4-100字符之间")
    private String loginId;
    
    /**
     * 登录密码
     * 使用JsonProperty控制序列化行为，只允许写入不允许读取
     * 避免在日志或响应中泄露密码信息
     */
    @Schema(description = "登录密码", example = "your_password")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50字符之间")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    /**
     * 记住我选项
     * 如果为true，系统会延长登录状态的有效期
     * 通常用于设置更长的JWT过期时间或Cookie有效期
     */
    @Schema(description = "是否记住登录状态", example = "true")
    private Boolean rememberMe = false;
    
    /**
     * 验证码（可选）
     * 在需要额外安全验证的场景下使用，如：
     * - 多次登录失败后要求输入验证码
     * - 异地登录检测后要求验证码
     * - 敏感操作前的二次验证
     */
    @Schema(description = "验证码（可选）", example = "1234")
    @Size(max = 10, message = "验证码长度不能超过10个字符")
    private String captcha;
    
    /**
     * 验证码令牌（可选）
     * 与验证码配合使用，用于验证码的会话管理
     * 防止验证码被重复使用或跨会话使用
     */
    @Schema(description = "验证码令牌（可选）")
    private String captchaToken;
    
    // ======================== 业务方法 ========================
    
    /**
     * 判断登录凭证是否为邮箱格式
     * 
     * @return true表示是邮箱格式
     */
    public boolean isEmailLogin() {
        if (loginId == null || loginId.trim().isEmpty()) {
            return false;
        }
        // 简单的邮箱格式检查
        return loginId.contains("@") && loginId.contains(".");
    }
    
    /**
     * 判断登录凭证是否为手机号格式
     * 
     * @return true表示是手机号格式
     */
    public boolean isPhoneLogin() {
        if (loginId == null || loginId.trim().isEmpty()) {
            return false;
        }
        // 检查是否为中国大陆手机号格式
        return loginId.matches("^1[3-9]\\d{9}$");
    }
    
    /**
     * 判断登录凭证是否为用户名格式
     * 
     * @return true表示是用户名格式
     */
    public boolean isUsernameLogin() {
        return !isEmailLogin() && !isPhoneLogin();
    }
    
    /**
     * 获取登录方式的描述
     * 
     * @return 登录方式描述
     */
    public String getLoginType() {
        if (isEmailLogin()) {
            return "邮箱";
        } else if (isPhoneLogin()) {
            return "手机号";
        } else {
            return "用户名";
        }
    }
    
    /**
     * 是否需要验证码
     * 
     * @return true表示需要验证码
     */
    public boolean needsCaptcha() {
        return captcha != null && !captcha.trim().isEmpty();
    }
    
    /**
     * 获取清理后的登录凭证
     * 去除前后空格，统一转换为小写（邮箱情况下）
     * 
     * @return 清理后的登录凭证
     */
    public String getCleanedLoginId() {
        if (loginId == null) {
            return null;
        }
        
        String cleaned = loginId.trim();
        
        // 如果是邮箱，转换为小写
        if (isEmailLogin()) {
            cleaned = cleaned.toLowerCase();
        }
        
        return cleaned;
    }
}