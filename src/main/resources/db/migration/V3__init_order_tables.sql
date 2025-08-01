-- 文件级分析：
-- - 职责：创建订单相关数据表结构，为订单管理模块提供数据存储基础
-- - 位置考虑：放在db/migration目录下，版本号V3表示第三个数据库迁移
-- - 命名原因：init_order_tables表明初始化订单相关表
-- - 执行时机：V2商品表创建后执行，建立订单模块的数据基础
--
-- 设计思路：
-- 1. 创建订单主表，包含订单的完整信息和状态管理
-- 2. 创建订单项表，采用快照机制记录商品信息
-- 3. 建立合理的外键关系和索引优化
-- 4. 插入测试数据，便于开发调试和业务验证
-- 5. 创建订单号生成函数，确保订单号的唯一性

-- ================================================================================
-- 订单表（orders）
-- 存储订单的完整信息，包括基本信息、金额、地址、状态等
-- ================================================================================

CREATE TABLE orders (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    
    -- ======================== 基本信息字段 ========================
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号，全局唯一',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 订单状态：1-待支付，2-已支付，3-已确认，4-已发货，5-已送达，6-已完成，7-已取消，8-退款中，9-已退款
    status TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态',
    
    order_type TINYINT NOT NULL DEFAULT 1 COMMENT '订单类型：1-普通订单，2-预售订单，3-团购订单，4-秒杀订单',
    source TINYINT NOT NULL DEFAULT 1 COMMENT '订单来源：1-PC端，2-移动端，3-微信小程序，4-APP',
    
    -- ======================== 金额信息字段 ========================
    product_amount DECIMAL(12,2) NOT NULL COMMENT '商品总金额',
    shipping_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已支付金额',
    refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    
    -- ======================== 收货信息字段 ========================
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province VARCHAR(20) NOT NULL COMMENT '省份',
    city VARCHAR(20) NOT NULL COMMENT '城市',
    district VARCHAR(20) NOT NULL COMMENT '区县',
    address VARCHAR(200) NOT NULL COMMENT '详细地址',
    postal_code VARCHAR(6) NULL COMMENT '邮政编码',
    
    -- ======================== 物流信息字段 ========================
    shipping_company VARCHAR(20) NULL COMMENT '快递公司编码',
    shipping_company_name VARCHAR(50) NULL COMMENT '快递公司名称',
    tracking_number VARCHAR(50) NULL COMMENT '快递单号',
    shipping_status TINYINT NULL COMMENT '物流状态：1-待发货，2-已发货，3-运输中，4-派件中，5-已签收，6-异常',
    shipping_info JSON NULL COMMENT '物流跟踪信息',
    
    -- ======================== 时间节点字段 ========================
    payment_time DATETIME NULL COMMENT '支付时间',
    shipping_time DATETIME NULL COMMENT '发货时间',
    delivery_time DATETIME NULL COMMENT '送达时间',
    completion_time DATETIME NULL COMMENT '完成时间',
    cancellation_time DATETIME NULL COMMENT '取消时间',
    payment_timeout DATETIME NULL COMMENT '支付超时时间',
    
    -- ======================== 扩展信息字段 ========================
    user_remark VARCHAR(500) NULL COMMENT '用户备注',
    merchant_remark VARCHAR(500) NULL COMMENT '商家备注',
    cancel_reason VARCHAR(200) NULL COMMENT '取消原因',
    coupon_id BIGINT NULL COMMENT '优惠券ID',
    promotion_id BIGINT NULL COMMENT '促销活动ID',
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
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    
    -- 检查约束
    CONSTRAINT chk_orders_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7, 8, 9)),
    CONSTRAINT chk_orders_type CHECK (order_type IN (1, 2, 3, 4)),
    CONSTRAINT chk_orders_source CHECK (source IN (1, 2, 3, 4)),
    CONSTRAINT chk_orders_amounts CHECK (
        product_amount >= 0.01 AND 
        shipping_amount >= 0.00 AND 
        discount_amount >= 0.00 AND 
        total_amount >= 0.01 AND
        paid_amount >= 0.00 AND
        refunded_amount >= 0.00
    ),
    CONSTRAINT chk_orders_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='订单表';

-- ================================================================================
-- 订单项表（order_items）
-- 存储订单中每个商品的详细信息，采用快照机制
-- ================================================================================

