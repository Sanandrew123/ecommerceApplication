package com.ecommerce.validator.annotation;

/*
 * 文件职责: 字段匹配验证注解，用于验证两个字段值是否相等
 * 
 * 开发心理活动：
 * 1. 为什么需要自定义验证注解？
 *    - Bean Validation标准注解无法处理跨字段验证
 *    - 密码确认等场景需要比较两个字段的值
 *    - 自定义注解提供更好的语义化和复用性
 * 
 * 2. 验证注解设计考虑：
 *    - 支持任意两个字段的值比较
 *    - 可配置字段名，增强灵活性
 *    - 自定义错误消息，提升用户体验
 *    - 支持null值处理，避免空指针异常
 * 
 * 3. 使用场景：
 *    - 密码确认验证
 *    - 邮箱确认验证
 *    - 任何需要两个字段值一致的场景
 * 
 * 包结构设计思路:
 * - 放在validator.annotation包下，专门存放自定义验证注解
 * - 与impl包分离，接口和实现解耦
 * 
 * 命名原因:
 * - FieldMatch明确表达字段匹配的功能
 * - 符合Bean Validation注解命名规范
 * 
 * 依赖关系:
 * - 依赖FieldMatchValidator实现具体验证逻辑
 * - 被DTO类使用，进行跨字段验证
 */

import com.ecommerce.validator.impl.FieldMatchValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 字段匹配验证注解
 * 
 * 功能说明：
 * 1. 验证同一对象中两个字段的值是否相等
 * 2. 支持任意类型的字段比较
 * 3. 处理null值情况，避免异常
 * 4. 提供灵活的配置选项
 * 
 * 使用示例：
 * ```java
 * @FieldMatch(first = "password", second = "confirmPassword", 
 *            message = "密码和确认密码不一致")
 * public class UserRegisterRequest {
 *     private String password;
 *     private String confirmPassword;
 *     // ... other fields
 * }
 * ```
 * 
 * 验证逻辑：
 * 1. 通过反射获取指定字段的值
 * 2. 比较两个字段值是否相等（使用equals方法）
 * 3. 处理null值情况：两个都为null视为相等
 * 4. 返回验证结果和错误消息
 * 
 * 技术特点：
 * 1. 基于Bean Validation规范
 * 2. 支持类级别验证
 * 3. 可重复使用，提高代码复用性
 * 4. 集成Spring Boot验证框架
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Target({ElementType.TYPE})  // 只能用于类级别
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
@Constraint(validatedBy = FieldMatchValidator.class)  // 指定验证器实现类
@Documented  // 包含在JavaDoc中
public @interface FieldMatch {

    /**
     * 错误消息
     * 
     * 默认消息使用占位符，会被实际字段名替换
     * 可以在使用时自定义具体的错误消息
     * 
     * @return 错误消息模板
     */
    String message() default "字段 {first} 和 {second} 的值不匹配";

    /**
     * 验证分组
     * 
     * 用于分组验证，可以在不同场景下应用不同的验证规则
     * 默认为Default组，表示所有验证场景都会执行
     * 
     * @return 验证分组数组
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     * 
     * 用于携带额外的元数据信息
     * 通常用于严重级别定义或其他扩展用途
     * 
     * @return 负载类数组
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 第一个字段名
     * 
     * 指定要比较的第一个字段的名称
     * 必须是被验证对象的有效字段名
     * 
     * @return 第一个字段名
     */
    String first();

    /**
     * 第二个字段名
     * 
     * 指定要比较的第二个字段的名称
     * 必须是被验证对象的有效字段名
     * 
     * @return 第二个字段名
     */
    String second();

    /**
     * 是否忽略大小写
     * 
     * 仅对字符串类型的字段有效
     * true：忽略大小写进行比较
     * false：区分大小写进行比较
     * 
     * @return 是否忽略大小写
     */
    boolean ignoreCase() default false;

    /**
     * 是否允许null值
     * 
     * true：当两个字段都为null时视为匹配
     * false：任何一个字段为null都视为不匹配
     * 
     * @return 是否允许null值
     */
    boolean allowNull() default true;

    /**
     * 支持多个@FieldMatch注解
     * 
     * 当需要在同一个类上使用多个@FieldMatch验证时使用
     * 例如：同时验证密码确认和邮箱确认
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        FieldMatch[] value();
    }
}