/*
文件级分析：
- 职责：JWT令牌的生成、解析、验证工具类，为用户认证和授权提供技术支持
- 包结构考虑：位于util包下，作为通用工具类供全系统使用
- 命名原因：JwtUtil清晰表明这是JWT相关的工具类
- 调用关系：被AuthService、SecurityConfig等安全相关组件调用

设计思路：
1. 基于Spring Security和JJWT库实现标准JWT操作
2. 支持Access Token和Refresh Token双令牌机制
3. 提供完整的令牌生命周期管理功能
4. 考虑安全性：密钥管理、令牌过期、签名验证等
5. 支持从HTTP请求中提取令牌的便捷方法
*/
package com.ecommerce.util;

import com.ecommerce.constants.CommonConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT工具类
 * 
 * 提供JWT令牌的完整操作功能，包括：
 * 1. 令牌生成：支持Access Token和Refresh Token
 * 2. 令牌解析：提取用户信息和权限数据
 * 3. 令牌验证：检查有效性、过期时间、签名等
 * 4. 令牌提取：从HTTP请求中提取Bearer令牌
 * 5. 令牌刷新：基于Refresh Token生成新的Access Token
 * 
 * 安全特性：
 * - 使用HMAC-SHA256算法进行签名
 * - 支持配置化的密钥和过期时间
 * - 完整的异常处理和日志记录
 * - 防止令牌重放攻击的时间窗口控制
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JwtUtil {
    
    // ======================== 配置属性 ========================
    
    /**
     * JWT签名密钥
     * 从配置文件中读取，生产环境应使用强密钥
     */
    @Value("${jwt.secret:ecommerce-jwt-secret-key-for-token-signing-must-be-at-least-256-bits}")
    private String jwtSecret;
    
    /**
     * Access Token过期时间（秒）
     * 默认2小时
     */
    @Value("${jwt.access-token-expiration:7200}")
    private Long accessTokenExpiration;
    
    /**
     * Refresh Token过期时间（秒）
     * 默认7天
     */
    @Value("${jwt.refresh-token-expiration:604800}")
    private Long refreshTokenExpiration;
    
    /**
     * JWT发行者标识
     */
    @Value("${jwt.issuer:ecommerce-system}")
    private String issuer;
    
    /**
     * 令牌前缀
     */
    private static final String TOKEN_PREFIX = "Bearer ";
    
    /**
     * Authorization请求头名称
     */
    private static final String HEADER_NAME = "Authorization";
    
    // ======================== 令牌生成方法 ========================
    
    /**
     * 生成Access Token
     * 
     * @param userDetails Spring Security用户详情
     * @return JWT Access Token
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails.getUsername(), null, userDetails.getAuthorities());
    }
    
    /**
     * 生成Access Token
     * 
     * @param username 用户名
     * @param userId 用户ID
     * @param authorities 用户权限
     * @return JWT Access Token
     */
    public String generateAccessToken(String username, Long userId, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        
        if (userId != null) {
            claims.put("userId", userId);
        }
        
        if (authorities != null && !authorities.isEmpty()) {
            claims.put("authorities", authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }
        
        return createToken(claims, username, accessTokenExpiration);
    }
    
    /**
     * 生成Refresh Token
     * 
     * @param username 用户名
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String username) {
        return generateRefreshToken(username, null);
    }
    
    /**
     * 生成Refresh Token
     * 
     * @param username 用户名
     * @param userId 用户ID
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        if (userId != null) {
            claims.put("userId", userId);
        }
        
        return createToken(claims, username, refreshTokenExpiration);
    }
    
    /**
     * 创建JWT令牌的核心方法
     * 
     * @param claims 声明信息
     * @param subject 主题（通常是用户名）
     * @param expirationSeconds 过期时间（秒）
     * @return JWT令牌字符串
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // ======================== 令牌解析方法 ========================
    
    /**
     * 从令牌中提取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中提取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            return userId != null ? Long.valueOf(userId.toString()) : null;
        });
    }
    
    /**
     * 从令牌中提取权限列表
     * 
     * @param token JWT令牌
     * @return 权限列表
     */
    @SuppressWarnings("unchecked")
    public Collection<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> {
            Object authorities = claims.get("authorities");
            return authorities instanceof Collection ? (Collection<String>) authorities : null;
        });
    }
    
    /**
     * 从令牌中提取令牌类型
     * 
     * @param token JWT令牌
     * @return 令牌类型（access/refresh）
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("type"));
    }
    
    /**
     * 从令牌中提取过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 从令牌中提取发行时间
     * 
     * @param token JWT令牌
     * @return 发行时间
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }
    
    /**
     * 通用的声明提取方法
     * 
     * @param token JWT令牌
     * @param claimsResolver 声明解析器
     * @param <T> 返回类型
     * @return 提取的声明值
     */
    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.resolve(claims);
    }
    
    /**
     * 提取所有声明
     * 
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("JWT令牌解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("无效的JWT令牌", e);
        }
    }
    
    // ======================== 令牌验证方法 ========================
    
    /**
     * 验证令牌是否有效
     * 
     * @param token JWT令牌
     * @param userDetails 用户详情
     * @return true表示有效，false表示无效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证令牌是否有效（仅验证令牌本身）
     * 
     * @param token JWT令牌
     * @return true表示有效，false表示无效
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查令牌是否已过期
     * 
     * @param token JWT令牌
     * @return true表示已过期，false表示未过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }
    
    /**
     * 检查令牌是否为Access Token
     * 
     * @param token JWT令牌
     * @return true表示是Access Token
     */
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }
    
    /**
     * 检查令牌是否为Refresh Token
     * 
     * @param token JWT令牌
     * @return true表示是Refresh Token
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }
    
    // ======================== 令牌提取方法 ========================
    
    /**
     * 从HTTP请求中提取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌字符串，如果没有则返回null
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_NAME);
        return extractTokenFromHeader(authHeader);
    }
    
    /**
     * 从Authorization头中提取JWT令牌
     * 
     * @param authHeader Authorization头值
     * @return JWT令牌字符串，如果没有则返回null
     */
    public String extractTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
    
    // ======================== 令牌刷新方法 ========================
    
    /**
     * 基于Refresh Token生成新的Access Token
     * 
     * @param refreshToken Refresh Token
     * @return 新的Access Token
     * @throws IllegalArgumentException 如果Refresh Token无效
     */
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new IllegalArgumentException("无效的Refresh Token");
        }
        
        if (!isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("令牌类型错误，需要Refresh Token");
        }
        
        String username = extractUsername(refreshToken);
        Long userId = extractUserId(refreshToken);
        
        // 生成新的Access Token（不包含权限，需要重新查询）
        return generateAccessToken(username, userId, null);
    }
    
    // ======================== 工具方法 ========================
    
    /**
     * 获取签名密钥
     * 
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 获取令牌剩余有效时间（秒）
     * 
     * @param token JWT令牌
     * @return 剩余有效时间（秒）
     */
    public long getRemainingValiditySeconds(String token) {
        Date expiration = extractExpiration(token);
        long now = System.currentTimeMillis();
        return Math.max(0, (expiration.getTime() - now) / 1000);
    }
    
    /**
     * 获取令牌信息的可读格式
     * 
     * @param token JWT令牌
     * @return 令牌信息Map
     */
    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("username", extractUsername(token));
            info.put("userId", extractUserId(token));
            info.put("type", extractTokenType(token));
            info.put("issuedAt", extractIssuedAt(token));
            info.put("expiration", extractExpiration(token));
            info.put("remainingSeconds", getRemainingValiditySeconds(token));
            info.put("expired", isTokenExpired(token));
            info.put("valid", validateToken(token));
        } catch (Exception e) {
            info.put("error", e.getMessage());
            info.put("valid", false);
        }
        return info;
    }
    
    /**
     * 将Date转换为LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime对象
     */
    public LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * 将LocalDateTime转换为Date
     * 
     * @param localDateTime LocalDateTime对象
     * @return Date对象
     */
    public Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    // ======================== 内部接口 ========================
    
    /**
     * 声明解析器函数式接口
     * 
     * @param <T> 返回类型
     */
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}