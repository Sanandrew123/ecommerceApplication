/*
文件级分析：
- 职责：定义系统中使用的通用常量，避免魔法数字和硬编码字符串
- 包结构考虑：放在constants包下，与其他常量类统一管理
- 命名原因：CommonConstants表示通用常量，语义明确
- 调用关系：被整个系统的各个模块引用，提高代码可维护性

设计思路：
1. 使用接口定义常量，利用接口中字段默认为public static final的特性
2. 按功能分组组织常量，便于查找和维护
3. 常量命名使用全大写+下划线的方式，符合Java规范
4. 为每个常量添加注释说明其用途
*/
package com.ecommerce.constants;

/**
 * 通用常量定义类
 * 
 * 该类定义了系统中使用的各种通用常量，包括：
 * - 分页相关常量
 * - 响应状态码
 * - 字符编码
 * - 日期时间格式
 * - 业务状态标识
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface CommonConstants {
    
    // ======================== 系统基础常量 ========================
    
    /** 系统默认字符编码 */
    String DEFAULT_CHARSET = "UTF-8";
    
    /** 系统默认时区 */
    String DEFAULT_TIMEZONE = "Asia/Shanghai";
    
    /** 默认日期时间格式 */
    String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /** 默认日期格式 */
    String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    /** 默认时间格式 */
    String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    
    // ======================== 分页相关常量 ========================
    
    /** 默认页码 */
    int DEFAULT_PAGE_NUMBER = 1;
    
    /** 默认每页大小 */
    int DEFAULT_PAGE_SIZE = 20;
    
    /** 最大每页大小 */
    int MAX_PAGE_SIZE = 100;
    
    /** 分页参数 - 页码 */
    String PAGE_PARAM = "page";
    
    /** 分页参数 - 每页大小 */
    String SIZE_PARAM = "size";
    
    /** 分页参数 - 排序字段 */
    String SORT_PARAM = "sort";
    
    // ======================== 响应状态码 ========================
    
    /** 成功状态码 */
    int SUCCESS_CODE = 200;
    
    /** 业务异常状态码 */
    int BUSINESS_ERROR_CODE = 400;
    
    /** 未授权状态码 */
    int UNAUTHORIZED_CODE = 401;
    
    /** 禁止访问状态码 */
    int FORBIDDEN_CODE = 403;
    
    /** 资源不存在状态码 */
    int NOT_FOUND_CODE = 404;
    
    /** 服务器内部错误状态码 */
    int INTERNAL_ERROR_CODE = 500;
    
    // ======================== 响应消息 ========================
    
    /** 操作成功消息 */
    String SUCCESS_MESSAGE = "操作成功";
    
    /** 操作失败消息 */
    String FAILURE_MESSAGE = "操作失败";
    
    /** 参数校验失败消息 */
    String VALIDATION_FAILED_MESSAGE = "参数校验失败";
    
    /** 资源不存在消息 */
    String RESOURCE_NOT_FOUND_MESSAGE = "资源不存在";
    
    /** 未授权访问消息 */
    String UNAUTHORIZED_MESSAGE = "未授权访问";
    
    /** 禁止访问消息 */
    String FORBIDDEN_MESSAGE = "禁止访问";
    
    /** 服务器内部错误消息 */
    String INTERNAL_ERROR_MESSAGE = "服务器内部错误";
    
    // ======================== 业务状态常量 ========================
    
    /** 启用状态 */
    int STATUS_ENABLED = 1;
    
    /** 禁用状态 */
    int STATUS_DISABLED = 0;
    
    /** 删除标记 - 已删除 */
    int DELETED_FLAG_TRUE = 1;
    
    /** 删除标记 - 未删除 */
    int DELETED_FLAG_FALSE = 0;
    
    // ======================== HTTP请求头常量 ========================
    
    /** Authorization请求头 */
    String AUTHORIZATION_HEADER = "Authorization";
    
    /** Content-Type请求头 */
    String CONTENT_TYPE_HEADER = "Content-Type";
    
    /** User-Agent请求头 */
    String USER_AGENT_HEADER = "User-Agent";
    
    /** JSON Content-Type */
    String CONTENT_TYPE_JSON = "application/json";
    
    /** 表单 Content-Type */
    String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    
    // ======================== 缓存相关常量 ========================
    
    /** 默认缓存过期时间（秒）- 1小时 */
    long DEFAULT_CACHE_EXPIRE_TIME = 3600L;
    
    /** 短期缓存过期时间（秒）- 5分钟 */
    long SHORT_CACHE_EXPIRE_TIME = 300L;
    
    /** 长期缓存过期时间（秒）- 24小时 */
    long LONG_CACHE_EXPIRE_TIME = 86400L;
    
    // ======================== 正则表达式常量 ========================
    
    /** 手机号正则表达式 */
    String PHONE_REGEX = "^1[3-9]\\d{9}$";
    
    /** 邮箱正则表达式 */
    String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    /** 身份证号正则表达式 */
    String ID_CARD_REGEX = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
    
    /** 用户名正则表达式（4-20位字母数字下划线） */
    String USERNAME_REGEX = "^[a-zA-Z0-9_]{4,20}$";
    
    /** 密码正则表达式（8-20位，至少包含字母和数字） */
    String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$";
}