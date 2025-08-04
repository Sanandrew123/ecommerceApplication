package com.ecommerce.repository;

/*
 * 文件职责: 商品数据访问层，提供商品相关的数据库操作方法
 * 
 * 开发心理活动：
 * 1. Repository设计原则：
 *    - 数据访问抽象：封装具体的数据库操作细节
 *    - 查询优化：合理使用索引和查询策略
 *    - 方法命名：遵循Spring Data JPA命名规范
 *    - 性能考虑：分页查询、批量操作、缓存策略
 * 
 * 2. 电商商品查询场景：
 *    - 商品列表：分页、排序、筛选
 *    - 商品搜索：关键词、分类、价格区间
 *    - 推荐算法：热门商品、相关商品
 *    - 库存管理：低库存预警、库存统计
 * 
 * 3. 查询性能优化：
 *    - 索引利用：基于高频查询字段创建索引
 *    - 分页查询：避免大数据量的内存占用
 *    - 联表查询：合理使用JOIN减少N+1问题
 *    - 缓存策略：热点数据的缓存处理
 * 
 * 4. 数据一致性考虑：
 *    - 库存更新：并发控制和事务处理
 *    - 状态变更：商品状态的业务规则
 *    - 关联数据：分类、用户等关联关系
 *    - 软删除：历史数据的保留策略
 * 
 * 包结构设计思路:
 * - 放在repository包下，作为数据访问层
 * - 继承JpaRepository，获得基础CRUD能力
 * 
 * 命名原因:
 * - ProductRepository明确表达商品数据访问功能
 * - 符合Repository后缀的命名规范
 * 
 * 依赖关系:
 * - 被Service层调用，提供数据访问服务
 * - 操作Product实体，执行数据库操作
 * - 与数据库表products直接交互
 */

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 商品数据访问层
 * 
 * 功能说明：
 * 1. 提供商品的基础CRUD操作
 * 2. 支持复杂的商品查询和筛选
 * 3. 提供商品统计和分析方法
 * 4. 优化查询性能和数据访问效率
 * 
 * 查询场景：
 * 1. 商品展示：按分类、状态、价格等条件查询
 * 2. 商品搜索：关键词搜索、高级筛选
 * 3. 推荐系统：热门商品、相关商品推荐
 * 4. 库存管理：库存预警、批量库存操作
 * 
 * 性能特性：
 * 1. 索引优化：基于查询频率设计索引
 * 2. 分页查询：支持大数据量的分页展示
 * 3. 批量操作：提高数据操作效率
 * 4. 缓存支持：配合二级缓存提升性能
 * 
 * 数据安全：
 * 1. 参数化查询：防止SQL注入攻击
 * 2. 权限控制：结合业务权限验证
 * 3. 软删除：保持数据完整性
 * 4. 审计日志：重要操作的日志记录
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== 基础查询方法 ==========

    /**
     * 根据商品编码查询商品
     * 
     * 业务场景：
     * - 通过SKU快速查找商品
     * - 批量导入时的商品验证
     * - 第三方系统的商品同步
     * - 客服系统的商品查询
     * 
     * 性能优化：
     * - code字段已建立唯一索引
     * - 查询结果唯一，性能较高
     * 
     * @param code 商品编码
     * @return 商品信息
     */
    Optional<Product> findByCode(String code);

    /**
     * 根据商品名称模糊查询
     * 
     * 应用场景：
     * - 商品名称的模糊搜索
     * - 管理后台的商品查找
     * - 重复商品名称的检查
     * 
     * 查询优化：
     * - 使用LIKE查询，注意性能影响
     * - 建议结合其他条件限制结果集
     * 
     * @param name 商品名称关键词
     * @return 匹配的商品列表
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * 根据商品状态查询
     * 
     * 使用场景：
     * - 获取所有上架商品
     * - 查询待审核商品
     * - 统计各状态商品数量
     * 
     * 索引利用：
     * - status字段已建立索引
     * - 支持高效的状态筛选
     * 
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 指定状态的商品分页列表
     */
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

    /**
     * 查询多个状态的商品
     * 
     * 业务需求：
     * - 同时查询上架和缺货商品
     * - 排除已删除商品的查询
     * - 状态组合的灵活查询
     * 
     * @param statuses 商品状态列表
     * @param pageable 分页参数
     * @return 指定状态的商品分页列表
     */
    Page<Product> findByStatusIn(List<Product.ProductStatus> statuses, Pageable pageable);

    // ========== 分类相关查询 ==========

    /**
     * 根据分类ID查询商品
     * 
     * 核心场景：
     * - 分类页面的商品展示
     * - 分类导航的商品加载
     * - 分类商品数量统计
     * 
     * 查询优化：
     * - category_id字段已建立索引
     * - 支持分页避免大数据量
     * 
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 该分类下的商品分页列表
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 根据分类ID和状态查询商品
     * 
     * 精确查询：
     * - 分类页面只显示上架商品
     * - 管理后台按分类和状态筛选
     * - 分类商品的状态管理
     * 
     * @param categoryId 分类ID
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 符合条件的商品分页列表
     */
    Page<Product> findByCategoryIdAndStatus(Long categoryId, Product.ProductStatus status, Pageable pageable);

    /**
     * 根据多个分类ID查询商品
     * 
     * 应用场景：
     * - 多分类商品的聚合展示
     * - 相关分类商品的推荐
     * - 分类组合的商品筛选
     * 
     * @param categoryIds 分类ID列表
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 多分类下的商品分页列表
     */
    Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, Product.ProductStatus status, Pageable pageable);

    /**
     * 统计分类下的商品数量
     * 
     * 统计用途：
     * - 分类导航显示商品数量
     * - 分类管理的数据统计
     * - 分类热度的量化指标
     * 
     * @param categoryId 分类ID
     * @param status 商品状态
     * @return 商品数量
     */
    long countByCategoryIdAndStatus(Long categoryId, Product.ProductStatus status);

    // ========== 价格区间查询 ==========

    /**
     * 根据价格区间查询商品
     * 
     * 价格筛选：
     * - 商品列表的价格筛选功能
     * - 价格区间的商品推荐
     * - 用户预算内的商品查找
     * 
     * 索引优化：
     * - current_price字段已建立索引
     * - 范围查询性能较好
     * 
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 价格区间内的商品分页列表
     */
    Page<Product> findByCurrentPriceBetweenAndStatus(
            BigDecimal minPrice, BigDecimal maxPrice, 
            Product.ProductStatus status, Pageable pageable);

    /**
     * 查询指定价格以下的商品
     * 
     * 应用场景：
     * - "XX元以下商品"的促销活动
     * - 价格敏感用户的商品推荐
     * - 预算限制的商品筛选
     * 
     * @param maxPrice 最高价格
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 价格以下的商品分页列表
     */
    Page<Product> findByCurrentPriceLessThanEqualAndStatus(
            BigDecimal maxPrice, Product.ProductStatus status, Pageable pageable);

    /**
     * 查询指定价格以上的商品
     * 
     * 使用场景：
     * - 高端商品的专区展示
     * - 奢侈品类商品筛选
     * - 价格排序的高价商品
     * 
     * @param minPrice 最低价格
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 价格以上的商品分页列表
     */
    Page<Product> findByCurrentPriceGreaterThanEqualAndStatus(
            BigDecimal minPrice, Product.ProductStatus status, Pageable pageable);

    // ========== 品牌相关查询 ==========

    /**
     * 根据品牌查询商品
     * 
     * 品牌筛选：
     * - 品牌页面的商品展示
     * - 品牌筛选功能
     * - 品牌商品统计分析
     * 
     * @param brand 品牌名称
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 该品牌的商品分页列表
     */
    Page<Product> findByBrandAndStatus(String brand, Product.ProductStatus status, Pageable pageable);

    /**
     * 根据多个品牌查询商品
     * 
     * 多品牌筛选：
     * - 用户选择多个品牌的商品筛选
     * - 品牌对比的商品展示
     * - 合作品牌的联合推广
     * 
     * @param brands 品牌名称列表
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 多品牌的商品分页列表
     */
    Page<Product> findByBrandInAndStatus(List<String> brands, Product.ProductStatus status, Pageable pageable);

    /**
     * 获取所有品牌列表
     * 
     * 数据源：
     * - 商品筛选的品牌选项
     * - 品牌导航的数据来源
     * - 品牌统计分析的基础数据
     * 
     * @return 所有品牌名称的去重列表
     */
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.status = 'ACTIVE' ORDER BY p.brand")
    List<String> findAllDistinctBrands();

    // ========== 库存相关查询 ==========

    /**
     * 查询库存充足的商品
     * 
     * 库存管理：
     * - 有库存商品的优先展示
     * - 库存充足的商品推荐
     * - 避免显示缺货商品
     * 
     * @param minStock 最小库存数量
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 库存充足的商品分页列表
     */
    Page<Product> findByAvailableStockGreaterThanAndStatus(
            Integer minStock, Product.ProductStatus status, Pageable pageable);

    /**
     * 查询低库存商品
     * 
     * 预警功能：
     * - 库存预警的商品列表
     * - 需要补货的商品统计
     * - 库存管理的重点关注商品
     * 
     * @param pageable 分页参数
     * @return 低库存商品分页列表
     */
    @Query("SELECT p FROM Product p WHERE p.availableStock <= p.lowStockThreshold AND p.status = 'ACTIVE'")
    Page<Product> findLowStockProducts(Pageable pageable);

    /**
     * 查询缺货商品
     * 
     * 缺货管理：
     * - 缺货商品的统计和管理
     * - 补货计划的制定依据
     * - 用户关注的缺货商品通知
     * 
     * @param pageable 分页参数
     * @return 缺货商品分页列表
     */
    Page<Product> findByAvailableStockAndStatus(Integer availableStock, Product.ProductStatus status, Pageable pageable);

    /**
     * 批量更新商品库存
     * 
     * 批量操作：
     * - 库存调整的批量处理
     * - 导入库存数据的批量更新
     * - 定期库存盘点的数据更新
     * 
     * @param productId 商品ID
     * @param newStock 新库存数量
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET p.availableStock = :newStock, p.totalStock = :newStock WHERE p.id = :productId")
    int updateProductStock(@Param("productId") Long productId, @Param("newStock") Integer newStock);

    // ========== 销售统计查询 ==========

    /**
     * 查询热门商品
     * 
     * 热门定义：
     * - 基于销量排序的热门商品
     * - 推荐系统的热门商品数据源
     * - 首页热门商品的展示
     * 
     * 排序策略：
     * - 按销量降序排列
     * - 可结合时间权重优化
     * 
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 热门商品分页列表
     */
    Page<Product> findByStatusOrderBySalesCountDesc(Product.ProductStatus status, Pageable pageable);

    /**
     * 查询最新商品
     * 
     * 新品展示：
     * - 新品推荐的数据来源
     * - 按上架时间排序的最新商品
     * - 新品促销活动的商品筛选
     * 
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 最新商品分页列表
     */
    Page<Product> findByStatusOrderByPublishedAtDesc(Product.ProductStatus status, Pageable pageable);

    /**
     * 查询高评分商品
     * 
     * 口碑商品：
     * - 基于用户评价的高分商品
     * - 品质保证的商品推荐
     * - 用户满意度高的商品展示
     * 
     * @param minRating 最低评分
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 高评分商品分页列表
     */
    Page<Product> findByAverageRatingGreaterThanEqualAndStatus(
            BigDecimal minRating, Product.ProductStatus status, Pageable pageable);

    /**
     * 根据销量区间查询商品
     * 
     * 销量筛选：
     * - 销量区间的商品筛选
     * - 基于销量的商品分级
     * - 销售数据的统计分析
     * 
     * @param minSales 最小销量
     * @param maxSales 最大销量
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 销量区间内的商品分页列表
     */
    Page<Product> findBySalesCountBetweenAndStatus(
            Long minSales, Long maxSales, Product.ProductStatus status, Pageable pageable);

    // ========== 时间范围查询 ==========

    /**
     * 查询指定时间范围内创建的商品
     * 
     * 时间筛选：
     * - 新品上架的时间统计
     * - 指定时期的商品分析
     * - 商品上架趋势的数据支撑
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 时间范围内的商品分页列表
     */
    Page<Product> findByCreatedAtBetweenAndStatus(
            LocalDateTime startTime, LocalDateTime endTime, 
            Product.ProductStatus status, Pageable pageable);

    /**
     * 查询指定时间范围内上架的商品
     * 
     * 上架分析：
     * - 商品上架时间的统计分析
     * - 上架活动的效果评估
     * - 季节性商品的上架规律
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 上架时间范围内的商品分页列表
     */
    Page<Product> findByPublishedAtBetweenAndStatus(
            LocalDateTime startTime, LocalDateTime endTime, 
            Product.ProductStatus status, Pageable pageable);

    /**
     * 查询最近N天内更新的商品
     * 
     * 更新跟踪：
     * - 商品信息的更新监控
     * - 价格调整的商品统计
     * - 库存变动的商品跟踪
     * 
     * @param days 天数
     * @return 最近更新的商品列表
     */
    @Query("SELECT p FROM Product p WHERE p.updatedAt >= :cutoffTime ORDER BY p.updatedAt DESC")
    List<Product> findRecentlyUpdatedProducts(@Param("cutoffTime") LocalDateTime cutoffTime);

    // ========== 复杂搜索查询 ==========

    /**
     * 多条件商品搜索
     * 
     * 高级搜索：
     * - 商品名称、描述的关键词搜索
     * - 结合分类、品牌、价格的综合筛选
     * - 搜索结果的相关性排序
     * 
     * 搜索优化：
     * - 使用全文检索提升搜索效果
     * - 分词匹配提高搜索准确性
     * - 搜索结果的权重排序
     * 
     * @param keyword 搜索关键词
     * @param categoryId 分类ID（可选）
     * @param brand 品牌（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 搜索结果分页列表
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND (:minPrice IS NULL OR p.currentPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.currentPrice <= :maxPrice) " +
           "AND p.status = :status " +
           "ORDER BY p.salesCount DESC, p.averageRating DESC")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") Product.ProductStatus status,
            Pageable pageable);

    /**
     * 相关商品推荐查询
     * 
     * 推荐策略：
     * - 同分类的相关商品推荐
     * - 同品牌的商品推荐
     * - 相似价格区间的商品推荐
     * - 排除当前商品本身
     * 
     * 推荐算法：
     * - 基于分类的协同过滤
     * - 基于品牌的相似性推荐
     * - 基于价格的区间推荐
     * 
     * @param currentProductId 当前商品ID
     * @param categoryId 商品分类ID
     * @param brand 商品品牌
     * @param currentPrice 商品价格
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 相关商品推荐列表
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.id != :currentProductId " +
           "AND p.status = :status " +
           "AND (p.categoryId = :categoryId " +
           "OR p.brand = :brand " +
           "OR (p.currentPrice BETWEEN :minPrice AND :maxPrice)) " +
           "ORDER BY " +
           "CASE WHEN p.categoryId = :categoryId THEN 3 ELSE 0 END + " +
           "CASE WHEN p.brand = :brand THEN 2 ELSE 0 END + " +
           "CASE WHEN p.currentPrice BETWEEN :minPrice AND :maxPrice THEN 1 ELSE 0 END DESC, " +
           "p.salesCount DESC, p.averageRating DESC")
    Page<Product> findRelatedProducts(
            @Param("currentProductId") Long currentProductId,
            @Param("categoryId") Long categoryId,
            @Param("brand") String brand,
            @Param("currentPrice") BigDecimal currentPrice,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") Product.ProductStatus status,
            Pageable pageable);

    // ========== 统计分析查询 ==========

    /**
     * 统计各状态商品数量
     * 
     * 数据统计：
     * - 管理后台的商品统计面板
     * - 商品状态的分布分析
     * - 运营数据的监控指标
     * 
     * @return 各状态商品数量统计
     */
    @Query("SELECT p.status, COUNT(p) FROM Product p GROUP BY p.status")
    List<Object[]> countProductsByStatus();

    /**
     * 统计各分类商品数量
     * 
     * 分类统计：
     * - 分类页面的商品数量显示
     * - 分类热度的量化分析
     * - 分类运营的数据支撑
     * 
     * @param status 商品状态
     * @return 各分类商品数量统计
     */
    @Query("SELECT p.categoryId, COUNT(p) FROM Product p WHERE p.status = :status GROUP BY p.categoryId")
    List<Object[]> countProductsByCategory(@Param("status") Product.ProductStatus status);

    /**
     * 统计各品牌商品数量
     * 
     * 品牌统计：
     * - 品牌页面的商品数量展示
     * - 品牌合作的数据分析
     * - 品牌商品的分布统计
     * 
     * @param status 商品状态
     * @return 各品牌商品数量统计
     */
    @Query("SELECT p.brand, COUNT(p) FROM Product p WHERE p.brand IS NOT NULL AND p.status = :status GROUP BY p.brand ORDER BY COUNT(p) DESC")
    List<Object[]> countProductsByBrand(@Param("status") Product.ProductStatus status);

    /**
     * 获取价格统计信息
     * 
     * 价格分析：
     * - 商品价格的分布分析
     * - 定价策略的数据支撑
     * - 价格区间的统计信息
     * 
     * @param status 商品状态
     * @return 价格统计信息 [最低价, 最高价, 平均价]
     */
    @Query("SELECT MIN(p.currentPrice), MAX(p.currentPrice), AVG(p.currentPrice) FROM Product p WHERE p.status = :status")
    Object[] getPriceStatistics(@Param("status") Product.ProductStatus status);

    /**
     * 获取库存统计信息
     * 
     * 库存分析：
     * - 库存分布的统计分析
     * - 库存管理的数据监控
     * - 库存预警的统计依据
     * 
     * @param status 商品状态
     * @return 库存统计信息 [总库存, 平均库存, 低库存商品数]
     */
    @Query("SELECT SUM(p.totalStock), AVG(p.availableStock), COUNT(CASE WHEN p.availableStock <= p.lowStockThreshold THEN 1 END) FROM Product p WHERE p.status = :status")
    Object[] getStockStatistics(@Param("status") Product.ProductStatus status);

    // ========== 批量操作方法 ==========

    /**
     * 批量更新商品状态
     * 
     * 批量管理：
     * - 商品批量上架/下架
     * - 批量状态变更操作
     * - 管理效率的提升工具
     * 
     * @param productIds 商品ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id IN :productIds")
    int batchUpdateProductStatus(@Param("productIds") List<Long> productIds, 
                                @Param("status") Product.ProductStatus status);

    /**
     * 批量删除商品（软删除）
     * 
     * 软删除：
     * - 批量商品的软删除操作
     * - 保持数据完整性和历史记录
     * - 可恢复的删除机制
     * 
     * @param productIds 商品ID列表
     * @return 删除的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET p.status = 'DELETED' WHERE p.id IN :productIds")
    int batchSoftDeleteProducts(@Param("productIds") List<Long> productIds);

    /**
     * 增加商品浏览量
     * 
     * 统计更新：
     * - 商品详情页访问时更新浏览量
     * - 用户行为数据的统计
     * - 热度指标的实时更新
     * 
     * @param productId 商品ID
     * @param increment 增加数量
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + :increment WHERE p.id = :productId")
    int incrementViewCount(@Param("productId") Long productId, @Param("increment") Long increment);

    /**
     * 增加商品收藏量
     * 
     * @param productId 商品ID
     * @param increment 增加数量（可为负数表示取消收藏）
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET p.favoriteCount = p.favoriteCount + :increment WHERE p.id = :productId")
    int incrementFavoriteCount(@Param("productId") Long productId, @Param("increment") Long increment);

    /**
     * 更新商品评价统计
     * 
     * 评价更新：
     * - 用户评价后更新商品评价统计
     * - 平均评分的重新计算
     * - 评价数量的实时更新
     * 
     * @param productId 商品ID
     * @param newRating 新评分
     * @param currentReviewCount 当前评价数量
     * @param currentAverageRating 当前平均评分
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Product p SET " +
           "p.reviewCount = :newReviewCount, " +
           "p.averageRating = :newAverageRating " +
           "WHERE p.id = :productId")
    int updateProductRating(@Param("productId") Long productId,
                           @Param("newReviewCount") Long newReviewCount,
                           @Param("newAverageRating") BigDecimal newAverageRating);
}