CREATE TABLE order_items (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    
    -- ======================== 关联关系字段 ========================
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    
    -- ======================== 商品快照信息字段 ========================
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称（快照）',
    product_sku VARCHAR(100) NULL COMMENT '商品SKU（快照）',
    product_brand VARCHAR(100) NULL COMMENT '商品品牌（快照）',
    category_name VARCHAR(100) NULL COMMENT '商品分类（快照）',
    product_image VARCHAR(500) NULL COMMENT '商品主图（快照）',
    product_specs JSON NULL COMMENT '商品规格（快照）',
    
    -- ======================== 价格信息字段 ========================
    unit_price DECIMAL(12,2) NOT NULL COMMENT '商品单价',
    original_price DECIMAL(12,2) NULL COMMENT '商品原价',
    
    -- ======================== 数量信息字段 ========================
    quantity INT NOT NULL COMMENT '购买数量',
    shipped_quantity INT NOT NULL DEFAULT 0 COMMENT '已发货数量',
    returned_quantity INT NOT NULL DEFAULT 0 COMMENT '已退货数量',
    
    -- ======================== 金额信息字段 ========================
    subtotal_amount DECIMAL(14,2) NOT NULL COMMENT '小计金额',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    actual_amount DECIMAL(14,2) NOT NULL COMMENT '实际金额',
    refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    
    -- ======================== 状态信息字段 ========================
    shipping_status TINYINT NOT NULL DEFAULT 1 COMMENT '发货状态：1-待发货，2-部分发货，3-全部发货',
    return_status TINYINT NOT NULL DEFAULT 0 COMMENT '退货状态：0-无退货，1-申请退货，2-退货中，3-已退货，4-退货拒绝',
    is_reviewed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已评价：0-未评价，1-已评价',
    
    -- ======================== 扩展信息字段 ========================
    product_snapshot JSON NULL COMMENT '商品快照',
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
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    
    -- 检查约束
    CONSTRAINT chk_order_items_quantity CHECK (quantity >= 1),
    CONSTRAINT chk_order_items_shipped_quantity CHECK (shipped_quantity >= 0 AND shipped_quantity <= quantity),
    CONSTRAINT chk_order_items_returned_quantity CHECK (returned_quantity >= 0 AND returned_quantity <= shipped_quantity),
    CONSTRAINT chk_order_items_amounts CHECK (
        unit_price >= 0.01 AND 
        subtotal_amount >= 0.00 AND 
        discount_amount >= 0.00 AND 
        actual_amount >= 0.00 AND
        refunded_amount >= 0.00
    ),
    CONSTRAINT chk_order_items_shipping_status CHECK (shipping_status IN (1, 2, 3)),
    CONSTRAINT chk_order_items_return_status CHECK (return_status IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_order_items_is_reviewed CHECK (is_reviewed IN (0, 1)),
    CONSTRAINT chk_order_items_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='订单项表';

-- ================================================================================
-- 创建索引，优化查询性能
-- ================================================================================

-- 订单表索引
CREATE UNIQUE INDEX uk_orders_order_no ON orders(order_no);
CREATE INDEX idx_orders_user_status ON orders(user_id, status, deleted);
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC, deleted);
CREATE INDEX idx_orders_payment_time ON orders(payment_time DESC);
CREATE INDEX idx_orders_total_amount ON orders(total_amount DESC, deleted);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC, deleted);

-- 订单项表索引
CREATE INDEX idx_order_items_order ON order_items(order_id, deleted);
CREATE INDEX idx_order_items_product ON order_items(product_id, deleted);
CREATE INDEX idx_order_items_order_product ON order_items(order_id, product_id, deleted);
CREATE INDEX idx_order_items_shipping_status ON order_items(shipping_status, deleted);
CREATE INDEX idx_order_items_return_status ON order_items(return_status, deleted);

-- ================================================================================
-- 创建订单号生成函数
-- ================================================================================

DELIMITER $$

CREATE FUNCTION generate_order_no() RETURNS VARCHAR(50)
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE order_no VARCHAR(50);
    DECLARE date_prefix VARCHAR(8);
    DECLARE sequence_no INT DEFAULT 1;
    DECLARE max_attempts INT DEFAULT 100;
    DECLARE attempt_count INT DEFAULT 0;
    
    -- 生成日期前缀（YYYYMMDD）
    SET date_prefix = DATE_FORMAT(NOW(), '%Y%m%d');
    
    -- 获取当天订单的最大序号
    SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, 9) AS UNSIGNED)), 0) + 1
    INTO sequence_no
    FROM orders 
    WHERE order_no LIKE CONCAT(date_prefix, '%') 
    AND deleted = 0;
    
    -- 生成订单号并检查唯一性
    WHILE attempt_count < max_attempts DO
        SET order_no = CONCAT(date_prefix, LPAD(sequence_no, 9, '0'));
        
        -- 检查订单号是否已存在
        IF NOT EXISTS (SELECT 1 FROM orders WHERE order_no = order_no AND deleted = 0) THEN
            RETURN order_no;
        END IF;
        
        SET sequence_no = sequence_no + 1;
        SET attempt_count = attempt_count + 1;
    END WHILE;
    
    -- 如果生成失败，使用时间戳作为后缀
    SET order_no = CONCAT(date_prefix, UNIX_TIMESTAMP(NOW()) % 1000000000);
    RETURN order_no;
END$$

DELIMITER ;

-- ================================================================================
-- 插入测试订单数据
-- ================================================================================

-- 生成测试订单
INSERT INTO orders (
    order_no, user_id, status, order_type, source,
    product_amount, shipping_amount, discount_amount, total_amount,
    receiver_name, receiver_phone, province, city, district, address,
    user_remark, payment_timeout,
    created_by, updated_by
) VALUES 
-- 待支付订单
(generate_order_no(), 2, 1, 1, 1, 
 8999.00, 0.00, 100.00, 8899.00,
 '张三', '13900139001', '北京市', '北京市', '朝阳区', '建国路88号SOHO现代城',
 '请尽快发货，谢谢！', DATE_ADD(NOW(), INTERVAL 30 MINUTE),
 2, 2),

