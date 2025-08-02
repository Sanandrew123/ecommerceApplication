/*
文件级分析：
- 职责：用户业务逻辑服务层，处理用户注册、登录、信息管理等核心业务
- 包结构考虑：位于service包下，作为业务层组件提供业务逻辑处理
- 命名原因：UserService清晰表明这是用户相关的业务服务
- 调用关系：被Controller调用，调用UserRepository进行数据操作，使用JwtUtil处理令牌

设计思路：
1. 实现完整的用户生命周期管理：注册、登录、信息更新、状态管理等
2. 集成Spring Security进行密码加密和用户认证
3. 支持多种登录方式：用户名、邮箱、手机号
4. 提供JWT令牌的生成和管理功能
5. 实现用户状态控制和安全验证机制
*/
package com.ecommerce.service;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.response.auth.LoginResponse;
import com.ecommerce.entity.User;
import com.ecommerce.enums.UserStatus;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户业务服务类
 * 
 * 提供完整的用户管理功能，包括：
 * 1. 用户注册：支持用户名、邮箱、手机号注册
 * 2. 用户登录：多方式登录，JWT令牌认证
 * 3. 用户信息管理：查询、更新、状态控制
 * 4. 安全认证：密码加密、登录验证、权限检查
 * 5. 会话管理：令牌生成、刷新、注销
 * 
 * 安全特性：
 * - 密码强度验证和BCrypt加密
 * - 登录失败次数限制和账号锁定
 * - 支持多种登录方式的安全验证
 * - 完整的用户状态管理和权限控制
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    // ======================== Spring Security集成 ========================
    
    /**
     * Spring Security UserDetailsService接口实现
     * 支持用户名、邮箱、手机号多种方式查询用户
     * 
     * @param username 用户标识（用户名、邮箱或手机号）
     * @return UserDetails用户详情
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户详情: {}", username);
        
        User user = findUserByLoginIdentifier(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        log.debug("用户详情加载成功: {}, 状态: {}", user.getUsername(), user.getStatus());
        return user;
    }
    
    // ======================== 用户注册功能 ========================
    
    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @param password 密码
     * @param nickname 昵称
     * @return 注册成功的用户信息
     */
    @Transactional
    public User register(String username, String email, String phone, String password, String nickname) {
        log.info("用户注册请求: username={}, email={}, phone={}", username, email, phone);
        
        // 验证注册信息
        validateRegistrationData(username, email, phone, password);
        
        // 检查用户是否已存在
        checkUserExists(username, email, phone);
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstLogin(true);
        user.setNeedChangePassword(false);
        user.setRegisteredAt(LocalDateTime.now());
        
        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return savedUser;
    }
    
    /**
     * 验证注册数据
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @param password 密码
     */
    private void validateRegistrationData(String username, String email, String phone, String password) {
        if (!StringUtils.hasText(username)) {
            throw BusinessException.badRequest("用户名不能为空");
        }
        
        if (!StringUtils.hasText(email)) {
            throw BusinessException.badRequest("邮箱不能为空");
        }
        
        if (!StringUtils.hasText(password)) {
            throw BusinessException.badRequest("密码不能为空");
        }
        
        // 验证密码强度
        if (password.length() < 6) {
            throw BusinessException.badRequest("密码长度不能少于6位");
        }
        
        // 验证邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw BusinessException.badRequest("邮箱格式不正确");
        }
        
        // 验证手机号格式（如果提供）
        if (StringUtils.hasText(phone) && !phone.matches("^1[3-9]\\d{9}$")) {
            throw BusinessException.badRequest("手机号格式不正确");
        }
    }
    
    /**
     * 检查用户是否已存在
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     */
    private void checkUserExists(String username, String email, String phone) {
        if (userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw BusinessException.conflict("用户名已存在");
        }
        
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw BusinessException.conflict("邮箱已被注册");
        }
        
        if (StringUtils.hasText(phone) && userRepository.existsByPhoneAndDeletedFalse(phone)) {
            throw BusinessException.conflict("手机号已被注册");
        }
    }
    
    // ======================== 用户登录功能 ========================
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应（包含JWT令牌）
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("用户登录请求: loginId={}, loginType={}", loginRequest.getLoginId(), loginRequest.getLoginType());
        
        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getLoginId(),
                            loginRequest.getPassword()
                    )
            );
            
            // 获取认证用户
            User user = (User) authentication.getPrincipal();
            
            // 检查用户状态
            checkUserStatus(user);
            
            // 更新登录信息
            updateLoginInfo(user);
            
            // 生成JWT令牌
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
            
            // 构建登录响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(7200L); // 2小时
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setFirstLogin(user.getFirstLogin());
            response.setNeedChangePassword(user.getNeedChangePassword());
            
            log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
            return response;
            
        } catch (BadCredentialsException e) {
            log.warn("用户登录失败-密码错误: loginId={}", loginRequest.getLoginId());
            
            // 记录登录失败
            recordLoginFailure(loginRequest.getLoginId());
            
            throw BusinessException.unauthorized("用户名或密码错误");
            
        } catch (DisabledException e) {
            log.warn("用户登录失败-账号被禁用: loginId={}", loginRequest.getLoginId());
            throw BusinessException.forbidden("账号已被禁用");
            
        } catch (AuthenticationException e) {
            log.warn("用户登录失败-认证异常: loginId={}, error={}", loginRequest.getLoginId(), e.getMessage());
            throw BusinessException.unauthorized("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户状态
     * 
     * @param user 用户对象
     */
    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        
        if (user.getStatus() == UserStatus.LOCKED) {
            throw BusinessException.forbidden("账号已被锁定，请联系管理员");
        }
        
        if (user.getStatus() == UserStatus.PENDING) {
            throw BusinessException.forbidden("账号待审核，请等待管理员审核");
        }
        
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw BusinessException.forbidden("账号未激活，请先激活账号");
        }
    }
    
    /**
     * 更新登录信息
     * 
     * @param user 用户对象
     */
    @Transactional
    public void updateLoginInfo(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        user.setFailedLoginAttempts(0); // 重置失败次数
        
        // 如果是首次登录，更新标记
        if (user.getFirstLogin()) {
            user.setFirstLogin(false);
        }
        
        userRepository.save(user);
        log.debug("更新用户登录信息: id={}, loginCount={}", user.getId(), user.getLoginCount());
    }
    
    /**
     * 记录登录失败
     * 
     * @param loginId 登录标识
     */
    @Transactional
    public void recordLoginFailure(String loginId) {
        Optional<User> userOpt = findUserByLoginIdentifier(loginId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLoginAt(LocalDateTime.now());
            
            // 如果失败次数超过限制，锁定账号
            if (user.getFailedLoginAttempts() >= 5) {
                user.setStatus(UserStatus.LOCKED);
                log.warn("用户账号因登录失败次数过多被锁定: id={}, username={}", user.getId(), user.getUsername());
            }
            
            userRepository.save(user);
        }
    }
    
    // ======================== 令牌管理功能 ========================
    
    /**
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    public LoginResponse refreshToken(String refreshToken) {
        log.debug("刷新访问令牌请求");
        
        try {
            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                throw BusinessException.unauthorized("无效的刷新令牌");
            }
            
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw BusinessException.badRequest("令牌类型错误");
            }
            
            // 提取用户信息
            String username = jwtUtil.extractUsername(refreshToken);
            User user = findUserByLoginIdentifier(username)
                    .orElseThrow(() -> BusinessException.notFound("用户不存在"));
            
            // 检查用户状态
            checkUserStatus(user);
            
            // 生成新的访问令牌
            String newAccessToken = jwtUtil.generateAccessToken(user);
            
            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken); // 保持原刷新令牌
            response.setTokenType("Bearer");
            response.setExpiresIn(7200L);
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            
            log.info("访问令牌刷新成功: userId={}, username={}", user.getId(), user.getUsername());
            return response;
            
        } catch (Exception e) {
            log.error("访问令牌刷新失败: {}", e.getMessage());
            throw BusinessException.unauthorized("令牌刷新失败");
        }
    }
    
    // ======================== 用户查询功能 ========================
    
    /**
     * 根据用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public User findById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username);
    }
    
    /**
     * 根据邮箱查询用户
     * 
     * @param email 邮箱
     * @return 用户信息
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email);
    }
    
    /**
     * 根据手机号查询用户
     * 
     * @param phone 手机号
     * @return 用户信息
     */
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhoneAndDeletedFalse(phone);
    }
    
    /**
     * 根据登录标识查询用户（支持用户名、邮箱、手机号）
     * 
     * @param loginIdentifier 登录标识
     * @return 用户信息
     */
    public Optional<User> findUserByLoginIdentifier(String loginIdentifier) {
        // 优先按用户名查询
        Optional<User> user = findByUsername(loginIdentifier);
        if (user.isPresent()) {
            return user;
        }
        
        // 其次按邮箱查询
        if (loginIdentifier.contains("@")) {
            user = findByEmail(loginIdentifier);
            if (user.isPresent()) {
                return user;
            }
        }
        
        // 最后按手机号查询
        if (loginIdentifier.matches("^1[3-9]\\d{9}$")) {
            user = findByPhone(loginIdentifier);
            if (user.isPresent()) {
                return user;
            }
        }
        
        return Optional.empty();
    }
    
    // ======================== 用户信息管理 ========================
    
    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param nickname 昵称
     * @param avatar 头像
     * @return 更新后的用户信息
     */
    @Transactional
    public User updateUserInfo(Long userId, String nickname, String avatar) {
        User user = findById(userId);
        
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }
        
        User savedUser = userRepository.save(user);
        log.info("用户信息更新成功: id={}, nickname={}", userId, nickname);
        
        return savedUser;
    }
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("旧密码不正确");
        }
        
        // 验证新密码强度
        if (newPassword.length() < 6) {
            throw BusinessException.badRequest("新密码长度不能少于6位");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setNeedChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("用户密码修改成功: id={}", userId);
    }
    
    /**
     * 更新用户状态
     * 
     * @param userId 用户ID
     * @param status 新状态
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = findById(userId);
        user.setStatus(status);
        
        userRepository.save(user);
        log.info("用户状态更新成功: id={}, status={}", userId, status);
    }
}