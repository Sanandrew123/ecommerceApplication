package com.ecommerce.entity;

/*
 * 文件职责: 商品实体类，电商系统的核心业务实体
 * 
 * 开发心理活动：
 * 1. 商品模型设计考虑：
 *    - 基础信息：名称、描述、价格、库存
 *    - 分类关联：归属分类，支持多级分类
 *    - 状态管理：上架/下架/删除状态
 *    - 扩展属性：规格、参数、自定义字段
 * 
 * 2. 电商业务场景：
 *    - SKU管理：商品的具体规格组合
 *    - 价格策略：原价、现价、会员价
 *    - 库存管理：总库存、可用库存、预占库存
 *    - 销售统计：销量、评价、收藏
 * 
 * 3. 性能优化考虑：
 *    - 索引设计：分类、状态、价格、销量
 *    - 图片存储：多张商品图片的管理
 *    - 搜索优化：全文检索的字段设计
 *    - 缓存策略：热门商品的缓存处理
 * 
 * 4. 数据完整性：
 *    - 价格合理性：原价>=现价>0
 *    - 库存一致性：库存扣减的并发控制
 *    - 分类关联：分类删除时商品的处理
 *    - 状态流转：商品状态变更的业务规则
 * 
 * 包结构设计思路:
 * - 放在entity包下，作为核心业务实体
 * - 与Category、User等实体形成关联关系
 * 
 * 命名原因:
 * - Product明确表达商品功能
 * - 符合电商领域的通用命名
 * 
 * 依赖关系:
 * - 多对一关联Category：商品归属分类
 * - 多对一关联User：商品的创建者
 * - 一对多关联OrderItem：订单明细引用
 */

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品实体类
 * 
 * 功能说明：
 * 1. 商品基础信息管理
 * 2. 商品分类和状态管理
 * 3. 价格和库存管理
 * 4. 商品图片和描述管理
 * 
 * 业务特性：
 * 1. 多规格支持：通过SKU管理不同规格
 * 2. 价格策略：支持原价、现价、会员价
 * 3. 库存管理：实时库存、预占库存
 * 4. 状态控制：草稿、审核、上架、下架
 * 
 * 营销特性：
 * 1. 推荐标识：热门、推荐、新品标签
 * 2. 促销支持：限时折扣、满减活动
 * 3. 评价系统：评分、评价数量统计
 * 4. 收藏统计：用户收藏数量跟踪
 * 
 * 扩展特性：
 * 1. SEO优化：标题、关键词、描述
 * 2. 多媒体支持：图片、视频、3D展示
 * 3. 个性化：推荐权重、标签系统
 * 4. 国际化：多语言、多货币支持
 * 
 * 数据一致性：
 * 1. 价格约束：确保价格的合理性
 * 2. 库存同步：库存变更的原子性
 * 3. 状态流转：商品状态的业务规则
 * 4. 关联完整性：分类、用户的外键约束
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category_id", columnList = "category_id"),
    @Index(name = "idx_product_status", columnList = "status"),
    @Index(name = "idx_product_price", columnList = "current_price"),
    @Index(name = "idx_product_sales_count", columnList = "sales_count"),
    @Index(name = "idx_product_created_at", columnList = "created_at"),
    @Index(name = "idx_product_updated_at", columnList = "updated_at"),
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_code", columnList = "code")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    // ========== 基础字段 ==========

    /**
     * 商品ID - 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 商品名称
     * 
     * 业务规则：
     * - 必填字段，用户可见的商品标题
     * - 长度限制：1-200字符
     * - 支持中英文、数字、特殊符号
     * - 影响搜索引擎优化效果
     * 
     * 使用场景：
     * - 商品列表展示
     * - 搜索结果标题
     * - 订单明细显示
     * - SEO页面标题
     */
    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 200, message = "商品名称长度必须在1-200个字符之间")
    private String name;

    /**
     * 商品编码/SKU
     * 
     * 业务价值：
     * - 商品的唯一标识码
     * - 库存管理的基础标识
     * - 第三方系统集成的稳定ID
     * - 条形码、二维码的关联
     * 
     * 编码规则：
     * - 全局唯一，便于查询
     * - 支持字母、数字、短横线
     * - 建议使用有意义的编码规则
     * - 长度控制在50字符以内
     */
    @Column(name = "code", unique = true, length = 50)
    @Size(max = 50, message = "商品编码长度不能超过50个字符")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "商品编码只能包含字母、数字、短横线和下划线")
    private String code;

    /**
     * 商品副标题
     * 
     * 营销用途：
     * - 商品卖点的补充说明
     * - 促销信息的展示位置
     * - 规格参数的简要描述
     * - 搜索关键词的扩展
     */
    @Column(name = "subtitle", length = 300)
    @Size(max = 300, message = "商品副标题长度不能超过300个字符")
    private String subtitle;

    /**
     * 商品简述
     * 
     * 展示用途：
     * - 商品列表的简要描述
     * - 搜索结果的摘要信息
     * - 移动端的精简展示
     * - 社交分享的描述文本
     */
    @Column(name = "summary", length = 500)
    @Size(max = 500, message = "商品简述长度不能超过500个字符")
    private String summary;

    /**
     * 商品详细描述
     * 
     * 详情内容：
     * - 商品的详细介绍
     * - 支持HTML格式
     * - 包含商品特性、使用方法
     * - 可插入图片、视频等多媒体
     * 
     * 存储策略：
     * - 使用TEXT类型存储大量文本
     * - 支持富文本编辑器内容
     * - 考虑内容审核和过滤
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ========== 分类和关联字段 ==========

    /**
     * 商品分类ID
     * 
     * 关联说明：
     * - 外键关联categories表
     * - 商品必须归属某个分类
     * - 分类决定商品的展示位置
     * - 影响商品的搜索和筛选
     */
    @Column(name = "category_id", nullable = false)
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    /**
     * 品牌名称
     * 
     * 品牌管理：
     * - 商品的品牌标识
     * - 支持品牌筛选功能
     * - 品牌页面的商品聚合
     * - 品牌权威性的展示
     * 
     * 扩展考虑：
     * - 后续可扩展为品牌实体关联
     * - 支持品牌Logo、描述等信息
     */
    @Column(name = "brand", length = 100)
    @Size(max = 100, message = "品牌名称长度不能超过100个字符")
    private String brand;

    /**
     * 商品标签
     * 
     * 标签系统：
     * - 多个标签用逗号分隔
     * - 支持商品的多维度分类
     * - 提升搜索的匹配度
     * - 推荐算法的特征提取
     * 
     * 标签示例：
     * - "热销,新品,限时折扣"
     * - "有机,健康,进口"
     */
    @Column(name = "tags", length = 500)
    @Size(max = 500, message = "商品标签长度不能超过500个字符")
    private String tags;

    // ========== 价格字段 ==========

    /**
     * 商品原价
     * 
     * 价格策略：
     * - 商品的市场指导价
     * - 折扣计算的基准价格
     * - 价格对比的参考依据
     * - 营销活动的原价展示
     * 
     * 精度控制：
     * - 使用BigDecimal确保精度
     * - 支持小数点后2位
     * - 价格不能为负数
     */
    @Column(name = "original_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "商品原价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "商品原价格式不正确")
    private BigDecimal originalPrice;

    /**
     * 商品现价
     * 
     * 销售价格：
     * - 商品的实际销售价格
     * - 用户下单时的计价基础
     * - 必须小于等于原价
     * - 影响商品的价格排序
     * 
     * 业务约束：
     * - 现价必须大于0
     * - 现价不能超过原价
     * - 价格变更需要记录历史
     */
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "商品现价不能为空")
    @DecimalMin(value = "0.01", message = "商品现价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "商品现价格式不正确")
    private BigDecimal currentPrice;

    /**
     * 会员价格
     * 
     * 会员权益：
     * - VIP用户的专享价格
     * - 会员等级的差异化定价
     * - 会员营销的重要工具
     * - 用户升级会员的动力
     * 
     * 定价规则：
     * - 会员价<=现价<=原价
     * - 可以为空，表示无会员价
     * - 支持会员等级的阶梯定价
     */
    @Column(name = "member_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "会员价格不能小于0")
    @Digits(integer = 8, fraction = 2, message = "会员价格格式不正确")
    private BigDecimal memberPrice;

    /**
     * 成本价格
     * 
     * 成本管理：
     * - 商品的采购或生产成本
     * - 利润计算的基础数据
     * - 定价策略的参考依据
     * - 财务分析的重要指标
     * 
     * 保密性：
     * - 仅内部使用，不对外展示
     * - 影响毛利率的计算
     * - 库存成本的核算基础
     */
    @Column(name = "cost_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "成本价格不能小于0")
    @Digits(integer = 8, fraction = 2, message = "成本价格格式不正确")
    private BigDecimal costPrice;

    // ========== 库存字段 ==========

    /**
     * 总库存数量
     * 
     * 库存管理：
     * - 商品的总库存数量
     * - 库存预警的判断依据
     * - 订单库存检查的基础
     * - 库存报表的统计来源
     * 
     * 业务规则：
     * - 库存不能为负数
     * - 库存扣减需要原子操作
     * - 支持库存的批量调整
     */
    @Column(name = "total_stock", nullable = false)
    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能小于0")
    @Builder.Default
    private Integer totalStock = 0;

    /**
     * 可用库存数量
     * 
     * 可用库存：
     * - 扣除预占后的可销售库存
     * - 前端展示的库存状态
     * - 下单时的库存检查依据
     * - 通常 可用库存 = 总库存 - 预占库存
     * 
     * 实时性：
     * - 订单支付时实时扣减
     * - 订单取消时实时释放
     * - 库存同步的关键字段
     */
    @Column(name = "available_stock", nullable = false)
    @Min(value = 0, message = "可用库存不能小于0")
    @Builder.Default
    private Integer availableStock = 0;

    /**
     * 预占库存数量
     * 
     * 预占机制：
     * - 用户下单未支付时的库存预占
     * - 防止超卖的重要机制
     * - 订单超时后自动释放
     * - 预占时长可配置（如30分钟）
     * 
     * 计算公式：
     * - 预占库存 = 未支付订单的商品数量总和
     * - 总库存 = 可用库存 + 预占库存 + 已售库存
     */
    @Column(name = "reserved_stock", nullable = false)
    @Min(value = 0, message = "预占库存不能小于0")
    @Builder.Default
    private Integer reservedStock = 0;

    /**
     * 库存预警值
     * 
     * 预警机制：
     * - 库存低于此值时触发预警
     * - 自动采购的触发条件
     * - 库存管理的重要工具
     * - 缺货风险的提前预防
     * 
     * 预警策略：
     * - 可以基于历史销量设置
     * - 支持不同商品的差异化设置
     * - 预警通知的及时性很重要
     */
    @Column(name = "low_stock_threshold")
    @Min(value = 0, message = "库存预警值不能小于0")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    // ========== 状态和配置字段 ==========

    /**
     * 商品状态
     */
    public enum ProductStatus {
        DRAFT("草稿", "商品信息编辑中，未发布"),
        PENDING("待审核", "商品等待管理员审核"),
        ACTIVE("上架", "商品正常销售中"),
        INACTIVE("下架", "商品暂停销售"),
        OUT_OF_STOCK("缺货", "商品库存不足"),
        DISCONTINUED("停产", "商品停止生产销售"),
        DELETED("已删除", "商品已删除，软删除状态");

        private final String displayName;
        private final String description;

        ProductStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 商品状态
     * 
     * 状态流转：
     * 草稿 -> 待审核 -> 上架/下架
     * 上架 -> 下架/缺货/停产
     * 任何状态 -> 删除
     * 
     * 业务逻辑：
     * - 只有上架状态的商品可以销售
     * - 缺货状态自动触发（库存为0时）
     * - 删除状态为软删除，保留历史数据
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * 是否为虚拟商品
     * 
     * 虚拟商品特性：
     * - 无需物流配送的商品
     * - 如：电子书、软件、会员服务
     * - 支付后立即发货（自动发货）
     * - 不计算重量和体积
     */
    @Column(name = "is_virtual", nullable = false)
    @Builder.Default
    private Boolean isVirtual = false;

    /**
     * 是否需要配送
     * 
     * 配送策略：
     * - false：虚拟商品或自提商品
     * - true：需要物流配送的实体商品
     * - 影响配送费用的计算
     * - 影响订单的履约流程
     */
    @Column(name = "requires_shipping", nullable = false)
    @Builder.Default
    private Boolean requiresShipping = true;

    /**
     * 是否可单独购买
     * 
     * 销售策略：
     * - true：可以单独加入购物车
     * - false：必须与其他商品搭配购买
     * - 用于套餐商品的销售策略
     * - 影响购物车的验证逻辑
     */
    @Column(name = "can_buy_alone", nullable = false)
    @Builder.Default
    private Boolean canBuyAlone = true;

    // ========== 营销和推广字段 ==========

    /**
     * 是否为热门商品
     * 
     * 热门标识：
     * - 基于销量、访问量等指标判断
     * - 首页推荐位的展示标识
     * - 搜索结果的优先排序因子
     * - 可以人工设置或算法自动判断
     */
    @Column(name = "is_hot", nullable = false)
    @Builder.Default
    private Boolean isHot = false;

    /**
     * 是否为推荐商品
     * 
     * 推荐机制：
     * - 编辑推荐或算法推荐
     * - 个性化推荐的候选商品
     * - 相关商品推荐的数据源
     * - 营销活动的重点商品
     */
    @Column(name = "is_recommended", nullable = false)
    @Builder.Default
    private Boolean isRecommended = false;

    /**
     * 是否为新品
     * 
     * 新品策略：
     * - 上架时间在N天内的商品
     * - 新品促销活动的标识
     * - 可以基于创建时间自动判断
     * - 影响商品的展示位置
     */
    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    /**
     * 推荐权重
     * 
     * 权重算法：
     * - 推荐系统的排序权重
     * - 数值越高推荐优先级越高
     * - 可以基于销量、评分等计算
     * - 支持人工调整权重值
     */
    @Column(name = "recommend_weight")
    @Min(value = 0, message = "推荐权重不能小于0")
    @Max(value = 100, message = "推荐权重不能大于100")
    @Builder.Default
    private Integer recommendWeight = 50;

    // ========== 物理属性字段 ==========

    /**
     * 商品重量（克）
     * 
     * 物流计算：
     * - 配送费用的计算依据
     * - 包装方案的选择参考
     * - 物流方式的选择依据
     * - 国际配送的重要参数
     */
    @Column(name = "weight")
    @DecimalMin(value = "0.0", message = "商品重量不能小于0")
    @Digits(integer = 8, fraction = 2, message = "商品重量格式不正确")
    private BigDecimal weight;

    /**
     * 商品长度（厘米）
     */
    @Column(name = "length")
    @DecimalMin(value = "0.0", message = "商品长度不能小于0")
    @Digits(integer = 8, fraction = 2, message = "商品长度格式不正确")
    private BigDecimal length;

    /**
     * 商品宽度（厘米）
     */
    @Column(name = "width")
    @DecimalMin(value = "0.0", message = "商品宽度不能小于0")
    @Digits(integer = 8, fraction = 2, message = "商品宽度格式不正确")
    private BigDecimal width;

    /**
     * 商品高度（厘米）
     */
    @Column(name = "height")
    @DecimalMin(value = "0.0", message = "商品高度不能小于0")
    @Digits(integer = 8, fraction = 2, message = "商品高度格式不正确")
    private BigDecimal height;

    // ========== 多媒体字段 ==========

    /**
     * 商品主图
     * 
     * 主图用途：
     * - 商品列表的缩略图展示
     * - 搜索结果的图片展示
     * - 购物车商品的图片标识
     * - 社交分享的预览图
     * 
     * 存储策略：
     * - 存储图片的完整URL地址
     * - 支持CDN加速的图片服务
     * - 建议尺寸：800x800像素
     */
    @Column(name = "main_image", length = 500)
    @Size(max = 500, message = "商品主图URL长度不能超过500个字符")
    private String mainImage;

    /**
     * 商品图片列表
     * 
     * 图片管理：
     * - 多张商品展示图片
     * - JSON数组格式存储URL列表
     * - 支持图片的排序和管理
     * - 商品详情页的轮播展示
     * 
     * 存储格式：
     * ["url1", "url2", "url3"]
     * 建议数量：3-8张图片
     */
    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    /**
     * 商品视频
     * 
     * 视频展示：
     * - 商品的动态展示视频
     * - 使用说明或效果演示
     * - 提升用户的购买转化率
     * - 支持多种视频格式
     * 
     * 存储方式：
     * - 视频URL地址
     * - 支持第三方视频平台
     * - 考虑视频的加载性能
     */
    @Column(name = "video_url", length = 500)
    @Size(max = 500, message = "商品视频URL长度不能超过500个字符")
    private String videoUrl;

    // ========== 统计字段 ==========

    /**
     * 销售数量
     * 
     * 销量统计：
     * - 商品的累计销售数量
     * - 热门商品判断的重要指标
     * - 推荐算法的权重因子
     * - 用户购买决策的参考
     * 
     * 更新时机：
     * - 订单支付成功后增加
     * - 订单退款后减少
     * - 定期校验数据一致性
     */
    @Column(name = "sales_count", nullable = false)
    @Min(value = 0, message = "销售数量不能小于0")
    @Builder.Default
    private Long salesCount = 0L;

    /**
     * 浏览次数
     * 
     * 访问统计：
     * - 商品详情页的访问次数
     * - 用户兴趣度的量化指标
     * - 推荐算法的特征数据
     * - 商品热度的判断依据
     * 
     * 统计策略：
     * - 去重统计：同一用户24小时内多次访问只计一次
     * - 实时更新或异步更新
     * - 考虑爬虫和恶意访问的过滤
     */
    @Column(name = "view_count", nullable = false)
    @Min(value = 0, message = "浏览次数不能小于0")
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * 收藏次数
     * 
     * 收藏统计：
     * - 用户收藏商品的次数统计
     * - 用户喜好度的重要指标
     * - 商品受欢迎程度的体现
     * - 个性化推荐的重要特征
     * 
     * 业务价值：
     * - 收藏转化率的分析
     * - 用户兴趣偏好的挖掘
     * - 商品优化的决策依据
     */
    @Column(name = "favorite_count", nullable = false)
    @Min(value = 0, message = "收藏次数不能小于0")
    @Builder.Default
    private Long favoriteCount = 0L;

    /**
     * 评价数量
     * 
     * 评价统计：
     * - 用户评价的总数量
     * - 商品信誉度的重要指标
     * - 影响其他用户的购买决策
     * - 搜索排序的权重因子
     */
    @Column(name = "review_count", nullable = false)
    @Min(value = 0, message = "评价数量不能小于0")
    @Builder.Default
    private Long reviewCount = 0L;

    /**
     * 平均评分
     * 
     * 评分系统：
     * - 用户评价的平均分数
     * - 通常采用5分制或10分制
     * - 商品质量的量化指标
     * - 搜索和推荐的重要因子
     * 
     * 计算方式：
     * - 所有评价分数的算术平均值
     * - 可以考虑评价时间的权重衰减
     * - 过滤恶意评价的影响
     */
    @Column(name = "average_rating", precision = 3, scale = 2)
    @DecimalMin(value = "0.00", message = "平均评分不能小于0")
    @DecimalMax(value = "5.00", message = "平均评分不能大于5")
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    // ========== SEO字段 ==========

    /**
     * SEO标题
     * 
     * 搜索优化：
     * - 页面title标签的内容
     * - 搜索引擎结果的标题显示
     * - 影响搜索排名的重要因素
     * - 社交分享时的标题内容
     */
    @Column(name = "seo_title", length = 200)
    @Size(max = 200, message = "SEO标题长度不能超过200个字符")
    private String seoTitle;

    /**
     * SEO关键词
     * 
     * 关键词策略：
     * - 页面meta keywords标签
     * - 内部搜索的匹配关键词
     * - 长尾关键词的覆盖
     * - 多个关键词用逗号分隔
     */
    @Column(name = "seo_keywords", length = 500)
    @Size(max = 500, message = "SEO关键词长度不能超过500个字符")
    private String seoKeywords;

    /**
     * SEO描述
     * 
     * 描述优化：
     * - 页面meta description标签
     * - 搜索结果的描述摘要
     * - 影响点击率的关键因素
     * - 社交分享的描述内容
     */
    @Column(name = "seo_description", length = 500)
    @Size(max = 500, message = "SEO描述长度不能超过500个字符")
    private String seoDescription;

    // ========== 扩展字段 ==========

    /**
     * 商品规格参数JSON
     * 
     * 规格管理：
     * - 商品的详细规格参数
     * - JSON格式存储键值对
     * - 支持动态的规格定义
     * - 规格对比功能的数据源
     * 
     * 存储格式：
     * {
     *   "颜色": "红色",
     *   "尺寸": "XL",
     *   "材质": "纯棉"
     * }
     */
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    /**
     * 商品参数JSON
     * 
     * 参数扩展：
     * - 商品的技术参数
     * - 使用场景、适用人群等
     * - 营销卖点的结构化存储
     * - 搜索筛选的数据基础
     */
    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    /**
     * 扩展信息JSON
     * 
     * 灵活扩展：
     * - 未来功能的预留字段
     * - 第三方集成的数据存储
     * - A/B测试的配置参数
     * - 动态配置的存储空间
     */
    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;

    // ========== 审计字段 ==========

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 更新者ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 上架时间
     * 
     * 时间记录：
     * - 商品首次上架的时间
     * - 新品判断的时间依据
     * - 商品生命周期的起始点
     * - 销售统计的时间基准
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 下架时间
     * 
     * 下架记录：
     * - 商品下架的时间记录
     * - 销售周期的结束时间
     * - 库存清理的时间参考
     * - 商品生命周期的终止点
     */
    @Column(name = "unpublished_at")
    private LocalDateTime unpublishedAt;

    // ========== 关联关系 ==========

    /**
     * 商品分类
     * 
     * 关联关系：
     * - 多对一关系，多个商品属于一个分类
     * - 懒加载策略，按需加载分类信息
     * - 用于分类页面的商品展示
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    /**
     * 创建者信息
     * 
     * 关联关系：
     * - 多对一关系，多个商品可能由同一人创建
     * - 懒加载策略，按需加载用户信息
     * - 用于商品管理的权限控制
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    // ========== 业务方法 ==========

    /**
     * 是否为上架状态
     * 
     * @return true-上架，false-非上架
     */
    public boolean isActive() {
        return ProductStatus.ACTIVE.equals(status);
    }

    /**
     * 是否已删除
     * 
     * @return true-已删除，false-未删除
     */
    public boolean isDeleted() {
        return ProductStatus.DELETED.equals(status);
    }

    /**
     * 是否有库存
     * 
     * @return true-有库存，false-无库存
     */
    public boolean hasStock() {
        return availableStock > 0;
    }

    /**
     * 是否库存充足
     * 
     * @param quantity 需要的数量
     * @return true-库存充足，false-库存不足
     */
    public boolean hasEnoughStock(Integer quantity) {
        return availableStock >= quantity;
    }

    /**
     * 是否需要库存预警
     * 
     * @return true-需要预警，false-不需要预警
     */
    public boolean needsLowStockAlert() {
        return lowStockThreshold != null && availableStock <= lowStockThreshold;
    }

    /**
     * 获取折扣比例
     * 
     * @return 折扣比例（0-1之间）
     */
    public BigDecimal getDiscountRate() {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = originalPrice.subtract(currentPrice);
        return discount.divide(originalPrice, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取节省金额
     * 
     * @return 节省的金额
     */
    public BigDecimal getSavedAmount() {
        if (originalPrice == null) {
            return BigDecimal.ZERO;
        }
        
        return originalPrice.subtract(currentPrice);
    }

    /**
     * 是否为促销商品
     * 
     * @return true-促销商品，false-非促销商品
     */
    public boolean isOnSale() {
        return originalPrice != null && 
               originalPrice.compareTo(currentPrice) > 0;
    }

    /**
     * 获取库存状态描述
     * 
     * @return 库存状态文本
     */
    public String getStockStatusText() {
        if (availableStock <= 0) {
            return "缺货";
        } else if (needsLowStockAlert()) {
            return "库存紧张";
        } else {
            return "现货";
        }
    }

    /**
     * 扣减库存
     * 
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    public boolean reduceStock(Integer quantity) {
        if (!hasEnoughStock(quantity)) {
            return false;
        }
        
        this.availableStock -= quantity;
        this.totalStock -= quantity;
        
        // 如果库存为0，自动设置为缺货状态
        if (this.availableStock == 0 && isActive()) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
        
        return true;
    }

    /**
     * 增加库存
     * 
     * @param quantity 增加数量
     */
    public void addStock(Integer quantity) {
        this.availableStock += quantity;
        this.totalStock += quantity;
        
        // 如果之前是缺货状态且现在有库存，恢复为上架状态
        if (this.availableStock > 0 && ProductStatus.OUT_OF_STOCK.equals(status)) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    /**
     * 预占库存
     * 
     * @param quantity 预占数量
     * @return 是否预占成功
     */
    public boolean reserveStock(Integer quantity) {
        if (!hasEnoughStock(quantity)) {
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
    public void releaseReservedStock(Integer quantity) {
        int releaseAmount = Math.min(quantity, this.reservedStock);
        this.reservedStock -= releaseAmount;
        this.availableStock += releaseAmount;
    }

    /**
     * 确认预占库存（转为销售）
     * 
     * @param quantity 确认数量
     */
    public void confirmReservedStock(Integer quantity) {
        int confirmAmount = Math.min(quantity, this.reservedStock);
        this.reservedStock -= confirmAmount;
        this.totalStock -= confirmAmount;
        this.salesCount += confirmAmount;
    }

    /**
     * 增加销量
     * 
     * @param quantity 销售数量
     */
    public void addSalesCount(Long quantity) {
        this.salesCount += quantity;
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
     * 更新评价统计
     * 
     * @param newRating 新评价分数
     */
    public void updateRating(BigDecimal newRating) {
        // 计算新的平均评分
        BigDecimal totalRating = averageRating.multiply(new BigDecimal(reviewCount));
        totalRating = totalRating.add(newRating);
        this.reviewCount++;
        this.averageRating = totalRating.divide(new BigDecimal(reviewCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 商品上架
     */
    public void publish() {
        this.status = ProductStatus.ACTIVE;
        this.publishedAt = LocalDateTime.now();
        this.unpublishedAt = null;
    }

    /**
     * 商品下架
     */
    public void unpublish() {
        this.status = ProductStatus.INACTIVE;
        this.unpublishedAt = LocalDateTime.now();
    }

    /**
     * 软删除商品
     */
    public void softDelete() {
        this.status = ProductStatus.DELETED;
        this.unpublishedAt = LocalDateTime.now();
    }

    /**
     * 恢复商品
     */
    public void restore() {
        this.status = ProductStatus.ACTIVE;
        this.publishedAt = LocalDateTime.now();
        this.unpublishedAt = null;
    }

    // ========== 重写方法 ==========

    /**
     * 重写equals方法
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Product product = (Product) obj;
        return id != null && id.equals(product.id);
    }

    /**
     * 重写hashCode方法
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 重写toString方法
     */
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", currentPrice=" + currentPrice +
                ", availableStock=" + availableStock +
                ", status=" + status +
                ", salesCount=" + salesCount +
                '}';
    }
}