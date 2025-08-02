/*
文件级分析：
- 职责：JWT认证过滤器，拦截HTTP请求，验证JWT令牌并设置安全上下文
- 包结构考虑：位于config包下，作为安全配置的一部分
- 命名原因：JwtAuthenticationFilter明确表示这是JWT认证过滤器
- 调用关系：被SecurityConfig配置使用，调用JwtUtil和UserService进行令牌验证

设计思路：
1. 继承OncePerRequestFilter，确保每个请求只执行一次过滤
2. 从HTTP请求头中提取JWT令牌
3. 验证令牌的有效性和完整性
4. 根据令牌信息加载用户详情并设置安全上下文
5. 处理各种异常情况，提供友好的错误响应
*/
package com.ecommerce.config;

import com.ecommerce.service.UserService;
import com.ecommerce.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器
 * 
 * 拦截HTTP请求，执行JWT令牌认证，包括：
 * 1. 从请求头中提取JWT令牌
 * 2. 验证令牌的有效性和完整性
 * 3. 解析令牌获取用户信息
 * 4. 加载用户详情并验证用户状态
 * 5. 设置Spring Security安全上下文
 * 6. 处理认证异常并返回适当的错误响应
 * 
 * 过滤器特性：
 * - 只对需要认证的请求进行处理
 * - 支持Bearer Token格式的认证头
 * - 完整的异常处理和错误响应
 * - 记录详细的认证日志便于调试
 * - 性能优化，避免重复认证
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 执行过滤器逻辑
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 跳过不需要认证的请求
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 提取JWT令牌
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateWithJwt(jwt, request);
            }
            
        } catch (Exception e) {
            log.error("JWT认证过程中发生异常: {}", e.getMessage());
            handleAuthenticationException(response, e);
            return;
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 判断是否应该跳过认证
     * 对于公开端点，跳过JWT认证
     * 
     * @param request HTTP请求
     * @return true表示跳过认证，false表示需要认证
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 公开的认证端点
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        
        // Swagger和API文档
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || 
            path.startsWith("/swagger-resources") || path.startsWith("/webjars")) {
            return true;
        }
        
        // 健康检查端点
        if (path.startsWith("/actuator/")) {
            return true;
        }
        
        // 静态资源
        if (path.equals("/favicon.ico") || path.equals("/error")) {
            return true;
        }
        
        // 商品查询接口（GET请求可公开访问）
        if ("GET".equals(method) && (path.startsWith("/api/products") || path.startsWith("/api/categories"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从HTTP请求中提取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌字符串，如果没有则返回null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
    
    /**
     * 使用JWT令牌进行认证
     * 
     * @param jwt JWT令牌
     * @param request HTTP请求
     */
    private void authenticateWithJwt(String jwt, HttpServletRequest request) {
        try {
            // 验证令牌有效性
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("无效的JWT令牌: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
                return;
            }
            
            // 检查令牌类型
            if (!jwtUtil.isAccessToken(jwt)) {
                log.warn("令牌类型错误，需要Access Token: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
                return;
            }
            
            // 提取用户名
            String username = jwtUtil.extractUsername(jwt);
            if (!StringUtils.hasText(username)) {
                log.warn("无法从令牌中提取用户名: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
                return;
            }
            
            // 加载用户详情
            UserDetails userDetails;
            try {
                userDetails = userService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                log.warn("令牌中的用户不存在: {}", username);
                return;
            }
            
            // 验证令牌与用户的匹配性
            if (!jwtUtil.validateToken(jwt, userDetails)) {
                log.warn("令牌与用户不匹配: {}", username);
                return;
            }
            
            // 检查用户是否启用
            if (!userDetails.isEnabled()) {
                log.warn("用户已被禁用: {}", username);
                return;
            }
            
            // 创建认证对象并设置到安全上下文
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            log.debug("JWT认证成功: {}", username);
            
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * 处理认证异常
     * 
     * @param response HTTP响应
     * @param exception 异常对象
     * @throws IOException IO异常
     */
    private void handleAuthenticationException(HttpServletResponse response, Exception exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 401);
        errorResponse.put("message", "认证失败");
        errorResponse.put("detail", exception.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", "");
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
    
    /**
     * 检查是否应该应用此过滤器
     * 
     * @param request HTTP请求
     * @return true表示不应用过滤器，false表示应用过滤器
     * @throws ServletException Servlet异常
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 对OPTIONS请求跳过过滤器（CORS预检请求）
        return "OPTIONS".equals(request.getMethod());
    }
}