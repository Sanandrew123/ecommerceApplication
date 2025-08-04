package com.ecommerce.service;

/*
 * 文件职责: 商品服务接口，定义商品相关的业务操作方法
 * 
 * 开发心理活动：
 * 1. 服务接口设计原则：
 *    - 业务抽象：定义清晰的业务操作接口
 *    - 职责单一：专注于商品相关的业务逻辑
 *    - 扩展友好：支持未来功能的扩展和演进
 *    - 异常清晰：明确的异常类型和处理策略
 * 
 * 2. 电商商品业务场景：
 *    - 商品管理：创建、更新、删除、查询商品
 *    - 库存管理：库存增减、预占、释放
 *    - 搜索推荐：商品搜索、相关推荐
 *    - 统计分析：销量、评价、热度统计
 * 
 * 3. 性能和缓存考虑：
 *    - 热点数据：商品详情的缓存策略
 *    - 分页查询：大数据量的分页处理
 *    - 批量操作：提升数据操作效率
 *    - 异步处理：耗时操作的异步化
 * 
 * 4. 业务规则设计：
 *    - 状态流转：商品状态的业务规则
 *    - 权限控制：不同角色的操作权限
 *    - 数据一致性：库存、价格的一致性保证
 *    - 业务校验：创建和更新时的业务验证
 * 
 * 包结构设计思路:
 * - 放在service包下，定义业务接口层
 * - 与实现类分离，便于替换和测试
 * 
 * 命名原因:
 * - ProductService明确表达商品业务服务功能
 * - 符合Service后缀的命名规范
 * 
 * 依赖关系:
 * - 被Controller调用，提供业务处理
 * - 被ServiceImpl实现，定义业务契约
 * - 独立于具体实现，便于扩展和测试
 */

import com.ecommerce.dto.request.product.ProductCreateRequest;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 商品服务接口
 * 
 * 功能说明：
 * 1. 定义商品管理的核心业务操作
 * 2. 提供商品搜索和推荐功能
 * 3. 支持库存管理和统计分析
 * 4. 确保业务逻辑的一致性和完整性
 * 
 * 业务模块：
 * 1. 商品生命周期管理：创建、更新、发布、下架、删除
 * 2. 库存管理：库存增减、预占释放、库存预警
 * 3. 搜索和推荐：多条件搜索、相关商品推荐
 * 4. 统计分析：销量统计、评价统计、热度分析
 * 
 * 设计特性：
 * 1. 接口导向：基于接口编程，便于扩展和测试
 * 2. 异常安全：明确的异常类型和处理策略
 * 3. 性能优化：缓存策略和批量操作支持
 * 4. 业务完整：覆盖电商商品的完整业务场景
 * 
 * 使用场景：
 * 1. 商家管理：商品发布、编辑、库存管理
 * 2. 用户购物：商品浏览、搜索、推荐
 * 3. 系统运营：商品统计、分析、监控
 * 4. 第三方集成：API接口、数据同步
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
public interface ProductService {

    // ========== 商品基础管理 ==========

    /**
     * 创建商品
     * 
     * 业务流程：
     * 1. 参数验证：校验商品信息的完整性和合法性
     * 2. 业务校验：分类存在性、价格合理性、库存逻辑性
     * 3. 数据处理：商品编码生成、图片处理、SEO优化
     * 4. 状态设置：根据创建者权限设置初始状态
     * 5. 索引更新：更新搜索引擎索引
     * 6. 缓存处理：清除相关缓存
     * 7. 事件通知：发送商品创建事件
     * 
     * 权限控制：
     * - 商家：可以创建自己的商品
     * - 管理员：可以创建任意商品
     * - 系统：支持批量导入创建
     * 
     * 异常处理：
     * - BusinessException：业务规则验证失败
     * - ValidationException：参数验证失败
     * - DataIntegrityException：数据完整性冲突
     * 
     * @param request 商品创建请求
     * @param creatorId 创建者ID
     * @return 创建成功的商品详情
     * @throws BusinessException 业务逻辑异常
     */
    ProductDetailResponse createProduct(ProductCreateRequest request, Long creatorId);

