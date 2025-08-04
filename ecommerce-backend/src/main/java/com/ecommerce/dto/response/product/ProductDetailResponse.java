package com.ecommerce.dto.response.product;

/*
 * 文件职责: 商品详情响应DTO，封装商品详情页面的完整信息
 * 
 * 开发心理活动：
 * 1. 响应DTO设计原则：
 *    - 数据安全：过滤敏感字段，如成本价格
 *    - 性能优化：按需返回字段，减少数据传输
 *    - 版本兼容：支持API版本演进的兼容性
 *    - 文档完整：提供清晰的字段说明和示例
 * 
 * 2. 商品详情场景：
 *    - 用户浏览：完整的商品展示信息
 *    - 搜索引擎：结构化数据的SEO优化
 *    - 社交分享：Open Graph和Twitter Card
 *    - 移动端：响应式布局的数据适配
 * 
 * 3. 数据组织考虑：
 *    - 基础信息：名称、价格、库存、描述
 *    - 营销信息：折扣、标签、推荐理由
 *    - 多媒体：图片、视频、360度展示
 *    - 用户行为：评价、收藏、分享统计
 * 
 * 4. 权限控制设计：
 *    - 公开信息：所有用户可见
 *    - 会员信息：登录用户可见的会员价
 *    - 管理信息：后台管理员可见的运营数据
 *    - 统计信息：权限控制的敏感统计数据
 * 
 * 包结构设计思路:
 * - 放在dto.response.product包下，专门处理商品响应
 * - 与request DTO分离，输入输出职责清晰
 * 
 * 命名原因:
 * - ProductDetailResponse明确表达商品详情响应功能
 * - Detail强调完整详细的信息展示
 * 
 * 依赖关系:
 * - 由Controller返回，作为接口出参  
 * - 从Product实体转换，由Service组装
 * - 独立于实体，便于API演进
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品详情响应DTO
 * 
 * 功能说明：
 * 1. 封装商品详情页的完整展示信息
 * 2. 根据用户权限过滤敏感数据
 * 3. 优化数据结构便于前端渲染
 * 4. 支持多种展示场景的数据需求
 * 
 * 数据组织：
 * 1. 基础信息：商品标识、名称、描述
 * 2. 价格信息：多层级价格体系
 * 3. 库存信息：实时库存和可售状态
 * 4. 多媒体：图片、视频等展示资源
 * 
 * 营销信息：
 * 1. 促销标识：热门、推荐、新品标签
 * 2. 优惠信息：折扣幅度、节省金额
 * 3. 社会证明：销量、评价、收藏数
 * 4. 推荐理由：个性化推荐的解释
 * 
 * SEO优化：
 * 1. 结构化数据：便于搜索引擎理解
 * 2. 社交分享：Open Graph元数据
 * 3. 关键词优化：标题、描述、标签
 * 4. 链接友好：规范化的URL结构
 * 
 * 权限控制：
 * 1. 公开数据：所有用户可见
 * 2. 会员数据：登录用户专享信息
 * 3. 管理数据：后台权限控制字段
 * 4. 动态过滤：根据用户角色动态调整
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {

    // ========== 基础标识信息 ==========

    /**
     * 商品ID
     * 
     * 用途说明：
     * - 商品的唯一标识符
     * - 前端路由和状态管理的key
     * - 用户操作（收藏、分享）的标识
     * - 统计分析和埋点的商品标识
     */
    private Long id;

    /**
     * 商品编码
     * 
     * 业务价值：
     * - 商品的业务标识码
     * - 客服查询的快速入口
     * - 库存管理的标准标识
     * - 第三方系统对接的稳定ID
     */
    private String code;

    /**
     * 商品名称
     * 
     * 展示要求：
     * - 商品详情页的主标题
     * - 页面title和H1标签内容
     * - 社交分享的标题文本
     * - 搜索结果的主要展示文本
     */
    private String name;

    /**
     * 商品副标题
     * 
     * 营销价值：
     * - 商品卖点的补充说明
     * - 促销信息的展示位置
     * - 规格特色的简要描述
     * - 用户购买决策的辅助信息
     */
    private String subtitle;

    /**
     * 商品简述
     * 
     * 应用场景：
     * - 商品详情页的简要介绍
     * - 搜索结果的描述摘要
     * - 分享链接的预览描述
     * - 移动端的精简展示
     */
    private String summary;

    /**
     * 商品详细描述
     * 
     * 内容丰富：
     * - 商品的完整介绍和说明
     * - 支持HTML格式的富文本
     * - 包含使用方法和注意事项
     * - 提升用户购买信心的关键内容
     */
    private String description;

    // ========== 分类和品牌信息 ==========

    /**
     * 商品分类信息
     * 
     * 分类作用：
     * - 面包屑导航的构建数据
     * - 相关商品推荐的分类依据
     * - 用户浏览路径的跟踪标识
     * - SEO优化的分类标签
     */
    @Builder.Default
    private CategoryInfo category = new CategoryInfo();

    /**
     * 品牌名称
     * 
     * 品牌价值：
     * - 品牌认知和信任建立
     * - 品牌页面的链接入口
     * - 品牌筛选的标识依据
     * - 用户品牌偏好的数据来源
     */
    private String brand;

    /**
     * 商品标签列表
     * 
     * 标签系统：
     * - 商品特性的多维度标识
     * - 搜索筛选的标签依据
     * - 个性化推荐的特征数据
     * - 营销活动的商品分组
     */
    private List<String> tags;

    // ========== 价格信息 ==========

    /**
     * 价格信息
     * 
     * 价格体系：
     * - 多层级的价格展示
     * - 折扣和优惠的计算结果
     * - 会员权益的价格差异
     * - 促销活动的价格策略
     */
    @Builder.Default
    private PriceInfo priceInfo = new PriceInfo();

    // ========== 库存和销售信息 ==========

    /**
     * 库存信息
     * 
     * 库存展示：
     * - 实时库存状态和数量
     * - 库存紧张的预警提示
     * - 缺货预估的补货时间
     * - 库存充足的购买鼓励
     */
    @Builder.Default
    private StockInfo stockInfo = new StockInfo();

    /**
     * 销售统计
     * 
     * 统计价值：
     * - 社会证明的数据支撑
     * - 用户购买决策的参考
     * - 商品热度的量化展示
     * - 推荐算法的权重因子
     */
    @Builder.Default
    private SalesInfo salesInfo = new SalesInfo();

    // ========== 商品属性 ==========

    /**
     * 商品状态
     * 
     * 状态含义：
     * - ACTIVE: 正常销售中
     * - INACTIVE: 暂时下架
     * - OUT_OF_STOCK: 库存不足
     * - DISCONTINUED: 停产停售
     */
    private String status;

    /**
     * 商品属性标识
     * 
     * 属性说明：
     * - 虚拟商品：无需物流配送
     * - 需要配送：影响运费计算
     * - 可单独购买：购物车验证规则
     */
    @Builder.Default
    private ProductAttributes attributes = new ProductAttributes();

    // ========== 物理属性 ==========

    /**
     * 物理规格
     * 
     * 规格用途：
     * - 物流费用的计算依据
     * - 包装方案的选择参考
     * - 商品对比的标准参数
     * - 用户选择的决策信息
     */
    @Builder.Default
    private PhysicalSpecs physicalSpecs = new PhysicalSpecs();

    // ========== 多媒体资源 ==========

    /**
     * 商品主图
     * 
     * 主图要求：
     * - 高清晰度的商品展示图
     * - 统一规格的图片尺寸
     * - CDN加速的图片服务
     * - 支持WebP格式的现代浏览器
     */
    private String mainImage;

    /**
     * 商品图片列表
     * 
     * 图片集合：
     * - 多角度的商品展示
     * - 细节特写的高清图片
     * - 使用场景的情境图片
     * - 规格对比的参照图片
     */
    private List<String> images;

    /**
     * 商品视频
     * 
     * 视频价值：
     * - 动态展示的商品效果
     * - 使用方法的演示教程
     * - 提升转化率的营销工具
     * - 减少退货率的有效手段
     */
    private String videoUrl;

    // ========== 营销标识 ==========

    /**
     * 营销标签
     * 
     * 标签作用：
     * - 吸引用户注意的视觉标识
     * - 营销活动的商品标记
     * - 个性化推荐的展示理由
     * - 用户购买决策的心理暗示
     */
    @Builder.Default
    private MarketingLabels marketingLabels = new MarketingLabels();

    /**
     * 推荐权重
     * 
     * 权重说明：
     * - 推荐系统的排序依据
     * - 商品曝光优先级的控制
     * - 个性化推荐的权重因子
     * - A/B测试的流量分配参数
     */
    private Integer recommendWeight;

    // ========== 规格参数 ==========

    /**
     * 商品规格参数
     * 
     * 规格展示：
     * - 结构化的参数列表
     * - 规格对比的标准数据
     * - 筛选功能的匹配依据
     * - 用户选择的参考信息
     * 
     * 数据格式：
     * {
     *   "颜色": "红色",
     *   "尺寸": "XL",
     *   "材质": "纯棉"
     * }
     */
    private Map<String, String> specifications;

    /**
     * 商品属性参数
     * 
     * 属性展示：
     * - 商品的扩展属性信息
     * - 营销卖点的结构化描述
     * - 使用场景的详细说明
     * - 保养方法的指导信息
     */
    private Map<String, String> productAttributes;

    // ========== SEO信息 ==========

    /**
     * SEO优化信息
     * 
     * SEO价值：
     * - 搜索引擎优化的元数据
     * - 社交分享的结构化信息
     * - 页面标题和描述的标准化
     * - 关键词排名的优化支撑
     */
    @Builder.Default
    private SeoInfo seoInfo = new SeoInfo();

    // ========== 时间信息 ==========

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 上架时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    // ========== 扩展信息 ==========

    /**
     * 扩展信息
     * 
     * 扩展用途：
     * - 特殊业务需求的数据存储
     * - A/B测试的配置参数
     * - 第三方集成的附加数据
     * - 未来功能的预留空间
     */
    private Map<String, Object> extraInfo;

    // ========== 内部类定义 ==========

    /**
     * 分类信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryInfo {
        /**
         * 分类ID
         */
        private Long id;
        
        /**
         * 分类名称
         */
        private String name;
        
        /**
         * 分类编码
         */
        private String code;
        
        /**
         * 分类层级
         */
        private Integer level;
        
        /**
         * 分类路径
         */
        private String path;
        
        /**
         * 父分类信息
         */
        private CategoryInfo parent;
        
        /**
         * 完整分类路径名称
         * 如："服装 > 男装 > 衬衫"
         */
        private String fullName;
        
        /**
         * 分类URL路径
         */
        private String urlPath;
    }

    /**
     * 价格信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceInfo {
        /**
         * 商品原价
         */
        private BigDecimal originalPrice;
        
        /**
         * 商品现价
         */
        private BigDecimal currentPrice;
        
        /**
         * 会员价格
         */
        private BigDecimal memberPrice;
        
        /**
         * 折扣比例 (0-1之间)
         */
        private BigDecimal discountRate;
        
        /**
         * 节省金额
         */
        private BigDecimal savedAmount;
        
        /**
         * 是否促销商品
         */
        private Boolean onSale;
        
        /**
         * 价格展示文本
         */
        private String priceText;
        
        /**
         * 折扣展示文本
         */
        private String discountText;
    }

    /**
     * 库存信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StockInfo {
        /**
         * 总库存
         */
        private Integer totalStock;
        
        /**
         * 可用库存
         */
        private Integer availableStock;
        
        /**
         * 预占库存
         */
        private Integer reservedStock;
        
        /**
         * 库存预警阈值
         */
        private Integer lowStockThreshold;
        
        /**
         * 是否有库存
         */
        private Boolean hasStock;
        
        /**
         * 是否库存充足
         */
        private Boolean stockSufficient;
        
        /**
         * 是否需要库存预警
         */
        private Boolean needsLowStockAlert;
        
        /**
         * 库存状态文本
         */
        private String stockStatusText;
        
        /**
         * 预计补货时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime estimatedRestockDate;
    }

    /**
     * 销售统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SalesInfo {
        /**
         * 销售数量
         */
        private Long salesCount;
        
        /**
         * 浏览次数
         */
        private Long viewCount;
        
        /**
         * 收藏次数
         */
        private Long favoriteCount;
        
        /**
         * 评价数量
         */
        private Long reviewCount;
        
        /**
         * 平均评分
         */
        private BigDecimal averageRating;
        
        /**
         * 评分星级 (1-5星)
         */
        private Integer starRating;
        
        /**
         * 好评率
         */
        private BigDecimal positiveRate;
        
        /**
         * 销售排名
         */
        private Integer salesRank;
        
        /**
         * 热度指数
         */
        private Integer popularityIndex;
    }

    /**
     * 商品属性标识
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductAttributes {
        /**
         * 是否为虚拟商品
         */
        private Boolean isVirtual;
        
        /**
         * 是否需要配送
         */
        private Boolean requiresShipping;
        
        /**
         * 是否可单独购买
         */
        private Boolean canBuyAlone;
        
        /**
         * 是否支持货到付款
         */
        private Boolean supportsCod;
        
        /**
         * 是否支持退货
         */
        private Boolean supportsReturn;
        
        /**
         * 退货期限（天）
         */
        private Integer returnPeriod;
        
        /**
         * 是否包邮
         */
        private Boolean freeShipping;
        
        /**
         * 发货时间（小时）
         */
        private Integer shippingTime;
    }

    /**
     * 物理规格
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhysicalSpecs {
        /**
         * 重量（克）
         */
        private BigDecimal weight;
        
        /**
         * 长度（厘米）
         */
        private BigDecimal length;
        
        /**
         * 宽度（厘米）
         */
        private BigDecimal width;
        
        /**
         * 高度（厘米）
         */
        private BigDecimal height;
        
        /**
         * 体积（立方厘米）
         */
        private BigDecimal volume;
        
        /**
         * 规格描述文本
         */
        private String specsText;
        
        /**
         * 包装尺寸描述
         */
        private String packageSize;
    }

    /**
     * 营销标签
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarketingLabels {
        /**
         * 是否热门商品
         */
        private Boolean isHot;
        
        /**
         * 是否推荐商品
         */
        private Boolean isRecommended;
        
        /**
         * 是否新品
         */
        private Boolean isNew;
        
        /**
         * 是否限量商品
         */
        private Boolean isLimited;
        
        /**
         * 是否独家商品
         */
        private Boolean isExclusive;
        
        /**
         * 是否包邮商品
         */
        private Boolean isFreeShipping;
        
        /**
         * 是否秒杀商品
         */
        private Boolean isFlashSale;
        
        /**
         * 是否预售商品
         */
        private Boolean isPreSale;
        
        /**
         * 营销标签列表
         */
        private List<String> labelTexts;
        
        /**
         * 推荐理由
         */
        private String recommendReason;
    }

    /**
     * SEO信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeoInfo {
        /**
         * SEO标题
         */
        private String title;
        
        /**
         * SEO关键词
         */
        private String keywords;
        
        /**
         * SEO描述
         */
        private String description;
        
        /**
         * 规范化URL
         */
        private String canonicalUrl;
        
        /**
         * 商品URL
         */
        private String productUrl;
        
        /**
         * 分享链接
         */
        private String shareUrl;
        
        /**
         * Open Graph数据
         */
        private Map<String, String> openGraph;
        
        /**
         * Twitter Card数据
         */
        private Map<String, String> twitterCard;
        
        /**
         * 结构化数据 (JSON-LD)
         */
        private Map<String, Object> structuredData;
    }

    // ========== 业务方法 ==========

    /**
     * 是否可以购买
     * 
     * @return true-可购买，false-不可购买
     */
    public boolean canPurchase() {
        return "ACTIVE".equals(status) && 
               stockInfo.getHasStock() != null && 
               stockInfo.getHasStock();
    }

    /**
     * 是否显示会员价
     * 
     * @return true-显示会员价，false-不显示
     */
    public boolean showMemberPrice() {
        return priceInfo.getMemberPrice() != null && 
               priceInfo.getMemberPrice().compareTo(priceInfo.getCurrentPrice()) < 0;
    }

    /**
     * 获取主要卖点标签
     * 
     * @return 卖点标签列表
     */
    public List<String> getMainSellingPoints() {
        List<String> points = new java.util.ArrayList<>();
        
        if (marketingLabels.getIsHot() != null && marketingLabels.getIsHot()) {
            points.add("热门");
        }
        
        if (marketingLabels.getIsNew() != null && marketingLabels.getIsNew()) {
            points.add("新品");
        }
        
        if (priceInfo.getOnSale() != null && priceInfo.getOnSale()) {
            points.add("特价");
        }
        
        if (attributes.getFreeShipping() != null && attributes.getFreeShipping()) {
            points.add("包邮");
        }
        
        return points;
    }

    /**
     * 获取评价等级文本
     * 
     * @return 评价等级描述
     */
    public String getRatingLevelText() {
        if (salesInfo.getAverageRating() == null) {
            return "暂无评价";
        }
        
        BigDecimal rating = salesInfo.getAverageRating();
        if (rating.compareTo(new BigDecimal("4.5")) >= 0) {
            return "好评如潮";
        } else if (rating.compareTo(new BigDecimal("4.0")) >= 0) {
            return "好评较多";
        } else if (rating.compareTo(new BigDecimal("3.5")) >= 0) {
            return "评价一般";
        } else {
            return "评价较差";
        }
    }

    /**
     * 获取库存紧急程度
     * 
     * @return 紧急程度等级 (1-5，5最紧急)
     */
    public int getStockUrgencyLevel() {
        if (stockInfo.getAvailableStock() == null || stockInfo.getAvailableStock() <= 0) {
            return 5; // 缺货
        }
        
        if (stockInfo.getLowStockThreshold() == null) {
            return 1; // 库存充足
        }
        
        int available = stockInfo.getAvailableStock();
        int threshold = stockInfo.getLowStockThreshold();
        
        if (available <= threshold / 2) {
            return 4; // 库存极少
        } else if (available <= threshold) {
            return 3; // 库存紧张
        } else if (available <= threshold * 2) {
            return 2; // 库存一般  
        } else {
            return 1; // 库存充足
        }
    }

    /**
     * 获取商品详情页的页面标题
     * 
     * @return 页面标题
     */
    public String getPageTitle() {
        if (seoInfo.getTitle() != null && !seoInfo.getTitle().isEmpty()) {
            return seoInfo.getTitle();
        }
        
        StringBuilder title = new StringBuilder();
        title.append(name);
        
        if (brand != null && !brand.isEmpty()) {
            title.append(" - ").append(brand);
        }
        
        if (category.getName() != null) {
            title.append(" - ").append(category.getName());
        }
        
        return title.toString();
    }

    /**
     * 获取社交分享的描述文本
     * 
     * @return 分享描述
     */
    public String getShareDescription() {
        if (seoInfo.getDescription() != null && !seoInfo.getDescription().isEmpty()) {
            return seoInfo.getDescription();
        }
        
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append(name);
        
        if (priceInfo.getCurrentPrice() != null) {
            desc.append("，现价￥").append(priceInfo.getCurrentPrice());
        }
        
        if (priceInfo.getOnSale() != null && priceInfo.getOnSale()) {
            desc.append("，限时特价");
        }
        
        return desc.toString();
    }

    /**
     * 构建结构化数据用于SEO
     * 
     * @return JSON-LD格式的结构化数据
     */
    public Map<String, Object> buildStructuredData() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("@context", "https://schema.org/");
        data.put("@type", "Product");
        data.put("name", name);
        data.put("description", summary != null ? summary : name);
        data.put("image", images != null && !images.isEmpty() ? images : List.of(mainImage));
        data.put("brand", Map.of("@type", "Brand", "name", brand != null ? brand : ""));
        
        if (priceInfo.getCurrentPrice() != null) {
            Map<String, Object> offer = new java.util.HashMap<>();
            offer.put("@type", "Offer");
            offer.put("price", priceInfo.getCurrentPrice().toString());
            offer.put("priceCurrency", "CNY");
            offer.put("availability", stockInfo.getHasStock() ? "https://schema.org/InStock" : "https://schema.org/OutOfStock");
            data.put("offers", offer);
        }
        
        if (salesInfo.getAverageRating() != null && salesInfo.getReviewCount() > 0) {
            Map<String, Object> rating = new java.util.HashMap<>();
            rating.put("@type", "AggregateRating");
            rating.put("ratingValue", salesInfo.getAverageRating().toString());
            rating.put("reviewCount", salesInfo.getReviewCount().toString());
            rating.put("bestRating", "5");
            rating.put("worstRating", "1");
            data.put("aggregateRating", rating);
        }
        
        return data;
    }
}