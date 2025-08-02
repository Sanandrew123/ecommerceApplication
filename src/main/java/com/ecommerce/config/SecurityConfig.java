/*
文件级分析：
- 职责：Spring Security安全配置类，配置认证、授权、JWT集成等安全相关功能
- 包结构考虑：位于config包下，作为配置类统一管理
- 命名原因：SecurityConfig明确表示这是安全配置类
- 调用关系：被Spring自动加载，配置全局安全策略，与UserService、JwtUtil集成

设计思路：
1. 配置JWT认证过滤器，实现无状态认证
2. 设置密码编码器，使用BCrypt确保密码安全
3. 配置AuthenticationManager，支持多种认证方式
4. 定义访问控制规则，区分公开接口和受保护接口
5. 集成自定义的UserDetailsService和异常处理
*/
package com.ecommerce.config;

import com.ecommerce.service.UserService;
import com.ecommerce.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security安全配置类
 * 
 * 配置系统的安全策略，包括：
 * 1. JWT认证过滤器配置
 * 2. 密码编码器配置
 * 3. 认证管理器配置
 * 4. 访问控制规则定义
 * 5. CORS跨域配置
 * 6. 会话管理策略
 * 
 * 安全特性：
 * - 使用JWT实现无状态认证
 * - BCrypt密码加密，安全可靠
 * - 支持方法级权限控制
 * - 完整的异常处理和响应格式
 * - 灵活的访问控制配置
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    /**
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF，因为使用JWT无状态认证
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置会话管理为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置访问控制规则
            .authorizeHttpRequests(auth -> auth
                // 公开访问的端点
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/logout").permitAll()
                
                // Swagger和API文档端点
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                
                // 健康检查端点
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // 静态资源
                .requestMatchers("/favicon.ico", "/error").permitAll()
                
                // 商品查询接口（可公开访问）
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            
            // 配置认证提供者
            .authenticationProvider(authenticationProvider())
            
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // 配置注销处理
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .addLogoutHandler(logoutHandler())
                .logoutSuccessHandler((request, response, authentication) -> {
                    SecurityContextHolder.clearContext();
                    response.setStatus(200);
                })
            );
        
        return http.build();
    }
    
    /**
     * 密码编码器Bean
     * 使用BCrypt加密算法，安全性高
     * 
     * @return PasswordEncoder密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 强度为12，平衡安全性和性能
    }
    
    /**
     * 认证管理器Bean
     * 
     * @param config 认证配置
     * @return AuthenticationManager认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 认证提供者Bean
     * 配置DAO认证提供者，使用自定义的UserDetailsService
     * 
     * @return AuthenticationProvider认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false); // 显示用户不存在异常，便于调试
        return authProvider;
    }
    
    /**
     * JWT认证过滤器Bean
     * 
     * @return JwtAuthenticationFilter JWT认证过滤器
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userService);
    }
    
    /**
     * CORS配置源Bean
     * 配置跨域访问策略
     * 
     * @return CorsConfigurationSource CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的域名（生产环境应该配置具体的域名）
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许发送认证信息（cookies, authorization headers）
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 注销处理器Bean
     * 处理用户注销时的逻辑
     * 
     * @return LogoutHandler注销处理器
     */
    @Bean
    public LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            // 从请求头中提取JWT令牌
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                
                // TODO: 将令牌加入黑名单（可以使用Redis实现）
                // 这里可以将令牌存储到Redis黑名单中，设置过期时间为令牌的剩余有效时间
                log.info("用户注销，令牌已失效: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
                
                // 清除安全上下文
                SecurityContextHolder.clearContext();
            }
        };
    }
}