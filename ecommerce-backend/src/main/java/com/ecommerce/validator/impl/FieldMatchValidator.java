package com.ecommerce.validator.impl;

/*
 * 文件职责: FieldMatch注解的验证器实现类，执行字段匹配验证逻辑
 * 
 * 开发心理活动：
 * 1. 验证器实现的核心逻辑：
 *    - 通过反射获取字段值，处理各种异常情况
 *    - 实现null安全的值比较逻辑
 *    - 支持字符串大小写忽略比较
 *    - 提供详细的错误信息和日志记录
 * 
 * 2. 异常处理考虑：
 *    - 字段不存在的情况处理
 *    - 字段访问权限问题处理
 *    - 反射调用异常的统一处理
 *    - 验证失败时的错误信息构建
 * 
 * 3. 性能优化考虑：
 *    - 缓存反射获取的Field对象
 *    - 减少不必要的字符串操作
 *    - 使用高效的值比较算法
 * 
 * 包结构设计思路:
 * - 放在validator.impl包下，实现具体的验证逻辑
 * - 与annotation包分离，符合接口实现分离原则
 * 
 * 命名原因:
 * - FieldMatchValidator明确表达这是FieldMatch注解的验证器
 * - 符合Validator后缀命名规范
 * 
 * 依赖关系:
 * - 实现ConstraintValidator接口，集成Bean Validation框架
 * - 被FieldMatch注解引用，执行验证逻辑
 */

