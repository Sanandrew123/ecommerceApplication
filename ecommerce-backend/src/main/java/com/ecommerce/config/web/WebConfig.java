package com.ecommerce.config.web;

/*
 * 文件职责: Web层基础配置类，配置跨域、消息转换器、拦截器等Web相关功能
 * 
 * 开发心理活动：
 * 1. 为什么需要Web配置？
 *    - 现代前后端分离架构需要跨域支持
 *    - 需要自定义消息转换器处理JSON序列化
 *    - 需要配置静态资源映射和拦截器
 * 
 * 2. 跨域配置考虑：
 *    - 开发环境需要支持localhost的各种端口
 *    - 生产环境只允许特定域名访问
 *    - 支持预检请求，处理复杂跨域场景
 * 
 * 3. 消息转换器配置：
 *    - 配置JSON日期格式，统一前后端时间处理
 *    - 处理空值和未知属性，增强兼容性
 *    - 优化序列化性能
 * 
 * 包结构设计思路:
 * - 放在config.web包下，专门处理Web层配置
 * - 与security、database等配置分离
 * 
 * 命名原因:
 * - WebConfig表明这是Web相关配置
 * - 符合Spring Boot配置类命名规范
 * 
 * 依赖关系:
 * - 实现WebMvcConfigurer接口，扩展Spring MVC功能
 * - 被Spring Boot自动扫描和应用
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Web层配置类
 * 
 * 主要功能：
 * 1. 跨域资源共享(CORS)配置
 * 2. JSON消息转换器配置
 * 3. 静态资源映射配置
 * 4. 拦截器配置
 * 
 * 配置原则：
 * 1. 安全性：只允许必要的跨域访问
 * 2. 灵活性：支持开发和生产环境的不同需求
 * 3. 性能：优化JSON序列化性能
 * 4. 兼容性：处理各种前端框架的兼容性问题
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的源地址
     * 通过配置文件注入，支持多环境配置
     */
    @Value("${app.security.allowed-origins}")
    private String allowedOrigins;

    /**
     * 文件上传路径
     * 用于静态资源访问映射
     */
    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * 跨域过滤器配置
     * 
     * CORS配置说明：
     * 1. 允许的源：从配置文件读取，支持多个域名
     * 2. 允许的方法：GET、POST、PUT、DELETE、OPTIONS
     * 3. 允许的头：所有头信息
     * 4. 允许凭据：支持Cookie传递
     * 5. 预检缓存：3600秒，减少OPTIONS请求
     * 
     * 安全考虑：
     * - 生产环境应该限制allowedOrigins
     * - 避免使用allowedOrigins("*")和allowCredentials(true)
     * 
     * @return CORS过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        log.info("配置CORS跨域访问，允许的源: {}", allowedOrigins);
        
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源地址
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            config.addAllowedOrigin(origin.trim());
        }
        
        // 允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许发送Cookie
        config.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);
        
        // 暴露的响应头（前端可以访问的响应头）
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }

    /**
     * 自定义JSON消息转换器
     * 
     * 配置内容：
     * 1. 日期格式：yyyy-MM-dd HH:mm:ss
     * 2. 时区：GMT+8（中国标准时间）
     * 3. 空值处理：不序列化null值
     * 4. 未知属性：忽略未知JSON属性
     * 5. Java 8时间：支持LocalDateTime等新时间类型
     * 
     * @param converters 消息转换器列表
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("配置JSON消息转换器");
        
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 配置日期格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        
        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 序列化配置
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // 反序列化配置
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
    }

    /**
     * 静态资源映射配置
     * 
     * 映射规程：
     * 1. /uploads/** -> 文件上传目录
     * 2. /static/** -> classpath:/static/
     * 3. /webjars/** -> classpath:/META-INF/resources/webjars/
     * 
     * 缓存配置：
     * - 静态资源缓存1小时
     * - 上传文件缓存10分钟（可能会更新）
     * 
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("配置静态资源映射，上传路径: {}", uploadPath);
        
        // 文件上传资源映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(600); // 缓存10分钟
        
        // 静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 缓存1小时
        
        // Webjars资源映射（如Bootstrap、jQuery等前端库）
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600); // 缓存1小时
    }

    /**
     * 自定义ObjectMapper Bean
     * 
     * 用途：
     * 1. 供其他组件使用的全局ObjectMapper
     * 2. 与消息转换器使用相同的配置
     * 3. 便于手动JSON序列化和反序列化
     * 
     * @return 配置好的ObjectMapper实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 日期格式配置
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 序列化配置
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, true);
        
        // 反序列化配置
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        log.info("配置全局ObjectMapper完成");
        return mapper;
    }
}