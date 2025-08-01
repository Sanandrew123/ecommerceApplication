/*
文件级分析：
- 职责：商品实体类，映射数据库中的商品表，是电商系统的核心实体
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：Product是电商领域的核心概念，命名简洁明确
- 调用关系：关联Category分类，被OrderItem订单项引用，包含ProductImage图片

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 包含商品的完整信息：基本信息、价格、库存、规格等
3. 支持多规格商品设计（SPU/SKU概念）
4. 集成搜索优化字段，为Elasticsearch做准备
5. 包含营销相关字段，支持促销活动
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.ecommerce.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品实体类
 * 
 * 电商系统的核心实体，包含商品的完整信息：
 * 1. 基本信息：名称、描述、分类等
 * 2. 价格信息：原价、售价、成本价等
 * 3. 库存信息：总库存、可用库存、预占库存
 * 4. 规格信息：颜色、尺寸、重量等
 * 5. 营销信息：标签、推荐等级、促销标记
 * 6. 统计信息：销量、评分、浏览量等
 * 
 * SPU/SKU设计思路：
 * - 当前Product可以作为SPU（标准化产品单元）
 * - 如果商品有多个规格（颜色、尺寸等），可扩展ProductSku表
 * - 简单商品直接使用Product，复杂商品使用Product+ProductSku
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"category", "images"})
@ToString(callSuper = true, exclude = {"category", "images"})
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_category_id", columnList = "category_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_price", columnList = "sale_price"),
        @Index(name = "idx_sales_volume", columnList = "sales_volume DESC"),
        @Index(name = "idx_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_is_recommend", columnList = "is_recommend"),
        @Index(name = "idx_search", columnList = "name, brand, keywords")
})
public class Product extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 商品名称
     * 商品的完整名称，用于展示和搜索
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 200, message = "商品名称长度必须在1-200字符之间")
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    /**
     * 商品编码/SKU
     * 商品的唯一标识符，用于库存管理和订单处理
     */
    @Size(max = 100, message = "商品编码长度不能超过100字符")
    @Column(name = "sku", unique = true, length = 100)
    private String sku;
    
    /**
     * 商品品牌
     * 商品所属品牌，用于筛选和搜索
     */
    @Size(max = 100, message = "品牌名称长度不能超过100字符")
    @Column(name = "brand", length = 100)
    private String brand;
    
    /**
     * 商品型号
     * 商品的具体型号，用于区分同品牌不同款式
     */
    @Size(max = 100, message = "商品型号长度不能超过100字符")
    @Column(name = "model", length = 100)
    private String model;
    
    /**
     * 商品简介
     * 商品的简短描述，用于列表页展示
     */
    @Size(max = 500, message = "商品简介长度不能超过500字符")
    @Column(name = "summary", length = 500)
    private String summary;
    
    /**
     * 商品详情
     * 商品的详细描述，支持富文本格式
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 商品分类
     * 多对一关联，每个商品属于一个分类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_category"))
    @NotNull(message = "商品分类不能为空")
    private Category category;
    
    /**
     * 商品状态
     * 使用枚举映射，控制商品的生命周期
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;
    
    // ======================== 价格信息字段 ========================
    
    /**
     * 商品原价
     * 商品的标准售价，用于显示划线价格
     */
    @DecimalMin(value = "0.00", message = "商品原价不能为负数")
    @Digits(integer = 10, fraction = 2, message = "商品原价格式不正确")
    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;
    
    /**
     * 商品售价
     * 商品的实际销售价格，用户购买时的价格
     */
    @NotNull(message = "商品售价不能为空")
    @DecimalMin(value = "0.01", message = "商品售价必须大于0")
    @Digits(integer = 10, fraction = 2, message = "商品售价格式不正确")
    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;
    
    /**
     * 商品成本价
     * 商品的采购或生产成本，用于利润计算
     */
    @DecimalMin(value = "0.00", message = "商品成本价不能为负数")
    @Digits(integer = 10, fraction = 2, message = "商品成本价格式不正确")
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;
    
    // ======================== 库存信息字段 ========================
    
    /**
     * 商品库存总量
     * 商品的总库存数量
     */
    @Min(value = 0, message = "商品库存不能为负数")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;
    
    /**
     * 可用库存
     * 实际可销售的库存数量（总库存 - 预占库存）
     */
    @Min(value = 0, message = "可用库存不能为负数")
    @Column(name = "available_stock", nullable = false)
    private Integer availableStock = 0;
    
    /**
     * 预占库存
     * 已下单但未支付的商品数量
     */
    @Min(value = 0, message = "预占库存不能为负数")
    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;
    
    /**
     * 库存预警值
     * 当可用库存低于此值时触发预警
     */
    @Min(value = 0, message = "库存预警值不能为负数")
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;
    
    // ======================== 物理属性字段 ========================
    
    /**
     * 商品重量（克）
     * 用于运费计算和物流配送
     */
    @DecimalMin(value = "0.00", message = "商品重量不能为负数")
    @Digits(integer = 8, fraction = 2, message = "商品重量格式不正确")
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    /**
     * 商品长度（厘米）
     */
    @DecimalMin(value = "0.00", message = "商品长度不能为负数")
    @Digits(integer = 8, fraction = 2, message = "商品长度格式不正确")
    @Column(name = "length", precision = 10, scale = 2)
    private BigDecimal length;
    
    /**
     * 商品宽度（厘米）
     */
    @DecimalMin(value = "0.00", message = "商品宽度不能为负数")
    @Digits(integer = 8, fraction = 2, message = "商品宽度格式不正确")
    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width;
    
    /**
     * 商品高度（厘米）
     */
    @DecimalMin(value = "0.00", message = "商品高度不能为负数")
    @Digits(integer = 8, fraction = 2, message = "商品高度格式不正确")
    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;
    
    // ======================== 营销信息字段 ========================
    
    /**
     * 商品标签
     * 商品的营销标签，如"热销"、"新品"等，用逗号分隔
     */
    @Size(max = 500, message = "商品标签长度不能超过500字符")
    @Column(name = "tags", length = 500)
    private String tags;
    
    /**
     * 搜索关键词
     * 用于商品搜索的关键词，提高搜索命中率
     */
    @Size(max = 500, message = "搜索关键词长度不能超过500字符")
    @Column(name = "keywords", length = 500)
    private String keywords;
    
    /**
     * 是否推荐
     * 标记商品是否为推荐商品，用于首页或分类页展示
     */
    @Column(name = "is_recommend", nullable = false)
    private Boolean isRecommend = false;
    
    /**
     * 推荐等级
     * 推荐商品的等级，数值越大优先级越高
     */
    @Min(value = 0, message = "推荐等级不能为负数")
    @Max(value = 10, message = "推荐等级不能超过10")
    @Column(name = "recommend_level")
    private Integer recommendLevel = 0;
    
    /**
     * 是否新品
     * 标记商品是否为新品，用于新品推荐
     */
    @Column(name = "is_new", nullable = false)
    private Boolean isNew = false;
    
    /**
     * 是否热销
     * 标记商品是否为热销商品
     */
    @Column(name = "is_hot", nullable = false)
    private Boolean isHot = false;
    
    // ======================== 统计信息字段 ========================
    
    /**
     * 销售数量
     * 商品的总销售数量
     */
    @Min(value = 0, message = "销售数量不能为负数")
    @Column(name = "sales_volume", nullable = false)
    private Integer salesVolume = 0;
    
    /**
     * 浏览次数
     * 商品详情页的访问次数
     */
    @Min(value = 0, message = "浏览次数不能为负数")
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
    
    /**
     * 收藏次数
     * 用户收藏该商品的次数
     */
    @Min(value = 0, message = "收藏次数不能为负数")
    @Column(name = "favorite_count", nullable = false)
    private Integer favoriteCount = 0;
    
    /**
     * 评价总数
     * 商品收到的评价总数
     */
    @Min(value = 0, message = "评价总数不能为负数")
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;
    
    /**
     * 平均评分
     * 商品的平均评分，范围1-5分
     */
    @DecimalMin(value = "0.0", message = "评分不能为负数")
    @DecimalMax(value = "5.0", message = "评分不能超过5分")
    @Digits(integer = 1, fraction = 1, message = "评分格式不正确")
    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    // ======================== 时间信息字段 ========================
    
    /**
     * 上架时间
     * 商品首次上架的时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    /**
     * 下架时间
     * 商品下架的时间
     */
    @Column(name = "unpublished_at")
    private LocalDateTime unpublishedAt;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * 商品规格
     * JSON格式存储商品的规格参数，如颜色、尺寸等
     */
    @Column(name = "specifications", columnDefinition = "JSON")
    private String specifications;
    
    /**
     * 商品属性
     * JSON格式存储商品的额外属性信息
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    /**
     * SEO关键词
     * 用于搜索引擎优化的关键词
     */
    @Size(max = 500, message = "SEO关键词长度不能超过500字符")
    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;
    
    /**
     * SEO描述
     * 用于搜索引擎优化的描述信息
     */
    @Size(max = 500, message = "SEO描述长度不能超过500字符")
    @Column(name = "seo_description", length = 500)
    private String seoDescription;
    
    // ======================== 关联关系 ========================
    
    /**
     * 商品图片列表
     * 一对多关联，一个商品可以有多张图片
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();
    
    // ======================== 业务方法 ========================
    
    /**
     * 判断商品是否可销售
     * 
     * @return true表示可销售，false表示不可销售
     */
    public boolean isSaleable() {
        return this.status != null && this.status.isSaleable() && this.availableStock > 0;
    }
    
    /**
     * 判断商品是否缺货
     * 
     * @return true表示缺货，false表示有库存
     */
    public boolean isOutOfStock() {
        return this.availableStock <= 0;
    }
    
    /**
     * 判断商品是否库存不足
     * 
     * @return true表示库存不足，false表示库存充足
     */
    public boolean isLowStock() {
        return this.lowStockThreshold != null && this.availableStock <= this.lowStockThreshold;
    }
    
    /**
     * 减少库存
     * 
     * @param quantity 减少的数量
     * @return true表示减少成功，false表示库存不足
     */
    public boolean reduceStock(int quantity) {
        if (quantity <= 0) {
            return false;
        }
        
        if (this.availableStock < quantity) {
            return false;
        }
        
        this.availableStock -= quantity;
        this.stockQuantity -= quantity;
        
        // 更新销售数量
        this.salesVolume += quantity;
        
        // 检查是否需要更新状态为缺货
        if (this.availableStock <= 0 && this.status == ProductStatus.ACTIVE) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
        
        return true;
    }
    
    /**
     * 增加库存
     * 
     * @param quantity 增加的数量
     */
    public void addStock(int quantity) {
        if (quantity > 0) {
            this.stockQuantity += quantity;
            this.availableStock += quantity;
            
            // 如果原来是缺货状态，恢复为上架状态
            if (this.status == ProductStatus.OUT_OF_STOCK && this.availableStock > 0) {
                this.status = ProductStatus.ACTIVE;
            }
        }
    }
    
    /**
     * 预占库存
     * 
     * @param quantity 预占数量
     * @return true表示预占成功，false表示库存不足
     */
    public boolean reserveStock(int quantity) {
        if (quantity <= 0) {
            return false;
        }
        
        if (this.availableStock < quantity) {
            return false;
        }
        
        this.availableStock -= quantity;
        this.reservedStock += quantity;
        
        return true;
    }
    
    /**
     * 释放预占库存
     * 
     * @param quantity 释放数量
     */
    public void releaseStock(int quantity) {
        if (quantity > 0 && this.reservedStock >= quantity) {
            this.reservedStock -= quantity;
            this.availableStock += quantity;
        }
    }
    
    /**
     * 增加浏览次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    /**
     * 增加收藏次数
     */
    public void incrementFavoriteCount() {
        this.favoriteCount++;
    }
    
    /**
     * 减少收藏次数
     */
    public void decrementFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
    }
    
    /**
     * 更新评分
     * 
     * @param rating 新评分
     * @param isNewReview 是否为新评价
     */
    public void updateRating(BigDecimal rating, boolean isNewReview) {
        if (isNewReview) {
            this.reviewCount++;
        }
        
        if (this.reviewCount > 0) {
            // 重新计算平均评分
            BigDecimal totalRating = this.averageRating.multiply(BigDecimal.valueOf(this.reviewCount - 1));
            totalRating = totalRating.add(rating);
            this.averageRating = totalRating.divide(BigDecimal.valueOf(this.reviewCount), 1, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * 上架商品
     */
    public void publish() {
        if (this.status == ProductStatus.INACTIVE || this.status == ProductStatus.DRAFT) {
            this.status = this.availableStock > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK;
            this.publishedAt = LocalDateTime.now();
            this.unpublishedAt = null;
        }
    }
    
    /**
     * 下架商品
     */
    public void unpublish() {
        if (this.status == ProductStatus.ACTIVE || this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.INACTIVE;
            this.unpublishedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 获取主图URL
     * 
     * @return 主图URL，如果没有图片则返回null
     */
    public String getMainImageUrl() {
        if (this.images != null && !this.images.isEmpty()) {
            return this.images.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(this.images.get(0).getImageUrl());
        }
        return null;
    }
    
    /**
     * 计算利润
     * 
     * @return 商品利润（售价 - 成本价）
     */
    public BigDecimal getProfit() {
        if (this.salePrice != null && this.costPrice != null) {
            return this.salePrice.subtract(this.costPrice);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * 计算利润率
     * 
     * @return 利润率百分比
     */
    public BigDecimal getProfitMargin() {
        if (this.salePrice != null && this.costPrice != null && this.salePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = this.salePrice.subtract(this.costPrice);
            return profit.divide(this.salePrice, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}