    /**
     * 更新商品信息
     * 
     * 更新策略：
     * 1. 权限验证：只能更新自己的商品或管理员权限
     * 2. 状态检查：某些状态下不允许更新
     * 3. 差异对比：只更新变化的字段
     * 4. 业务校验：更新后的数据合法性验证
     * 5. 关联更新：更新相关的统计数据
     * 6. 缓存刷新：清除和更新相关缓存
     * 7. 索引同步：同步搜索引擎索引
     * 
     * 限制规则：
     * - 已发布商品的某些字段不可修改
     * - 有订单的商品价格修改需要特殊处理
     * - 库存修改需要记录操作日志
     * 
     * @param productId 商品ID
     * @param request 更新请求
     * @param operatorId 操作者ID
     * @return 更新后的商品详情
     * @throws BusinessException 业务逻辑异常
     */
    ProductDetailResponse updateProduct(Long productId, ProductCreateRequest request, Long operatorId);

    /**
     * 根据ID获取商品详情
     * 
     * 查询策略：
     * 1. 缓存查询：优先从缓存中获取热点商品
     * 2. 数据库查询：缓存未命中时从数据库查询
     * 3. 权限过滤：根据用户权限过滤敏感信息
     * 4. 数据组装：组装完整的商品展示信息
     * 5. 统计更新：异步更新浏览量统计
     * 6. 推荐预热：预热相关商品推荐数据
     * 
     * 性能优化：
     * - 热点商品缓存：Redis缓存商品详情
     * - 懒加载：按需加载关联数据
     * - 数据预热：定时预热热门商品缓存
     * 
     * @param productId 商品ID
     * @param viewerId 查看者ID（可选，用于权限控制）
     * @return 商品详情响应
     * @throws BusinessException 商品不存在或无权限查看
     */
    Optional<ProductDetailResponse> getProductById(Long productId, Long viewerId);

    /**
     * 根据商品编码获取商品
     * 
     * 应用场景：
     * - 扫码查询：通过条形码或二维码查询
     * - 快速查找：通过SKU快速定位商品
     * - 系统集成：第三方系统的商品查询
     * - 批量操作：批量导入时的商品验证
     * 
     * @param code 商品编码
     * @param viewerId 查看者ID（可选）
     * @return 商品详情响应
     */
    Optional<ProductDetailResponse> getProductByCode(String code, Long viewerId);

    /**
     * 批量获取商品信息
     * 
     * 批量查询：
     * 1. 参数验证：批量ID的数量限制和格式验证
     * 2. 权限过滤：过滤无权限查看的商品
     * 3. 缓存查询：批量查询缓存中的商品
     * 4. 数据库补充：查询缓存未命中的商品
     * 5. 结果组装：按请求顺序组装返回结果
     * 
     * 性能优化：
     * - 批量查询：减少数据库查询次数
     * - 缓存利用：最大化利用缓存数据
     * - 并行处理：并行处理多个查询请求
     * 
     * @param productIds 商品ID列表
     * @param viewerId 查看者ID（可选）
     * @return 商品详情列表
     */
    List<ProductDetailResponse> getProductsByIds(List<Long> productIds, Long viewerId);

    /**
     * 删除商品（软删除）
     * 
     * 删除策略：
     * 1. 权限验证：只能删除自己的商品或管理员权限
     * 2. 状态检查：某些状态下不允许删除
     * 3. 关联检查：检查是否有未完成的订单
     * 4. 软删除：标记删除状态而不是物理删除
     * 5. 关联处理：处理购物车中的该商品
     * 6. 缓存清理：清除相关缓存数据
     * 7. 索引更新：从搜索引擎中移除
     * 
     * 业务规则：
     * - 有未完成订单的商品不能删除
     * - 删除后的商品可以恢复
     * - 删除操作需要记录操作日志
     * 
     * @param productId 商品ID
     * @param operatorId 操作者ID
     * @return 删除是否成功
     * @throws BusinessException 删除条件不满足
     */
    boolean deleteProduct(Long productId, Long operatorId);

    /**
     * 批量删除商品
     * 
     * 批量删除：
     * 1. 权限验证：验证所有商品的删除权限
     * 2. 状态检查：检查所有商品的删除条件
     * 3. 事务处理：确保批量操作的原子性
     * 4. 异常处理：部分失败时的处理策略
     * 5. 日志记录：记录批量删除的操作日志
     * 
     * @param productIds 商品ID列表
     * @param operatorId 操作者ID
     * @return 删除结果统计
     */
    BatchOperationResult batchDeleteProducts(List<Long> productIds, Long operatorId);