import com.ecommerce.validator.annotation.FieldMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * 字段匹配验证器实现类
 * 
 * 实现说明：
 * 1. 实现ConstraintValidator接口，集成Bean Validation框架
 * 2. 通过反射机制获取对象字段值
 * 3. 实现灵活的值比较逻辑
 * 4. 提供详细的验证错误信息
 * 
 * 验证流程：
 * 1. 初始化阶段：解析注解参数，准备验证环境
 * 2. 验证阶段：获取字段值，执行比较逻辑
 * 3. 结果处理：返回验证结果，构建错误消息
 * 
 * 技术特点：
 * 1. 反射安全：处理字段访问异常和权限问题
 * 2. null安全：正确处理null值比较
 * 3. 类型通用：支持任意类型的字段比较
 * 4. 配置灵活：支持大小写忽略、null处理等选项
 * 
 * 性能考虑：
 * 1. 反射调用优化：使用setAccessible提高访问效率
 * 2. 异常处理最小化：避免频繁的异常创建和处理
 * 3. 字符串比较优化：根据配置选择合适的比较方法
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    /**
     * 第一个字段名
     */
    private String firstFieldName;

    /**
     * 第二个字段名
     */
    private String secondFieldName;

    /**
     * 是否忽略大小写
     */
    private boolean ignoreCase;

    /**
     * 是否允许null值
     */
    private boolean allowNull;

    /**
     * 错误消息模板
     */
    private String messageTemplate;

    /**
     * 初始化验证器
     * 
     * 在验证器创建时调用，用于解析注解参数
     * 保存验证所需的配置信息
     * 
     * @param constraintAnnotation FieldMatch注解实例
     */
    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
        this.ignoreCase = constraintAnnotation.ignoreCase();
        this.allowNull = constraintAnnotation.allowNull();
        this.messageTemplate = constraintAnnotation.message();
        
        log.debug("初始化FieldMatch验证器: first={}, second={}, ignoreCase={}, allowNull={}", 
                 firstFieldName, secondFieldName, ignoreCase, allowNull);
    }

    /**
     * 执行字段匹配验证
     * 
     * 验证逻辑：
     * 1. 通过反射获取两个字段的值
     * 2. 根据配置执行值比较
     * 3. 处理各种边界情况
     * 4. 返回验证结果
     * 
     * @param value 要验证的对象
     * @param context 验证上下文
     * @return true-验证通过，false-验证失败
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 如果验证对象为null，跳过验证（由@NotNull等注解处理）
        if (value == null) {
            return true;
        }

        try {
            // 获取第一个字段的值
            Object firstValue = getFieldValue(value, firstFieldName);
            
            // 获取第二个字段的值
            Object secondValue = getFieldValue(value, secondFieldName);
            
            // 执行字段值比较
            boolean isMatch = compareValues(firstValue, secondValue);
            
            // 如果验证失败，构建自定义错误消息
            if (!isMatch) {
                buildErrorMessage(context, firstValue, secondValue);
            }
            
            log.debug("字段匹配验证结果: {}={}, {}={}, match={}", 
                     firstFieldName, firstValue, secondFieldName, secondValue, isMatch);
            
            return isMatch;
            
        } catch (Exception e) {
            log.error("字段匹配验证异常: object={}, first={}, second={}", 
                     value.getClass().getSimpleName(), firstFieldName, secondFieldName, e);
            
            // 验证过程中出现异常，视为验证失败
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("字段验证过程中发生错误")
                   .addConstraintViolation();
            return false;
        }
    }

    /**
     * 通过反射获取字段值
     * 
     * 处理过程：
     * 1. 获取字段对象
     * 2. 设置字段可访问（处理private字段）
     * 3. 获取字段值
     * 4. 处理异常情况
     * 
     * @param object 目标对象
     * @param fieldName 字段名
     * @return 字段值
     * @throws Exception 反射操作异常
     */
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();
        Field field = null;
        
        // 在类层次结构中查找字段（支持继承）
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        
        // 字段不存在
        if (field == null) {
            throw new NoSuchFieldException("字段 '" + fieldName + "' 不存在");
        }
        
        // 设置字段可访问（处理private字段）
        field.setAccessible(true);
        
        // 获取字段值
        return field.get(object);
    }

    /**
     * 比较两个字段值
     * 
     * 比较逻辑：
     * 1. null值处理：根据allowNull配置决定处理方式
     * 2. 字符串比较：根据ignoreCase配置选择比较方法
     * 3. 其他类型：使用equals方法比较
     * 
     * @param firstValue 第一个字段值
     * @param secondValue 第二个字段值
     * @return true-值相等，false-值不等
     */
    private boolean compareValues(Object firstValue, Object secondValue) {
        // 处理null值情况
        if (firstValue == null && secondValue == null) {
            return allowNull; // 两个都为null时，根据配置决定
        }
        
        if (firstValue == null || secondValue == null) {
            return false; // 一个为null一个不为null，视为不匹配
        }
        
        // 字符串类型的特殊处理
        if (firstValue instanceof String && secondValue instanceof String) {
            String first = (String) firstValue;
            String second = (String) secondValue;
            
            if (ignoreCase) {
                return first.equalsIgnoreCase(second);
            } else {
                return first.equals(second);
            }
        }
        
        // 其他类型使用equals方法比较
        return Objects.equals(firstValue, secondValue);
    }

    /**
     * 构建自定义错误消息
     * 
     * 错误消息功能：
     * 1. 禁用默认错误消息
     * 2. 构建包含字段名的自定义消息
     * 3. 支持消息模板参数替换
     * 4. 提供用户友好的错误提示
     * 
     * @param context 验证上下文
     * @param firstValue 第一个字段值
     * @param secondValue 第二个字段值
     */
    private void buildErrorMessage(ConstraintValidatorContext context, 
                                  Object firstValue, Object secondValue) {
        // 禁用默认的约束违反消息
        context.disableDefaultConstraintViolation();
        
        // 构建自定义错误消息
        String errorMessage = messageTemplate
            .replace("{first}", firstFieldName)
            .replace("{second}", secondFieldName);
        
        // 添加字段值信息（用于调试，生产环境可能需要屏蔽敏感信息）
        if (log.isDebugEnabled()) {
            errorMessage += String.format(" (实际值: %s != %s)", 
                                        safeToString(firstValue), 
                                        safeToString(secondValue));
        }
        
        // 创建新的约束违反
        context.buildConstraintViolationWithTemplate(errorMessage)
               .addConstraintViolation();
    }

    /**
     * 安全的toString方法
     * 
     * 处理对象转字符串时的异常情况
     * 避免敏感信息泄露
     * 
     * @param value 要转换的值
     * @return 安全的字符串表示
     */
    private String safeToString(Object value) {
        if (value == null) {
            return "null";
        }
        
        try {
            // 对于敏感信息（如密码），只显示长度
            if (value instanceof String) {
                String str = (String) value;
                if (firstFieldName.toLowerCase().contains("password") || 
                    secondFieldName.toLowerCase().contains("password")) {
                    return "[" + str.length() + " 个字符]";
                }
            }
            
            return value.toString();
        } catch (Exception e) {
            return "[toString() 异常]";
        }
    }
}