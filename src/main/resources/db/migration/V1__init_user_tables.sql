-- 文件级分析：
-- - 职责：初始化用户相关数据表结构，为用户模块提供数据存储基础
-- - 位置考虑：放在db/migration目录下，遵循Flyway数据库版本管理规范
-- - 命名原因：V1表示第一个版本，init_user_tables表明初始化用户表
-- - 执行时机：应用启动时由Flyway自动执行，确保数据库结构与代码同步
--
-- 设计思路：
-- 1. 严格按照JPA实体类设计创建表结构
-- 2. 合理设置字段类型和约束，保证数据完整性
-- 3. 创建必要的索引，优化查询性能
-- 4. 支持软删除和审计字段
-- 5. 考虑字符集和排序规则，支持中文和emoji

-- ================================================================================
-- 用户表（users）
-- 存储系统中所有用户的基本信息、认证信息和状态信息
-- ================================================================================

CREATE TABLE users (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    
    -- ======================== 基本信息字段 ========================
    username VARCHAR(50) NOT NULL COMMENT '用户名，4-20位字母数字下划线',
    email VARCHAR(100) NOT NULL COMMENT '邮箱地址，用于登录和通知',
    phone VARCHAR(20) NULL COMMENT '手机号码，支持国际格式',
    nickname VARCHAR(50) NULL COMMENT '昵称，用户显示名称',
    real_name VARCHAR(50) NULL COMMENT '真实姓名，用于实名认证',
    
    -- ======================== 认证信息字段 ========================
    password VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密存储',
    
    -- 用户状态：1-活跃，2-非活跃，3-禁用，4-锁定，5-待审核
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态',
    
    email_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证：0-未验证，1-已验证',
    phone_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '手机号是否已验证：0-未验证，1-已验证',
    
    -- ======================== 扩展信息字段 ========================
    avatar_url VARCHAR(500) NULL COMMENT '头像URL地址',
    gender TINYINT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE NULL COMMENT '生日',
    bio VARCHAR(500) NULL COMMENT '个人简介',
    
    -- ======================== 统计信息字段 ========================
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) NULL COMMENT '最后登录IP地址',
    login_failure_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    locked_until DATETIME NULL COMMENT '账户锁定截止时间',
    
    -- ======================== 审计字段 ========================
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    -- ======================== 主键和约束 ========================
    PRIMARY KEY (id),
    
    -- 唯一约束
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_phone (phone),
    
    -- 检查约束（MySQL 8.0+支持）
    CONSTRAINT chk_users_status CHECK (status IN (1, 2, 3, 4, 5)),
    CONSTRAINT chk_users_gender CHECK (gender IN (0, 1, 2)),
    CONSTRAINT chk_users_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT chk_users_email_verified CHECK (email_verified IN (0, 1)),
    CONSTRAINT chk_users_phone_verified CHECK (phone_verified IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='用户表';

-- ================================================================================
-- 创建索引，优化查询性能
-- ================================================================================

-- 复合索引：状态 + 删除标记 + 创建时间（用于用户列表查询）
CREATE INDEX idx_users_status_deleted_created ON users(status, deleted, created_at DESC);

-- 复合索引：删除标记 + 更新时间（用于数据同步）
CREATE INDEX idx_users_deleted_updated ON users(deleted, updated_at);

-- 索引：最后登录时间（用于活跃用户统计）
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

-- 索引：锁定截止时间（用于自动解锁任务）
CREATE INDEX idx_users_locked_until ON users(locked_until);

-- 索引：邮箱验证状态（用于未验证用户查询）
CREATE INDEX idx_users_email_verified ON users(email_verified);

-- 索引：手机验证状态（用于未验证用户查询）
CREATE INDEX idx_users_phone_verified ON users(phone_verified);

-- ================================================================================
-- 插入初始数据
-- ================================================================================

-- 插入系统管理员账户
-- 密码：admin123（BCrypt加密后的值需要在应用中生成）
INSERT INTO users (
    username, 
    email, 
    phone,
    nickname,
    real_name,
    password, 
    status,
    email_verified,
    phone_verified,
    created_by,
    updated_by
) VALUES (
    'admin',
    'admin@ecommerce.com',
    '13800138000',
    '系统管理员',
    '管理员',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.ZstkF9be', -- admin123
    1, -- ACTIVE
    1, -- 邮箱已验证
    1, -- 手机已验证
    1, -- 创建者为自己
    1  -- 更新者为自己
);

-- 插入测试用户账户（仅开发环境使用）
INSERT INTO users (
    username, 
    email, 
    phone,
    nickname,
    real_name,
    password, 
    status,
    email_verified,
    phone_verified,
    avatar_url,
    gender,
    birthday,
    bio,
    created_by,
    updated_by
) VALUES (
    'testuser',
    'test@example.com',
    '13900139000',
    '测试用户',
    '张三',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.ZstkF9be', -- admin123
    1, -- ACTIVE
    1, -- 邮箱已验证
    0, -- 手机未验证
    'https://example.com/avatar/default.jpg',
    1, -- 男
    '1990-01-01',
    '这是一个测试用户账户，用于系统功能验证。',
    1, -- 创建者为admin
    1  -- 更新者为admin
);

-- ================================================================================
-- 创建视图，便于查询
-- ================================================================================

-- 活跃用户视图（排除已删除和已禁用的用户）
CREATE VIEW v_active_users AS
SELECT 
    id,
    username,
    email,
    phone,
    nickname,
    real_name,
    avatar_url,
    gender,
    birthday,
    status,
    email_verified,
    phone_verified,
    last_login_at,
    created_at,
    updated_at
FROM users 
WHERE deleted = 0 AND status IN (1, 2); -- ACTIVE 或 INACTIVE

-- 用户统计视图
CREATE VIEW v_user_statistics AS
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as inactive_users,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as disabled_users,
    SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as locked_users,
    SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as pending_users,
    SUM(CASE WHEN email_verified = 1 THEN 1 ELSE 0 END) as email_verified_users,
    SUM(CASE WHEN phone_verified = 1 THEN 1 ELSE 0 END) as phone_verified_users,
    SUM(CASE WHEN last_login_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as monthly_active_users,
    SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) as new_users_this_week
FROM users 
WHERE deleted = 0;