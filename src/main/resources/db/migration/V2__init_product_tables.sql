-- 文件级分析：
-- - 职责：创建商品相关数据表结构，为商品管理模块提供数据存储基础
-- - 位置考虑：放在db/migration目录下，版本号V2表示第二个数据库迁移
-- - 命名原因：init_product_tables表明初始化商品相关表
-- - 执行时机：V1用户表创建后执行，建立商品模块的数据基础
--
-- 设计思路：
-- 1. 创建分类表，支持无限级分类结构
-- 2. 创建商品表，包含完整的商品信息和状态管理
-- 3. 创建商品图片表，支持多种类型的图片管理
-- 4. 建立合理的外键关系和索引优化
-- 5. 插入基础测试数据，便于开发调试

-- ================================================================================
-- 商品分类表（categories）
-- 支持多级分类的树形结构，用于商品分类管理和导航
-- ================================================================================

CREATE TABLE categories (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    
    -- ======================== 基本信息字段 ========================
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NULL UNIQUE COMMENT '分类编码，用于系统内部识别',
    description VARCHAR(500) NULL COMMENT '分类描述',
    icon VARCHAR(200) NULL COMMENT '分类图标URL或类名',
    image_url VARCHAR(500) NULL COMMENT '分类封面图片URL',
    
    -- ======================== 层级关系字段 ========================
    parent_id BIGINT NULL COMMENT '父分类ID，根分类为NULL',
    level INT NOT NULL DEFAULT 1 COMMENT '分类层级，从1开始',
    path VARCHAR(500) NULL COMMENT '分类路径，如/1/2/3/',
    path_name VARCHAR(1000) NULL COMMENT '分类名称路径，如/电子产品/手机/',
    
    -- ======================== 显示控制字段 ========================
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序权重，数值越小越靠前',
    is_visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    is_leaf TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否叶子节点：0-否，1-是',
    
    -- ======================== 统计信息字段 ========================
    product_count INT NOT NULL DEFAULT 0 COMMENT '商品数量',
    children_count INT NOT NULL DEFAULT 0 COMMENT '子分类数量',
    
    -- ======================== SEO字段 ========================
    seo_keywords VARCHAR(500) NULL COMMENT 'SEO关键词',
    seo_description VARCHAR(500) NULL COMMENT 'SEO描述',
    
    -- ======================== 扩展字段 ========================
    attributes JSON NULL COMMENT '扩展属性，JSON格式',
    
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
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
    
    -- 检查约束
    CONSTRAINT chk_categories_level CHECK (level >= 1),
    CONSTRAINT chk_categories_is_visible CHECK (is_visible IN (0, 1)),
    CONSTRAINT chk_categories_is_leaf CHECK (is_leaf IN (0, 1)),
    CONSTRAINT chk_categories_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='商品分类表';

-- ================================================================================
-- 商品表（products）
-- 存储商品的完整信息，包括基本信息、价格、库存、统计等
-- ================================================================================

CREATE TABLE products (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    
    -- ======================== 基本信息字段 ========================
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    sku VARCHAR(100) NULL UNIQUE COMMENT '商品编码/SKU',
    brand VARCHAR(100) NULL COMMENT '商品品牌',
    model VARCHAR(100) NULL COMMENT '商品型号',
    summary VARCHAR(500) NULL COMMENT '商品简介',
    description TEXT NULL COMMENT '商品详情描述',
    
    -- 分类关联
    category_id BIGINT NOT NULL COMMENT '商品分类ID',
    
    -- 商品状态：1-草稿，2-待审核，3-上架，4-下架，5-缺货，6-停产
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态',
    
    -- ======================== 价格信息字段 ========================
    original_price DECIMAL(12,2) NULL COMMENT '商品原价',
    sale_price DECIMAL(12,2) NOT NULL COMMENT '商品售价',
    cost_price DECIMAL(12,2) NULL COMMENT '商品成本价',
    
    -- ======================== 库存信息字段 ========================
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存总量',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    reserved_stock INT NOT NULL DEFAULT 0 COMMENT '预占库存',
    low_stock_threshold INT NULL DEFAULT 10 COMMENT '库存预警值',
    
    -- ======================== 物理属性字段 ========================
    weight DECIMAL(10,2) NULL COMMENT '商品重量（克）',
    length DECIMAL(10,2) NULL COMMENT '商品长度（厘米）',
    width DECIMAL(10,2) NULL COMMENT '商品宽度（厘米）',
    height DECIMAL(10,2) NULL COMMENT '商品高度（厘米）',
    
    -- ======================== 营销信息字段 ========================
    tags VARCHAR(500) NULL COMMENT '商品标签，逗号分隔',
    keywords VARCHAR(500) NULL COMMENT '搜索关键词',
    is_recommend TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
    recommend_level INT NULL DEFAULT 0 COMMENT '推荐等级，0-10',
    is_new TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否新品：0-否，1-是',
    is_hot TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否热销：0-否，1-是',
    
    -- ======================== 统计信息字段 ========================
    sales_volume INT NOT NULL DEFAULT 0 COMMENT '销售数量',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
    review_count INT NOT NULL DEFAULT 0 COMMENT '评价总数',
    average_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '平均评分',
    
    -- ======================== 时间信息字段 ========================
    published_at DATETIME NULL COMMENT '上架时间',
    unpublished_at DATETIME NULL COMMENT '下架时间',
    
    -- ======================== 扩展信息字段 ========================
    specifications JSON NULL COMMENT '商品规格参数',
    attributes JSON NULL COMMENT '商品属性',
    seo_keywords VARCHAR(500) NULL COMMENT 'SEO关键词',
    seo_description VARCHAR(500) NULL COMMENT 'SEO描述',
    
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
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    
    -- 检查约束
    CONSTRAINT chk_products_status CHECK (status IN (1, 2, 3, 4, 5, 6)),
    CONSTRAINT chk_products_sale_price CHECK (sale_price >= 0.01),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0 AND available_stock >= 0 AND reserved_stock >= 0),
    CONSTRAINT chk_products_rating CHECK (average_rating >= 0.0 AND average_rating <= 5.0),
    CONSTRAINT chk_products_recommend_level CHECK (recommend_level >= 0 AND recommend_level <= 10),
    CONSTRAINT chk_products_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='商品表';

-- ================================================================================
-- 商品图片表（product_images）
-- 存储商品的图片信息，支持多种类型和尺寸
-- ================================================================================

CREATE TABLE product_images (
    -- 主键，使用自增ID
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    
    -- 关联商品
    product_id BIGINT NOT NULL COMMENT '商品ID',
    
    -- ======================== 图片信息字段 ========================
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL地址',
    thumbnail_url VARCHAR(500) NULL COMMENT '缩略图URL',
    medium_url VARCHAR(500) NULL COMMENT '中等尺寸图片URL',
    large_url VARCHAR(500) NULL COMMENT '大尺寸图片URL',
    
    title VARCHAR(200) NULL COMMENT '图片标题',
    description VARCHAR(500) NULL COMMENT '图片描述',
    
    -- ======================== 图片类型字段 ========================
    image_type TINYINT NOT NULL DEFAULT 2 COMMENT '图片类型：1-主图，2-详情图，3-规格图',
    is_main TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主图：0-否，1-是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    
    -- ======================== 图片属性字段 ========================
    width INT NULL COMMENT '图片宽度（像素）',
    height INT NULL COMMENT '图片高度（像素）',
    file_size BIGINT NULL COMMENT '文件大小（字节）',
    format VARCHAR(10) NULL COMMENT '图片格式',
    original_filename VARCHAR(255) NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) NULL COMMENT '存储路径',
    
    -- ======================== 规格相关字段 ========================
    color VARCHAR(7) NULL COMMENT '颜色值（十六进制）',
    spec_value VARCHAR(100) NULL COMMENT '规格值',
    
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
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    
    -- 检查约束
    CONSTRAINT chk_product_images_type CHECK (image_type IN (1, 2, 3)),
    CONSTRAINT chk_product_images_is_main CHECK (is_main IN (0, 1)),
    CONSTRAINT chk_product_images_deleted CHECK (deleted IN (0, 1))
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='商品图片表';

-- ================================================================================
-- 创建索引，优化查询性能
-- ================================================================================

-- 分类表索引
CREATE INDEX idx_categories_parent_deleted ON categories(parent_id, deleted);
CREATE INDEX idx_categories_level_sort ON categories(level, sort_order);
CREATE INDEX idx_categories_path ON categories(path);
CREATE INDEX idx_categories_visible ON categories(is_visible, deleted);

-- 商品表索引
CREATE INDEX idx_products_category_status ON products(category_id, status, deleted);
CREATE INDEX idx_products_status_stock ON products(status, available_stock, deleted);
CREATE INDEX idx_products_price_range ON products(sale_price, deleted);
CREATE INDEX idx_products_sales_volume ON products(sales_volume DESC, deleted);
CREATE INDEX idx_products_created_time ON products(created_at DESC, deleted);
CREATE INDEX idx_products_recommend ON products(is_recommend, recommend_level DESC, deleted);
CREATE INDEX idx_products_search ON products(name, brand, keywords, deleted);

-- 商品图片表索引
CREATE INDEX idx_product_images_product_sort ON product_images(product_id, sort_order, deleted);
CREATE INDEX idx_product_images_main ON product_images(product_id, is_main, deleted);
CREATE INDEX idx_product_images_type ON product_images(image_type, deleted);

-- ================================================================================
-- 插入初始数据
-- ================================================================================

-- 插入根分类数据
INSERT INTO categories (name, code, description, level, path, path_name, sort_order, is_visible, created_by, updated_by) VALUES
('电子产品', 'electronics', '电子产品和数码设备', 1, '/1/', '/电子产品/', 1, 1, 1, 1),
('服装鞋帽', 'clothing', '服装、鞋子和配饰', 1, '/2/', '/服装鞋帽/', 2, 1, 1, 1),
('家居用品', 'home', '家居装饰和生活用品', 1, '/3/', '/家居用品/', 3, 1, 1, 1),
('图书音像', 'books', '图书、音乐和影视产品', 1, '/4/', '/图书音像/', 4, 1, 1, 1),
('运动户外', 'sports', '运动器材和户外用品', 1, '/5/', '/运动户外/', 5, 1, 1, 1);

-- 插入二级分类数据
INSERT INTO categories (name, code, description, parent_id, level, path, path_name, sort_order, is_visible, is_leaf, created_by, updated_by) VALUES
-- 电子产品子分类
('手机通讯', 'mobile', '智能手机和通讯设备', 1, 2, '/1/6/', '/电子产品/手机通讯/', 1, 1, 1, 1, 1),
('电脑办公', 'computer', '电脑和办公设备', 1, 2, '/1/7/', '/电子产品/电脑办公/', 2, 1, 1, 1, 1),
('数码相机', 'camera', '相机和摄影设备', 1, 2, '/1/8/', '/电子产品/数码相机/', 3, 1, 1, 1, 1),

-- 服装鞋帽子分类
('男装', 'mens-clothing', '男士服装', 2, 2, '/2/9/', '/服装鞋帽/男装/', 1, 1, 1, 1, 1),
('女装', 'womens-clothing', '女士服装', 2, 2, '/2/10/', '/服装鞋帽/女装/', 2, 1, 1, 1, 1),
('鞋靴', 'shoes', '各类鞋靴', 2, 2, '/2/11/', '/服装鞋帽/鞋靴/', 3, 1, 1, 1, 1),

-- 家居用品子分类
('家具', 'furniture', '各类家具', 3, 2, '/3/12/', '/家居用品/家具/', 1, 1, 1, 1, 1),
('家纺', 'textiles', '床上用品和家纺', 3, 2, '/3/13/', '/家居用品/家纺/', 2, 1, 1, 1, 1),
('厨具', 'kitchenware', '厨房用具', 3, 2, '/3/14/', '/家居用品/厨具/', 3, 1, 1, 1, 1);

-- 更新分类统计信息
UPDATE categories SET children_count = 3 WHERE id IN (1, 2, 3);

-- 插入测试商品数据
INSERT INTO products (
    name, sku, brand, summary, description, category_id, status, 
    sale_price, original_price, cost_price, 
    stock_quantity, available_stock, 
    is_recommend, is_new, 
    published_at, created_by, updated_by
) VALUES
('iPhone 15 Pro 256GB 深空黑', 'IP15P-256-BLACK', 'Apple', 
'Apple iPhone 15 Pro，搭载A17 Pro芯片，钛金属设计，专业级摄像系统', 
'iPhone 15 Pro 采用钛金属设计，搭载强大的A17 Pro芯片，配备专业级摄像系统，支持Action Button动作按钮，提供卓越的性能和拍摄体验。', 
6, 3, 8999.00, 9999.00, 7200.00, 100, 100, 1, 1, NOW(), 1, 1),

('MacBook Pro 14英寸 M3芯片', 'MBP14-M3-512', 'Apple', 
'MacBook Pro 14英寸，搭载M3芯片，512GB存储，适合专业创作', 
'全新MacBook Pro 14英寸搭载强大的M3芯片，配备14英寸Liquid Retina XDR显示屏，512GB SSD存储，为专业用户提供卓越的性能和续航能力。', 
7, 3, 15999.00, 16999.00, 12800.00, 50, 50, 1, 1, NOW(), 1, 1),

('华为Mate 60 Pro 512GB 雅川青', 'HW-M60P-512-GREEN', '华为', 
'华为Mate 60 Pro，麒麟芯片回归，专业摄影旗舰', 
'华为Mate 60 Pro搭载全新麒麟芯片，配备专业级影像系统，支持卫星通话，拥有优雅的雅川青配色，是华为最新旗舰产品。', 
6, 3, 6999.00, 7999.00, 5600.00, 80, 80, 1, 1, NOW(), 1, 1),

('小米14 Ultra 16GB+1TB 白色', 'MI14U-16-1TB-WHITE', '小米', 
'小米14 Ultra，徕卡光学镜头，骁龙8 Gen3', 
'小米14 Ultra配备徕卡专业光学镜头，搭载骁龙8 Gen3处理器，16GB+1TB超大存储组合，白色陶瓷机身，为摄影爱好者打造的影像旗舰。', 
6, 3, 6499.00, 6999.00, 5200.00, 120, 120, 1, 1, NOW(), 1, 1),

('联想ThinkPad X1 Carbon Gen11', 'TP-X1C-G11-I7', '联想', 
'ThinkPad X1 Carbon 第11代，商务办公首选', 
'联想ThinkPad X1 Carbon第11代，采用英特尔第13代酷睿处理器，碳纤维机身，14英寸2.8K显示屏，为商务用户提供轻薄便携的办公体验。', 
7, 3, 12999.00, 13999.00, 10400.00, 30, 30, 1, 0, NOW(), 1, 1);

-- 插入商品图片数据
INSERT INTO product_images (
    product_id, image_url, thumbnail_url, title, image_type, is_main, sort_order, 
    width, height, format, created_by, updated_by
) VALUES
-- iPhone 15 Pro 图片
(1, '/uploads/products/iphone15pro/main.jpg', '/uploads/products/iphone15pro/thumb.jpg', 'iPhone 15 Pro 主图', 1, 1, 0, 800, 800, 'jpg', 1, 1),
(1, '/uploads/products/iphone15pro/detail1.jpg', '/uploads/products/iphone15pro/detail1_thumb.jpg', 'iPhone 15 Pro 细节图1', 2, 0, 1, 800, 600, 'jpg', 1, 1),
(1, '/uploads/products/iphone15pro/detail2.jpg', '/uploads/products/iphone15pro/detail2_thumb.jpg', 'iPhone 15 Pro 细节图2', 2, 0, 2, 800, 600, 'jpg', 1, 1),

-- MacBook Pro 图片
(2, '/uploads/products/macbookpro14/main.jpg', '/uploads/products/macbookpro14/thumb.jpg', 'MacBook Pro 14 主图', 1, 1, 0, 800, 600, 'jpg', 1, 1),
(2, '/uploads/products/macbookpro14/detail1.jpg', '/uploads/products/macbookpro14/detail1_thumb.jpg', 'MacBook Pro 14 细节图1', 2, 0, 1, 800, 600, 'jpg', 1, 1),

-- 华为Mate 60 Pro 图片
(3, '/uploads/products/mate60pro/main.jpg', '/uploads/products/mate60pro/thumb.jpg', '华为Mate 60 Pro 主图', 1, 1, 0, 800, 800, 'jpg', 1, 1),
(3, '/uploads/products/mate60pro/detail1.jpg', '/uploads/products/mate60pro/detail1_thumb.jpg', '华为Mate 60 Pro 细节图1', 2, 0, 1, 800, 600, 'jpg', 1, 1),

-- 小米14 Ultra 图片
(4, '/uploads/products/mi14ultra/main.jpg', '/uploads/products/mi14ultra/thumb.jpg', '小米14 Ultra 主图', 1, 1, 0, 800, 800, 'jpg', 1, 1),
(4, '/uploads/products/mi14ultra/detail1.jpg', '/uploads/products/mi14ultra/detail1_thumb.jpg', '小米14 Ultra 细节图1', 2, 0, 1, 800, 600, 'jpg', 1, 1),

-- ThinkPad X1 Carbon 图片
(5, '/uploads/products/thinkpadx1/main.jpg', '/uploads/products/thinkpadx1/thumb.jpg', 'ThinkPad X1 Carbon 主图', 1, 1, 0, 800, 600, 'jpg', 1, 1),
(5, '/uploads/products/thinkpadx1/detail1.jpg', '/uploads/products/thinkpadx1/detail1_thumb.jpg', 'ThinkPad X1 Carbon 细节图1', 2, 0, 1, 800, 600, 'jpg', 1, 1);

-- 更新分类商品统计
UPDATE categories SET product_count = 4 WHERE id = 6; -- 手机通讯
UPDATE categories SET product_count = 2 WHERE id = 7; -- 电脑办公
UPDATE categories SET product_count = 6 WHERE id = 1; -- 电子产品总数

-- ================================================================================
-- 创建视图，便于查询
-- ================================================================================

-- 商品列表视图（包含分类信息）
CREATE VIEW v_product_list AS
SELECT 
    p.id,
    p.name,
    p.sku,
    p.brand,
    p.summary,
    p.sale_price,
    p.original_price,
    p.status,
    p.stock_quantity,
    p.available_stock,
    p.sales_volume,
    p.average_rating,
    p.is_recommend,
    p.is_new,
    p.is_hot,
    p.published_at,
    p.created_at,
    c.name as category_name,
    c.path_name as category_path,
    (SELECT pi.image_url FROM product_images pi 
     WHERE pi.product_id = p.id AND pi.is_main = 1 AND pi.deleted = 0 
     LIMIT 1) as main_image_url
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
WHERE p.deleted = 0 AND c.deleted = 0;

-- 商品统计视图
CREATE VIEW v_product_statistics AS
SELECT 
    COUNT(*) as total_products,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as active_products,
    SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as inactive_products,
    SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as out_of_stock_products,
    SUM(CASE WHEN is_recommend = 1 THEN 1 ELSE 0 END) as recommend_products,
    SUM(CASE WHEN is_new = 1 THEN 1 ELSE 0 END) as new_products,
    SUM(stock_quantity) as total_stock,
    SUM(sales_volume) as total_sales,
    AVG(sale_price) as average_price,
    COUNT(CASE WHEN available_stock <= low_stock_threshold THEN 1 END) as low_stock_products
FROM products 
WHERE deleted = 0;