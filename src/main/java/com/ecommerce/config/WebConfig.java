/*
文件级分析：
- 职责：Web层配置类，配置Web相关的组件和行为
- 包结构考虑：位于config包下，统一管理配置类
- 命名原因：WebConfig明确表示这是Web配置类
- 调用关系：被Spring Boot启动时加载，配置Web层的各种组件

设计思路：
1. 配置CORS跨域支持，解决前后端分离的跨域问题
2. 配置静态资源映射，支持文件上传后的访问
3. 配置JSON序列化行为，统一数据格式
4. 配置国际化支持，为多语言做准备
5. 配置Web相关的拦截器和过滤器
*/
package com.ecommerce.config;

import com.ecommerce.constants.CommonConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Web配置类
 * 
 * 负责配置Web层的各种组件，包括：
 * 1. CORS跨域配置 - 支持前后端分离架构
 * 2. 静态资源映射 - 支持文件上传和访问
 * 3. JSON序列化配置 - 统一数据格式
 * 4. HTTP消息转换器 - 自定义请求响应处理
 * 
 * 设计原则：
 * - 安全优先：CORS配置要严格控制允许的域名
 * - 性能考虑：静态资源要设置合理的缓存策略
 * - 一致性：所有API的数据格式要保持一致
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /** 文件上传路径 */
    @Value("${app.file.upload-path:./uploads/}")
    private String uploadPath;
    
    // ======================== CORS跨域配置 ========================
    
    /**
     * CORS跨域配置
     * 
     * 在前后端分离的架构中，前端和后端往往部署在不同的域名或端口上，
     * 需要配置CORS来解决浏览器的跨域限制。
     * 
     * 安全考虑：
     * - 生产环境应该明确指定允许的域名，不要使用通配符
     * - 控制允许的HTTP方法和请求头
     * - 合理设置预检请求的缓存时间
     * 
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的域名 - 开发环境允许所有域名，生产环境应该明确指定
        configuration.addAllowedOriginPattern("*");
        
        // 允许的HTTP方法
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        
        // 允许的请求头
        configuration.addAllowedHeader("*");
        
        // 允许发送认证信息（如Cookie、Authorization header）
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        // 暴露给前端的响应头
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Content-Type");
        configuration.addExposedHeader("X-Total-Count");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    // ======================== 静态资源配置 ========================
    
    /**
     * 静态资源映射配置
     * 
     * 配置文件上传后的访问路径，使上传的文件可以通过HTTP访问。
     * 这对于图片、文档等文件的展示非常重要。
     * 
     * 性能考虑：
     * - 设置合理的缓存时间，减轻服务器压力
     * - 考虑使用CDN来分发静态资源
     * 
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件的访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(3600 * 24 * 7); // 缓存7天
        
        // 映射静态资源（CSS、JS、图片等）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600 * 24 * 30); // 缓存30天
    }
    
    // ======================== JSON序列化配置 ========================
    
    /**
     * 自定义ObjectMapper配置
     * 
     * 统一配置JSON序列化和反序列化的行为，确保：
     * 1. 日期时间格式的一致性
     * 2. 字段命名策略的统一性
     * 3. null值的处理策略
     * 4. 时区的正确处理
     * 
     * @return 配置好的ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 日期格式配置
        mapper.setDateFormat(new SimpleDateFormat(CommonConstants.DEFAULT_DATETIME_FORMAT));
        mapper.setTimeZone(TimeZone.getTimeZone(CommonConstants.DEFAULT_TIMEZONE));
        
        // 字段命名策略 - 使用下划线命名
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // 序列化配置
        mapper.getSerializationConfig()
                .withoutFeatures(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
                );
        
        // 反序列化配置
        mapper.getDeserializationConfig()
                .withoutFeatures(
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
                );
        
        return mapper;
    }
    
    /**
     * 配置HTTP消息转换器
     * 
     * 使用自定义的ObjectMapper来处理HTTP请求和响应的JSON转换，
     * 确保所有接口的数据格式保持一致。
     * 
     * @param converters HTTP消息转换器列表
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        converters.add(0, converter); // 添加到第一个位置，确保优先使用
    }
}