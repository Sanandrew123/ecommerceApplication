-- 文件级分析：
-- - 职责：创建支付相关数据表结构，为支付管理模块提供数据存储基础
-- - 位置考虑：放在db/migration目录下，版本号V4表示第四个数据库迁移
-- - 命名原因：init_payment_tables表明初始化支付相关表
-- - 执行时机：V3订单表创建后执行，建立支付模块的数据基础
--
-- 设计思路：
-- 1. 创建支付记录表，记录每笔支付的详细信息
-- 2. 创建支付日志表，追踪支付的完整链路
-- 3. 建立合理的外键关系和索引优化
-- 4. 插入测试数据，便于开发调试和业务验证
-- 5. 创建支付单号生成函数，确保支付单号的唯一性

-- ================================================================================
-- 支付记录表（payments）
-- 存储系统中每笔支付的详细信息
-- ================================================================================

CREATE TABLE payments (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    
    -- ======================== 基本信息字段 ========================
    payment_no VARCHAR(50) NOT NULL UNIQUE COMMENT '支付单号，全局唯一',
    
    -- 关联字段
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    user_id BIGINT NOT NULL COMMENT '支付用户ID',
    
    -- 支付状态：1-待支付，2-支付处理中，3-支付成功，4-支付失败，5-支付取消，6-支付超时，7-退款中，8-已退款，9-部分退款
    status TINYINT NOT NULL DEFAULT 1 COMMENT '支付状态',
    
    -- 支付方式：1-支付宝，2-微信支付，3-银行卡，4-信用卡，5-余额支付，6-积分支付，7-货到付款，8-银行转账
    payment_method TINYINT NOT NULL COMMENT '支付方式',
    
    -- ======================== 金额信息字段 ========================
    payment_amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    fee_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '手续费金额',
    actual_amount DECIMAL(12,2) NOT NULL COMMENT '实际支付金额',
    refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    
    -- ======================== 第三方支付信息字段 ========================
    third_party_transaction_no VARCHAR(100) NULL COMMENT '第三方交易号',
    third_party_channel VARCHAR(50) NULL COMMENT '第三方支付渠道',
    third_party_status VARCHAR(20) NULL COMMENT '第三方支付状态',
    payment_url VARCHAR(500) NULL COMMENT '支付链接或二维码',
    
    -- ======================== 时间信息字段 ========================
    payment_time DATETIME NULL COMMENT '支付时间',
    expire_time DATETIME NULL COMMENT '支付超时时间',
    notify_time DATETIME NULL COMMENT '通知时间',
    
    -- ======================== 处理结果字段 ========================
    result_code VARCHAR(20) NULL COMMENT '处理结果码',
    result_message VARCHAR(200) NULL COMMENT '处理结果描述',
    error_code VARCHAR(50) NULL COMMENT '错误代码',
    error_message VARCHAR(200) NULL COMMENT '错误描述',
    
    -- ======================== 扩展信息字段 ========================
    request_params JSON NULL COMMENT '支付请求参数',
    response_data JSON NULL COMMENT '支付响应数据',
    notify_data JSON NULL COMMENT '回调通知数据',
    client_ip VARCHAR(45) NULL COMMENT '客户端IP地址',
    user_agent VARCHAR(500) NULL COMMENT '用户代理信息',
    remark VARCHAR(500) NULL COMMENT '备注信息',
    attributes JSON NULL COMMENT '扩展属性',
    
    -- ======================== 审计字段 ========================
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    -- ======================== 主键和约束 ========================
    PRIMARY KEY (id),
    
    -- 外键约束
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id),
    
    -- 检查约束
    CONSTRAINT chk_payments_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7, 8, 9)),
    CONSTRAINT chk_payments_method CHECK (payment_method IN (1, 2, 3, 4, 5, 6, 7, 8)),
    CONSTRAINT chk_payments_amounts CHECK (
        payment_amount >= 0.01 AND 
        fee_amount >= 0.00 AND 
        actual_amount >= 0.01 AND
        refunded_amount >= 0.00 AND
        refunded_amount <= payment_amount
    ),
    CONSTRAINT chk_payments_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='支付记录表';

-- ================================================================================
-- 支付日志表（payment_logs）
-- 记录支付过程中的所有操作和状态变更
-- ================================================================================