    // ========== 商品状态管理 ==========

    /**
     * 发布商品（上架）
     * 
     * 发布流程：
     * 1. 状态验证：当前状态是否允许发布
     * 2. 信息完整性：检查商品信息是否完整
     * 3. 审核检查：是否需要审核流程
     * 4. 状态更新：更新为发布状态
     * 5. 时间记录：记录发布时间
     * 6. 搜索索引：添加到搜索引擎
     * 7. 缓存预热：预热商品详情缓存
     * 8. 通知推送：通知关注用户
     * 
     * 发布条件：
     * - 商品信息完整
     * - 有可用库存
     * - 价格设置合理
     * - 必要的审核通过
     * 
     * @param productId 商品ID
     * @param operatorId 操作者ID
     * @return 发布是否成功
     * @throws BusinessException 发布条件不满足
     */
    boolean publishProduct(Long productId, Long operatorId);

    /**
     * 下架商品
     * 
     * 下架处理：
     * 1. 状态更新：更新为下架状态
     * 2. 时间记录：记录下架时间
     * 3. 购物车处理：从用户购物车中移除
     * 4. 搜索移除：从搜索结果中移除
     * 5. 缓存清理：清除相关缓存
     * 6. 用户通知：通知收藏该商品的用户
     * 
     * @param productId 商品ID
     * @param operatorId 操作者ID
     * @param reason 下架原因
     * @return 下架是否成功
     */
    boolean unpublishProduct(Long productId, Long operatorId, String reason);

    /**
     * 批量更新商品状态
     * 
     * @param productIds 商品ID列表
     * @param status 目标状态
     * @param operatorId 操作者ID
     * @return 更新结果统计
     */
    BatchOperationResult batchUpdateProductStatus(List<Long> productIds, Product.ProductStatus status, Long operatorId);

    // ========== 商品查询和搜索 ==========

    /**
     * 分页查询商品列表
     * 
     * 查询功能：
     * 1. 基础筛选：按状态、分类、品牌筛选
     * 2. 价格筛选：价格区间筛选
     * 3. 排序支持：价格、销量、评分、时间排序
     * 4. 分页处理：支持大数据量的分页查询
     * 5. 缓存策略：热门查询结果缓存
     * 6. 权限过滤：根据用户权限过滤结果
     * 
     * 性能优化：
     * - 索引利用：基于数据库索引优化查询
     * - 结果缓存：缓存常用查询结果
     * - 分页优化：使用游标分页优化深分页
     * 
     * @param categoryId 分类ID（可选）
     * @param brand 品牌（可选）
     * @param status 商品状态（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param pageable 分页参数
     * @param viewerId 查看者ID（可选）
     * @return 商品分页列表
     */
    Page<ProductDetailResponse> getProducts(Long categoryId, String brand, Product.ProductStatus status,
                                          BigDecimal minPrice, BigDecimal maxPrice, 
                                          Pageable pageable, Long viewerId);

    /**
     * 搜索商品
     * 
     * 搜索功能：
     * 1. 关键词搜索：商品名称、描述、标签的全文检索
     * 2. 高级筛选：分类、品牌、价格、属性等多维度筛选
     * 3. 智能排序：相关性、销量、评分的综合排序
     * 4. 搜索建议：输入提示和搜索推荐
     * 5. 搜索统计：搜索词统计和热门搜索
     * 6. 个性化：基于用户行为的个性化搜索
     * 
     * 搜索优化：
     * - 分词匹配：中文分词和同义词扩展
     * - 搜索缓存：热门搜索结果缓存
     * - 实时索引：商品变更的实时索引更新
     * 
     * @param keyword 搜索关键词
     * @param categoryId 分类筛选（可选）
     * @param brand 品牌筛选（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param pageable 分页参数
     * @param searcherId 搜索者ID（可选）
     * @return 搜索结果分页列表
     */
    Page<ProductDetailResponse> searchProducts(String keyword, Long categoryId, String brand,
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             String sortBy, String sortDirection,
                                             Pageable pageable, Long searcherId);