-- 已支付订单 
(generate_order_no(), 2, 2, 1, 2,
 15999.00, 0.00, 0.00, 15999.00,
 '李四', '13900139002', '上海市', '上海市', '浦东新区', '陆家嘴金融中心999号',
 '工作日送货', NOW(),
 2, 2),

-- 已完成订单
(generate_order_no(), 2, 6, 1, 1,
 6999.00, 10.00, 200.00, 6809.00,
 '王五', '13900139003', '广东省', '深圳市', '南山区', '科技园南路15号',
 '', NOW(),
 2, 2);

-- 更新已支付和已完成订单的时间
UPDATE orders SET 
    payment_time = DATE_SUB(NOW(), INTERVAL 1 HOUR),
    updated_at = NOW()
WHERE status IN (2, 6) AND user_id = 2;

UPDATE orders SET 
    payment_time = DATE_SUB(NOW(), INTERVAL 3 DAY),
    shipping_time = DATE_SUB(NOW(), INTERVAL 2 DAY),
    delivery_time = DATE_SUB(NOW(), INTERVAL 1 DAY),
    completion_time = NOW(),
    updated_at = NOW()
WHERE status = 6 AND user_id = 2;

-- 插入订单项数据
INSERT INTO order_items (
    order_id, product_id, product_name, product_sku, product_brand, 
    category_name, product_image, unit_price, original_price,
    quantity, subtotal_amount, discount_amount, actual_amount,
    created_by, updated_by
) VALUES
-- 第一个订单的订单项
(1, 1, 'iPhone 15 Pro 256GB 深空黑', 'IP15P-256-BLACK', 'Apple',
 '手机通讯', '/uploads/products/iphone15pro/main.jpg', 8999.00, 9999.00,
 1, 8999.00, 100.00, 8899.00, 2, 2),

-- 第二个订单的订单项  
(2, 2, 'MacBook Pro 14英寸 M3芯片', 'MBP14-M3-512', 'Apple',
 '电脑办公', '/uploads/products/macbookpro14/main.jpg', 15999.00, 16999.00,
 1, 15999.00, 0.00, 15999.00, 2, 2),

-- 第三个订单的订单项
(3, 3, '华为Mate 60 Pro 512GB 雅川青', 'HW-M60P-512-GREEN', '华为',
 '手机通讯', '/uploads/products/mate60pro/main.jpg', 6999.00, 7999.00,
 1, 6999.00, 200.00, 6799.00, 2, 2);

-- 更新已完成订单项的发货状态
UPDATE order_items SET 
    shipped_quantity = quantity,
    shipping_status = 3,
    updated_at = NOW()
WHERE order_id = 3;

-- ================================================================================
-- 创建视图，便于查询
-- ================================================================================

-- 订单详情视图（包含用户和订单项信息）
CREATE VIEW v_order_details AS
SELECT 
    o.id,
    o.order_no,
    o.status,
    o.total_amount,
    o.created_at,
    o.payment_time,
    o.shipping_time,
    o.completion_time,
    u.username,
    u.email,
    o.receiver_name,
    o.receiver_phone,
    CONCAT(o.province, ' ', o.city, ' ', o.district, ' ', o.address) as full_address,
    COUNT(oi.id) as item_count,
    SUM(oi.quantity) as total_quantity
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
LEFT JOIN order_items oi ON o.id = oi.order_id AND oi.deleted = 0
WHERE o.deleted = 0 AND u.deleted = 0
GROUP BY o.id;

-- 订单统计视图
CREATE VIEW v_order_statistics AS
SELECT 
    COUNT(*) as total_orders,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as pending_payment_orders,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as paid_orders,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as confirmed_orders,
    SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as shipped_orders,
    SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as delivered_orders,
    SUM(CASE WHEN status = 6 THEN 1 ELSE 0 END) as completed_orders,
    SUM(CASE WHEN status = 7 THEN 1 ELSE 0 END) as cancelled_orders,
    SUM(CASE WHEN status IN (8, 9) THEN 1 ELSE 0 END) as refund_orders,
    SUM(total_amount) as total_amount,
    SUM(CASE WHEN status >= 2 THEN total_amount ELSE 0 END) as paid_amount,
    AVG(total_amount) as average_order_amount,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 END) as orders_this_week,
    COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as orders_this_month
FROM orders 
WHERE deleted = 0;

-- 用户订单统计视图
CREATE VIEW v_user_order_statistics AS
SELECT 
    u.id as user_id,
    u.username,
    COUNT(o.id) as total_orders,
    SUM(o.total_amount) as total_amount,
    SUM(CASE WHEN o.status = 6 THEN o.total_amount ELSE 0 END) as completed_amount,
    AVG(o.total_amount) as average_order_amount,
    MAX(o.created_at) as last_order_time,
    COUNT(CASE WHEN o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as orders_this_month
FROM users u
LEFT JOIN orders o ON u.id = o.user_id AND o.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id;