CREATE TABLE payment_logs (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付日志ID',
    
    -- ======================== 基本信息字段 ========================
    payment_id BIGINT NOT NULL COMMENT '关联支付记录ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    log_level VARCHAR(10) NOT NULL DEFAULT 'INFO' COMMENT '日志级别',
    title VARCHAR(200) NOT NULL COMMENT '操作标题',
    description VARCHAR(1000) NULL COMMENT '操作描述',
    
    -- ======================== 状态信息字段 ========================
    before_status TINYINT NULL COMMENT '操作前状态',
    after_status TINYINT NULL COMMENT '操作后状态',
    result_status VARCHAR(20) NOT NULL COMMENT '处理结果状态',
    result_code VARCHAR(50) NULL COMMENT '结果代码',
    result_message VARCHAR(500) NULL COMMENT '结果消息',
    
    -- ======================== 详细数据字段 ========================
    request_data TEXT NULL COMMENT '请求参数',
    response_data TEXT NULL COMMENT '响应数据',
    error_info TEXT NULL COMMENT '错误信息',
    exception_stack TEXT NULL COMMENT '异常堆栈',
    
    -- ======================== 执行信息字段 ========================
    execute_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    complete_time DATETIME NULL COMMENT '完成时间',
    duration BIGINT NULL COMMENT '执行耗时（毫秒）',
    operator_id BIGINT NULL COMMENT '操作者ID',
    operator_name VARCHAR(100) NULL COMMENT '操作者名称',
    
    -- ======================== 技术信息字段 ========================
    trace_id VARCHAR(100) NULL COMMENT '追踪ID',
    session_id VARCHAR(100) NULL COMMENT '会话ID',
    client_ip VARCHAR(45) NULL COMMENT '客户端IP地址',
    user_agent VARCHAR(500) NULL COMMENT '用户代理信息',
    server_info VARCHAR(100) NULL COMMENT '服务器信息',
    
    -- ======================== 扩展信息字段 ========================
    business_tags VARCHAR(200) NULL COMMENT '业务标签',
    attributes JSON NULL COMMENT '扩展属性',
    
    -- ======================== 审计字段 ========================
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    -- ======================== 主键和约束 ========================
    PRIMARY KEY (id),
    
    -- 外键约束
    CONSTRAINT fk_payment_log_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    
    -- 检查约束
    CONSTRAINT chk_payment_logs_level CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    CONSTRAINT chk_payment_logs_result_status CHECK (result_status IN ('SUCCESS', 'FAILED', 'PROCESSING')),
    CONSTRAINT chk_payment_logs_duration CHECK (duration >= 0),
    CONSTRAINT chk_payment_logs_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='支付日志表';

-- ================================================================================
-- 创建索引，优化查询性能
-- ================================================================================

-- 支付记录表索引
CREATE UNIQUE INDEX uk_payments_payment_no ON payments(payment_no);
CREATE INDEX idx_payments_order ON payments(order_id, deleted);
CREATE INDEX idx_payments_user ON payments(user_id, deleted);
CREATE INDEX idx_payments_status ON payments(status, deleted);
CREATE INDEX idx_payments_method ON payments(payment_method, deleted);
CREATE INDEX idx_payments_third_party_no ON payments(third_party_transaction_no);
CREATE INDEX idx_payments_payment_time ON payments(payment_time DESC);
CREATE INDEX idx_payments_amount ON payments(payment_amount DESC, deleted);
CREATE INDEX idx_payments_user_status ON payments(user_id, status, deleted);
CREATE INDEX idx_payments_created ON payments(created_at DESC, deleted);

-- 支付日志表索引
CREATE INDEX idx_payment_logs_payment ON payment_logs(payment_id, deleted);
CREATE INDEX idx_payment_logs_operation ON payment_logs(operation_type, deleted);
CREATE INDEX idx_payment_logs_level ON payment_logs(log_level, deleted);
CREATE INDEX idx_payment_logs_result_status ON payment_logs(result_status, deleted);
CREATE INDEX idx_payment_logs_execute_time ON payment_logs(execute_time DESC);
CREATE INDEX idx_payment_logs_payment_operation ON payment_logs(payment_id, operation_type, deleted);
CREATE INDEX idx_payment_logs_trace_id ON payment_logs(trace_id);
CREATE INDEX idx_payment_logs_duration ON payment_logs(duration DESC);

-- ================================================================================
-- 创建支付单号生成函数
-- ================================================================================

DELIMITER $$

CREATE FUNCTION generate_payment_no() RETURNS VARCHAR(50)
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE payment_no VARCHAR(50);
    DECLARE date_prefix VARCHAR(11);
    DECLARE sequence_no INT DEFAULT 1;
    DECLARE max_attempts INT DEFAULT 100;
    DECLARE attempt_count INT DEFAULT 0;
    
    -- 生成日期前缀（PAY + YYYYMMDD）
    SET date_prefix = CONCAT('PAY', DATE_FORMAT(NOW(), '%Y%m%d'));
    
    -- 获取当天支付单的最大序号
    SELECT COALESCE(MAX(CAST(SUBSTRING(payment_no, 12) AS UNSIGNED)), 0) + 1
    INTO sequence_no
    FROM payments 
    WHERE payment_no LIKE CONCAT(date_prefix, '%') 
    AND deleted = 0;
    
    -- 生成支付单号并检查唯一性
    WHILE attempt_count < max_attempts DO
        SET payment_no = CONCAT(date_prefix, LPAD(sequence_no, 9, '0'));
        
        -- 检查支付单号是否已存在
        IF NOT EXISTS (SELECT 1 FROM payments WHERE payment_no = payment_no AND deleted = 0) THEN
            RETURN payment_no;
        END IF;
        
        SET sequence_no = sequence_no + 1;
        SET attempt_count = attempt_count + 1;
    END WHILE;
    
    -- 如果生成失败，使用时间戳作为后缀
    SET payment_no = CONCAT(date_prefix, UNIX_TIMESTAMP(NOW()) % 1000000000);
    RETURN payment_no;
END$$

DELIMITER ;

-- ================================================================================
-- 插入测试支付数据
-- ================================================================================

-- 生成测试支付记录
INSERT INTO payments (
    payment_no, order_id, user_id, status, payment_method,
    payment_amount, fee_amount, actual_amount,
    payment_time, expire_time,
    result_code, result_message,
    client_ip, created_by, updated_by
) VALUES 
-- 支付成功的支付记录
(generate_payment_no(), 2, 2, 3, 1, 
 15999.00, 95.99, 16094.99,
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 29 MINUTE),
 'SUCCESS', '支付成功',
 '192.168.1.100', 2, 2),