    /**
     * 获取相关商品推荐
     * 
     * 推荐算法：
     * 1. 协同过滤：基于用户行为的协同推荐
     * 2. 内容推荐：基于商品属性的相似性推荐
     * 3. 热度推荐：基于销量和评价的热度推荐
     * 4. 分类推荐：同分类下的相关商品推荐
     * 5. 品牌推荐：同品牌的其他商品推荐
     * 6. 价格推荐：相似价格区间的商品推荐
     * 
     * 推荐优化：
     * - 实时计算：结合实时用户行为
     * - 多样性：避免推荐结果过于单一
     * - 个性化：基于用户画像的个性化推荐
     * 
     * @param productId 当前商品ID
     * @param userId 用户ID（可选，用于个性化推荐）
     * @param limit 推荐数量限制
     * @return 相关商品推荐列表
     */
    List<ProductDetailResponse> getRelatedProducts(Long productId, Long userId, int limit);

    /**
     * 获取热门商品
     * 
     * 热门定义：
     * 1. 销量排行：基于销量统计的热门商品
     * 2. 浏览排行：基于浏览量的热门商品
     * 3. 收藏排行：基于收藏数的热门商品
     * 4. 评价排行：基于评价分数的热门商品
     * 5. 综合排行：多指标加权的综合热门度
     * 
     * 时间维度：
     * - 实时热门：当前时段的热门商品
     * - 日热门：24小时内的热门商品
     * - 周热门：7天内的热门商品
     * - 月热门：30天内的热门商品
     * 
     * @param categoryId 分类ID（可选）
     * @param timeRange 时间范围
     * @param limit 数量限制
     * @return 热门商品列表
     */
    List<ProductDetailResponse> getHotProducts(Long categoryId, String timeRange, int limit);

    /**
     * 获取最新商品
     * 
     * @param categoryId 分类ID（可选）
     * @param limit 数量限制
     * @return 最新商品列表
     */
    List<ProductDetailResponse> getLatestProducts(Long categoryId, int limit);

    // ========== 库存管理 ==========

    /**
     * 更新商品库存
     * 
     * 库存更新：
     * 1. 并发控制：使用乐观锁或悲观锁防止并发问题
     * 2. 业务校验：库存数量的合理性验证
     * 3. 状态联动：库存变化时商品状态的联动更新
     * 4. 预警检查：库存低于预警线时触发通知
     * 5. 日志记录：详细记录库存变更日志
     * 6. 缓存更新：实时更新库存缓存
     * 
     * 库存规则：
     * - 库存不能为负数
     * - 有预占库存时不能随意减少总库存
     * - 库存为0时自动设置缺货状态
     * 
     * @param productId 商品ID
     * @param quantity 库存数量
     * @param operatorId 操作者ID
     * @param reason 库存变更原因
     * @return 更新是否成功
     * @throws BusinessException 库存更新失败
     */
    boolean updateProductStock(Long productId, int quantity, Long operatorId, String reason);

    /**
     * 预占库存
     * 
     * 预占机制：
     * 1. 库存检查：验证可用库存是否充足
     * 2. 原子操作：确保预占操作的原子性
     * 3. 超时处理：设置预占超时自动释放
     * 4. 并发控制：防止超卖问题
     * 5. 状态更新：更新库存状态
     * 
     * 应用场景：
     * - 下单预占：用户下单时预占库存
     * - 活动预占：秒杀活动的库存预占
     * - 批量预占：批量订单的库存预占
     * 
     * @param productId 商品ID
     * @param quantity 预占数量
     * @param reserverId 预占者ID
     * @param timeoutMinutes 预占超时时间（分钟）
     * @return 预占结果
     */
    StockReservationResult reserveStock(Long productId, int quantity, Long reserverId, int timeoutMinutes);

    /**
     * 释放预占库存
     * 
     * 释放场景：
     * - 订单取消：用户取消订单释放预占
     * - 支付超时：支付超时自动释放预占
     * - 库存调整：管理员手动释放预占
     * 
     * @param productId 商品ID
     * @param quantity 释放数量
     * @param reserverId 预占者ID
     * @return 释放是否成功
     */
    boolean releaseReservedStock(Long productId, int quantity, Long reserverId);

    /**
     * 确认预占库存（转为销售）
     * 
     * 确认场景：
     * - 支付成功：支付成功后确认预占库存
     * - 发货确认：发货时确认库存扣减
     * 
     * @param productId 商品ID
     * @param quantity 确认数量
     * @param reserverId 预占者ID
     * @return 确认是否成功
     */
    boolean confirmReservedStock(Long productId, int quantity, Long reserverId);