-- 待支付的支付记录
(generate_payment_no(), 1, 2, 1, 2,
 8899.00, 53.39, 8952.39,
 NULL, DATE_ADD(NOW(), INTERVAL 25 MINUTE),
 NULL, NULL,
 '192.168.1.101', 2, 2),

-- 已完成的支付记录（对应已完成订单）
(generate_payment_no(), 3, 2, 3, 1,
 6809.00, 40.85, 6849.85,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY, -30, MINUTE),
 'SUCCESS', '支付成功',
 '192.168.1.102', 2, 2);

-- 更新第三方支付信息
UPDATE payments SET 
    third_party_transaction_no = CONCAT('2024010112345678', id),
    third_party_channel = CASE payment_method 
        WHEN 1 THEN 'alipay_pc'
        WHEN 2 THEN 'wechat_jsapi'
        ELSE 'unknown'
    END,
    third_party_status = CASE status
        WHEN 3 THEN 'TRADE_SUCCESS'
        WHEN 1 THEN 'WAIT_BUYER_PAY'
        ELSE 'UNKNOWN'
    END,
    updated_at = NOW()
WHERE status IN (1, 3) AND user_id = 2;

-- ================================================================================
-- 插入支付日志数据
-- ================================================================================

-- 为成功支付记录插入日志
INSERT INTO payment_logs (
    payment_id, operation_type, log_level, title, description,
    before_status, after_status, result_status, result_code, result_message,
    execute_time, complete_time, duration,
    operator_name, trace_id, client_ip,
    created_by, updated_by
) VALUES
-- 创建支付记录日志
(1, 'CREATE', 'INFO', '创建支付记录', '用户发起支付，创建支付记录',
 NULL, 1, 'SUCCESS', 'CREATE_SUCCESS', '支付记录创建成功',
 DATE_SUB(NOW(), INTERVAL 1 HOUR, -5, MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR, -5, MINUTE, -100, MICROSECOND), 100,
 'system', CONCAT('TRACE_', UNIX_TIMESTAMP(), '_1'), '192.168.1.100',
 NULL, NULL),

-- 支付处理日志
(1, 'PAY', 'INFO', '处理支付请求', '调用支付宝接口处理支付',
 1, 2, 'SUCCESS', 'PAY_PROCESSING', '支付处理中',
 DATE_SUB(NOW(), INTERVAL 1 HOUR, -1, MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR, -1, MINUTE, -500, MICROSECOND), 500,
 'system', CONCAT('TRACE_', UNIX_TIMESTAMP(), '_2'), '192.168.1.100',
 NULL, NULL),

-- 支付成功日志
(1, 'NOTIFY', 'INFO', '支付成功通知', '接收支付宝支付成功回调',
 2, 3, 'SUCCESS', 'NOTIFY_SUCCESS', '支付成功',
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR, -200, MICROSECOND), 200,
 'system', CONCAT('TRACE_', UNIX_TIMESTAMP(), '_3'), '47.96.11.45',
 NULL, NULL);

-- 为待支付记录插入创建日志
INSERT INTO payment_logs (
    payment_id, operation_type, log_level, title, description,
    before_status, after_status, result_status, result_code, result_message,
    execute_time, complete_time, duration,
    operator_name, trace_id, client_ip,
    created_by, updated_by
) VALUES
(2, 'CREATE', 'INFO', '创建支付记录', '用户发起支付，创建支付记录',
 NULL, 1, 'SUCCESS', 'CREATE_SUCCESS', '支付记录创建成功',
 NOW(), NOW(), 50,
 'system', CONCAT('TRACE_', UNIX_TIMESTAMP(), '_4'), '192.168.1.101',
 NULL, NULL);

-- ================================================================================
-- 创建视图，便于查询
-- ================================================================================

-- 支付详情视图（包含订单和用户信息）
CREATE VIEW v_payment_details AS
SELECT 
    p.id,
    p.payment_no,
    p.status,
    p.payment_method,
    p.payment_amount,
    p.actual_amount,
    p.payment_time,
    p.created_at,
    o.order_no,
    o.total_amount as order_amount,
    u.username,
    u.email,
    CASE p.payment_method
        WHEN 1 THEN '支付宝'
        WHEN 2 THEN '微信支付'
        WHEN 3 THEN '银行卡'
        WHEN 4 THEN '信用卡'
        WHEN 5 THEN '余额支付'
        WHEN 6 THEN '积分支付'
        WHEN 7 THEN '货到付款'
        WHEN 8 THEN '银行转账'
        ELSE '未知'
    END as payment_method_name,
    CASE p.status
        WHEN 1 THEN '待支付'
        WHEN 2 THEN '支付处理中'
        WHEN 3 THEN '支付成功'
        WHEN 4 THEN '支付失败'
        WHEN 5 THEN '支付取消'
        WHEN 6 THEN '支付超时'
        WHEN 7 THEN '退款中'
        WHEN 8 THEN '已退款'
        WHEN 9 THEN '部分退款'
        ELSE '未知'
    END as status_name
FROM payments p
LEFT JOIN orders o ON p.order_id = o.id
LEFT JOIN users u ON p.user_id = u.id
WHERE p.deleted = 0 AND o.deleted = 0 AND u.deleted = 0;

-- 支付统计视图
CREATE VIEW v_payment_statistics AS
SELECT 
    COUNT(*) as total_payments,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as pending_payments,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as processing_payments,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as success_payments,
    SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as failed_payments,
    SUM(CASE WHEN status IN (5, 6) THEN 1 ELSE 0 END) as cancelled_payments,
    SUM(CASE WHEN status IN (7, 8, 9) THEN 1 ELSE 0 END) as refund_payments,
    SUM(payment_amount) as total_amount,
    SUM(CASE WHEN status = 3 THEN payment_amount ELSE 0 END) as success_amount,
    SUM(fee_amount) as total_fee_amount,
    SUM(refunded_amount) as total_refunded_amount,
    AVG(payment_amount) as average_payment_amount,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 END) as payments_this_week,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as payments_this_month
FROM payments 
WHERE deleted = 0;

-- 用户支付统计视图
CREATE VIEW v_user_payment_statistics AS
SELECT 
    u.id as user_id,
    u.username,
    COUNT(p.id) as total_payments,
    SUM(p.payment_amount) as total_amount,
    SUM(CASE WHEN p.status = 3 THEN p.payment_amount ELSE 0 END) as success_amount,
    AVG(p.payment_amount) as average_payment_amount,
    MAX(p.payment_time) as last_payment_time,
    COUNT(CASE WHEN p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as payments_this_month,
    -- 统计各支付方式使用次数
    SUM(CASE WHEN p.payment_method = 1 AND p.status = 3 THEN 1 ELSE 0 END) as alipay_count,
    SUM(CASE WHEN p.payment_method = 2 AND p.status = 3 THEN 1 ELSE 0 END) as wechat_count,
    SUM(CASE WHEN p.payment_method IN (3, 4) AND p.status = 3 THEN 1 ELSE 0 END) as card_count
FROM users u
LEFT JOIN payments p ON u.id = p.user_id AND p.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id;