    /**
     * 获取低库存商品列表
     * 
     * @param pageable 分页参数
     * @return 低库存商品分页列表
     */
    Page<ProductDetailResponse> getLowStockProducts(Pageable pageable);

    // ========== 统计分析 ==========

    /**
     * 更新商品浏览量
     * 
     * 统计策略：
     * 1. 去重统计：同一用户24小时内多次浏览只计一次
     * 2. 异步更新：使用异步方式更新避免影响查询性能
     * 3. 批量更新：定期批量更新数据库
     * 4. 缓存计数：使用Redis缓存实时计数
     * 
     * @param productId 商品ID
     * @param viewerId 浏览者ID（可选）
     * @return 更新是否成功
     */
    boolean incrementViewCount(Long productId, Long viewerId);

    /**
     * 更新商品收藏量
     * 
     * @param productId 商品ID
     * @param increment 增量（正数表示收藏，负数表示取消收藏）
     * @return 更新是否成功
     */
    boolean updateFavoriteCount(Long productId, int increment);

    /**
     * 更新商品评价统计
     * 
     * 评价统计：
     * 1. 评分计算：重新计算平均评分
     * 2. 评价数量：更新评价总数
     * 3. 好评率：计算好评率统计
     * 4. 标签统计：统计评价标签分布
     * 5. 缓存更新：更新评价相关缓存
     * 
     * @param productId 商品ID
     * @param rating 评分
     * @param reviewId 评价ID
     * @return 更新是否成功
     */
    boolean updateProductRating(Long productId, BigDecimal rating, Long reviewId);

    /**
     * 获取商品统计信息
     * 
     * 统计维度：
     * 1. 基础统计：商品总数、各状态分布
     * 2. 分类统计：各分类商品数量分布
     * 3. 品牌统计：各品牌商品数量分布
     * 4. 价格统计：价格分布区间统计
     * 5. 库存统计：库存分布和预警统计
     * 6. 销售统计：销量和评价统计
     * 
     * @return 商品统计信息
     */
    ProductStatistics getProductStatistics();

    /**
     * 获取商品销售趋势
     * 
     * 趋势分析：
     * 1. 时间维度：日、周、月销售趋势
     * 2. 分类维度：各分类销售趋势对比
     * 3. 价格维度：不同价格区间销售趋势
     * 4. 地域维度：不同地区销售趋势
     * 
     * @param productId 商品ID（可选，为空时统计全部）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param dimension 统计维度
     * @return 销售趋势数据
     */
    List<SalesTrendData> getSalesTrend(Long productId, LocalDateTime startDate, 
                                      LocalDateTime endDate, String dimension);

    // ========== 业务结果类定义 ==========

    /**
     * 批量操作结果
     */
    record BatchOperationResult(
        int total,           // 总数
        int success,         // 成功数
        int failed,          // 失败数
        List<String> errors  // 错误信息列表
    ) {}

    /**
     * 库存预占结果
     */
    record StockReservationResult(
        boolean success,           // 预占是否成功
        String reservationId,      // 预占ID
        int reservedQuantity,      // 预占数量
        LocalDateTime expiryTime,  // 过期时间
        String message            // 结果消息
    ) {}

    /**
     * 商品统计信息
     */
    record ProductStatistics(
        long totalProducts,                    // 商品总数
        Map<String, Long> statusDistribution, // 状态分布
        Map<String, Long> categoryDistribution, // 分类分布
        Map<String, Long> brandDistribution,  // 品牌分布
        PriceStatistics priceStats,           // 价格统计
        StockStatistics stockStats            // 库存统计
    ) {}

    /**
     * 价格统计信息
     */
    record PriceStatistics(
        BigDecimal minPrice,     // 最低价
        BigDecimal maxPrice,     // 最高价
        BigDecimal avgPrice,     // 平均价
        BigDecimal medianPrice   // 中位价
    ) {}

    /**
     * 库存统计信息
     */
    record StockStatistics(
        long totalStock,        // 总库存
        long availableStock,    // 可用库存
        long reservedStock,     // 预占库存
        long lowStockCount,     // 低库存商品数
        long outOfStockCount    // 缺货商品数
    ) {}

    /**
     * 销售趋势数据
     */
    record SalesTrendData(
        LocalDateTime date,     // 日期
        long salesCount,        // 销量
        BigDecimal salesAmount, // 销售额
        long orderCount,        // 订单数
        String dimension        // 统计维度
    ) {}
}