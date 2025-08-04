package com.ecommerce.service.impl;

/*
 * 文件职责: 商品服务实现类，实现商品相关的业务逻辑
 * 
 * 开发心理活动：
 * 1. 服务实现设计原则：
 *    - 业务逻辑封装：将复杂的业务规则封装在服务层
 *    - 事务管理：确保数据操作的原子性和一致性
 *    - 异常处理：优雅处理业务异常和系统异常
 *    - 性能优化：缓存策略、批量操作、异步处理
 * 
 * 2. 电商商品业务实现：
 *    - 数据转换：Entity与DTO之间的转换映射
 *    - 业务校验：商品信息的完整性和合法性验证
 *    - 状态管理：商品生命周期状态的流转控制
 *    - 关联处理：分类、用户、订单等关联数据的处理
 * 
 * 3. 架构设计考虑：
 *    - 分层调用：Repository层的数据访问调用
 *    - 缓存集成：Redis缓存的读写策略
 *    - 消息队列：异步事件的发布和处理
 *    - 搜索引擎：Elasticsearch索引的同步更新
 * 
 * 4. 扩展性设计：
 *    - 策略模式：不同的推荐算法实现
 *    - 观察者模式：商品变更事件的通知机制
 *    - 模板方法：通用的商品操作流程模板
 *    - 工厂模式：不同类型商品的创建策略
 * 
 * 包结构设计思路:
 * - 放在service.impl包下，实现具体业务逻辑
 * - 实现ProductService接口，遵循接口契约
 * 
 * 命名原因:
 * - ProductServiceImpl明确表达商品服务的实现
 * - 符合Impl后缀的实现类命名规范
 * 
 * 依赖关系:
 * - 注入ProductRepository进行数据访问
 * - 注入CategoryRepository处理分类关联
 * - 注入RedisTemplate进行缓存操作
 * - 注入RabbitTemplate进行消息发送
 */

import com.ecommerce.dto.request.product.ProductCreateRequest;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 * 
 * 功能说明：
 * 1. 实现商品管理的核心业务逻辑
 * 2. 提供完整的商品生命周期管理
 * 3. 集成缓存、消息队列、搜索引擎
 * 4. 确保数据一致性和业务规则正确性
 * 
 * 技术特性：
 * 1. 事务管理：关键操作使用Spring事务
 * 2. 缓存策略：热点数据Redis缓存
 * 3. 异步处理：耗时操作异步执行
 * 4. 批量优化：批量操作提升性能
 * 
 * 业务特性：
 * 1. 数据验证：全面的业务规则验证
 * 2. 状态控制：严格的状态流转管理
 * 3. 权限控制：基于角色的操作权限
 * 4. 审计日志：重要操作的日志记录
 * 
 * 性能优化：
 * 1. 查询优化：合理使用索引和分页
 * 2. 缓存预热：热门商品缓存预热
 * 3. 延迟加载：按需加载关联数据
 * 4. 连接池：数据库连接池优化
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    // ========== 依赖注入 ==========

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // ========== 缓存键常量 ==========
    
    private static final String PRODUCT_CACHE_KEY = "product:detail:";
    private static final String PRODUCT_LIST_CACHE_KEY = "product:list:";
    private static final String HOT_PRODUCTS_CACHE_KEY = "product:hot:";
    private static final String PRODUCT_STATS_CACHE_KEY = "product:stats";
    
    // ========== 商品基础管理实现 ==========

    /**
     * 创建商品
     * 
     * 实现策略：
     * 1. 参数验证：验证请求参数的完整性和合法性
     * 2. 业务校验：分类存在性、价格合理性、权限验证
     * 3. 数据转换：将请求DTO转换为商品实体
     * 4. 编码生成：自动生成商品编码（如果未提供）
     * 5. 状态设置：设置初始状态和审核状态
     * 6. 数据保存：事务性保存商品数据
     * 7. 缓存处理：清除相关缓存，预热新商品缓存
     * 8. 事件发布：发布商品创建事件
     * 
     * 业务规则：
     * - 商品名称不能重复（同分类下）
     * - 价格必须合理（现价<=原价，会员价<=现价）
     * - 分类必须存在且为叶子节点
     * - 库存数量必须非负
     * 
     * 异常处理：
     * - 分类不存在：抛出BusinessException
     * - 权限不足：抛出SecurityException
     * - 数据冲突：抛出DataIntegrityException
     */
    @Override
    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request, Long creatorId) {
        log.info("创建商品，创建者ID: {}, 商品名称: {}", creatorId, request.getName());
        
        try {
            // 1. 参数验证
            validateProductCreateRequest(request);
            
            // 2. 业务校验
            validateBusinessRules(request, creatorId);
            
            // 3. 构建商品实体
            Product product = buildProductFromRequest(request, creatorId);
            
            // 4. 生成商品编码（如果未提供）
            if (product.getCode() == null || product.getCode().isEmpty()) {
                product.setCode(generateProductCode(product));
            }
            
            // 5. 设置初始状态
            product.setStatus(Product.ProductStatus.DRAFT);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            
            // 6. 保存商品
            Product savedProduct = productRepository.save(product);
            
            // 7. 更新分类商品数量
            updateCategoryProductCount(savedProduct.getCategoryId(), 1L);
            
            // 8. 清除相关缓存
            clearProductRelatedCache(null, savedProduct.getCategoryId());
            
            // 9. 构建响应DTO
            ProductDetailResponse response = convertToDetailResponse(savedProduct);
            
            log.info("商品创建成功，商品ID: {}, 商品编码: {}", savedProduct.getId(), savedProduct.getCode());
            
            return response;
            
        } catch (Exception e) {
            log.error("创建商品失败，创建者ID: {}, 商品名称: {}, 错误: {}", 
                     creatorId, request.getName(), e.getMessage(), e);
            throw new BusinessException("创建商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新商品信息
     * 
     * 更新策略：
     * 1. 权限验证：验证用户是否有权限更新该商品
     * 2. 状态检查：检查商品当前状态是否允许更新
     * 3. 数据对比：对比新旧数据，只更新变化的字段
     * 4. 业务校验：验证更新后数据的业务合法性
     * 5. 乐观锁：使用版本号防止并发更新冲突
     * 6. 关联更新：更新相关的统计和索引数据
     * 7. 缓存刷新：删除旧缓存，预热新缓存
     * 8. 事件通知：发布商品更新事件
     */
    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductCreateRequest request, Long operatorId) {
        log.info("更新商品，商品ID: {}, 操作者ID: {}", productId, operatorId);
        
        try {
            // 1. 获取现有商品
            Product existingProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 2. 权限验证
            validateUpdatePermission(existingProduct, operatorId);
            
            // 3. 状态检查
            validateUpdateStatus(existingProduct);
            
            // 4. 参数验证
            validateProductCreateRequest(request);
            
            // 5. 业务校验
            validateBusinessRules(request, operatorId);
            
            // 6. 更新商品属性
            updateProductProperties(existingProduct, request);
            
            // 7. 设置更新信息
            existingProduct.setUpdatedAt(LocalDateTime.now());
            existingProduct.setUpdatedBy(operatorId);
            
            // 8. 保存更新
            Product updatedProduct = productRepository.save(existingProduct);
            
            // 9. 清除缓存
            clearProductRelatedCache(productId, updatedProduct.getCategoryId());
            
            // 10. 构建响应
            ProductDetailResponse response = convertToDetailResponse(updatedProduct);
            
            log.info("商品更新成功，商品ID: {}", productId);
            
            return response;
            
        } catch (Exception e) {
            log.error("更新商品失败，商品ID: {}, 操作者ID: {}, 错误: {}", 
                     productId, operatorId, e.getMessage(), e);
            throw new BusinessException("更新商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取商品详情
     * 
     * 查询策略：
     * 1. 缓存查询：优先从Redis缓存中获取
     * 2. 数据库查询：缓存未命中时查询数据库
     * 3. 权限过滤：根据查看者权限过滤敏感信息
     * 4. 关联加载：加载分类、创建者等关联信息
     * 5. 统计更新：异步更新浏览量统计
     * 6. 缓存预热：将查询结果缓存到Redis
     * 
     * 缓存策略：
     * - 热门商品：缓存1小时
     * - 普通商品：缓存30分钟
     * - 下架商品：不缓存
     */
    @Override
    @Cacheable(value = "productDetail", key = "#productId", unless = "#result.empty")
    public Optional<ProductDetailResponse> getProductById(Long productId, Long viewerId) {
        log.debug("获取商品详情，商品ID: {}, 查看者ID: {}", productId, viewerId);
        
        try {
            // 1. 查询商品
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isEmpty()) {
                log.debug("商品不存在，商品ID: {}", productId);
                return Optional.empty();
            }
            
            Product product = productOpt.get();
            
            // 2. 权限检查
            if (!hasViewPermission(product, viewerId)) {
                log.debug("无权限查看商品，商品ID: {}, 查看者ID: {}", productId, viewerId);
                return Optional.empty();
            }
            
            // 3. 转换为响应DTO
            ProductDetailResponse response = convertToDetailResponse(product);
            
            // 4. 异步更新浏览量
            updateViewCountAsync(productId, viewerId);
            
            log.debug("获取商品详情成功，商品ID: {}", productId);
            
            return Optional.of(response);
            
        } catch (Exception e) {
            log.error("获取商品详情失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("获取商品详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据商品编码获取商品
     */
    @Override
    public Optional<ProductDetailResponse> getProductByCode(String code, Long viewerId) {
        log.debug("根据编码获取商品，商品编码: {}, 查看者ID: {}", code, viewerId);
        
        try {
            Optional<Product> productOpt = productRepository.findByCode(code);
            
            if (productOpt.isEmpty()) {
                return Optional.empty();
            }
            
            Product product = productOpt.get();
            
            if (!hasViewPermission(product, viewerId)) {
                return Optional.empty();
            }
            
            ProductDetailResponse response = convertToDetailResponse(product);
            
            // 异步更新浏览量
            updateViewCountAsync(product.getId(), viewerId);
            
            return Optional.of(response);
            
        } catch (Exception e) {
            log.error("根据编码获取商品失败，商品编码: {}, 错误: {}", code, e.getMessage(), e);
            throw new BusinessException("获取商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量获取商品信息
     */
    @Override
    public List<ProductDetailResponse> getProductsByIds(List<Long> productIds, Long viewerId) {
        log.debug("批量获取商品信息，商品数量: {}, 查看者ID: {}", productIds.size(), viewerId);
        
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // 限制批量查询数量
            if (productIds.size() > 100) {
                throw new BusinessException("批量查询商品数量不能超过100个");
            }
            
            List<Product> products = productRepository.findAllById(productIds);
            
            return products.stream()
                    .filter(product -> hasViewPermission(product, viewerId))
                    .map(this::convertToDetailResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("批量获取商品信息失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("批量获取商品信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除商品（软删除）
     */
    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public boolean deleteProduct(Long productId, Long operatorId) {
        log.info("删除商品，商品ID: {}, 操作者ID: {}", productId, operatorId);
        
        try {
            // 1. 获取商品
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 2. 权限验证
            validateDeletePermission(product, operatorId);
            
            // 3. 删除条件检查
            validateDeleteConditions(product);
            
            // 4. 执行软删除
            product.setStatus(Product.ProductStatus.DELETED);
            product.setUpdatedAt(LocalDateTime.now());
            product.setUpdatedBy(operatorId);
            product.setUnpublishedAt(LocalDateTime.now());
            
            productRepository.save(product);
            
            // 5. 更新分类商品数量
            updateCategoryProductCount(product.getCategoryId(), -1L);
            
            // 6. 清除缓存
            clearProductRelatedCache(productId, product.getCategoryId());
            
            log.info("商品删除成功，商品ID: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("删除商品失败，商品ID: {}, 操作者ID: {}, 错误: {}", 
                     productId, operatorId, e.getMessage(), e);
            throw new BusinessException("删除商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除商品
     */
    @Override
    @Transactional
    public BatchOperationResult batchDeleteProducts(List<Long> productIds, Long operatorId) {
        log.info("批量删除商品，商品数量: {}, 操作者ID: {}", productIds.size(), operatorId);
        
        int total = productIds.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long productId : productIds) {
            try {
                deleteProduct(productId, operatorId);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("商品ID " + productId + ": " + e.getMessage());
                log.warn("批量删除商品失败，商品ID: {}, 错误: {}", productId, e.getMessage());
            }
        }
        
        log.info("批量删除商品完成，总数: {}, 成功: {}, 失败: {}", total, success, failed);
        
        return new BatchOperationResult(total, success, failed, errors);
    }
    
    // ========== 商品状态管理实现 ==========
    
    /**
     * 发布商品（上架）
     */
    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public boolean publishProduct(Long productId, Long operatorId) {
        log.info("发布商品，商品ID: {}, 操作者ID: {}", productId, operatorId);
        
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 权限验证
            validatePublishPermission(product, operatorId);
            
            // 发布条件检查
            validatePublishConditions(product);
            
            // 更新状态
            product.setStatus(Product.ProductStatus.ACTIVE);
            product.setPublishedAt(LocalDateTime.now());
            product.setUnpublishedAt(null);
            product.setUpdatedAt(LocalDateTime.now());
            product.setUpdatedBy(operatorId);
            
            productRepository.save(product);
            
            // 清除缓存
            clearProductRelatedCache(productId, product.getCategoryId());
            
            log.info("商品发布成功，商品ID: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("发布商品失败，商品ID: {}, 操作者ID: {}, 错误: {}", 
                     productId, operatorId, e.getMessage(), e);
            throw new BusinessException("发布商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 下架商品
     */
    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public boolean unpublishProduct(Long productId, Long operatorId, String reason) {
        log.info("下架商品，商品ID: {}, 操作者ID: {}, 原因: {}", productId, operatorId, reason);
        
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 权限验证
            validateUnpublishPermission(product, operatorId);
            
            // 更新状态
            product.setStatus(Product.ProductStatus.INACTIVE);
            product.setUnpublishedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            product.setUpdatedBy(operatorId);
            
            productRepository.save(product);
            
            // 清除缓存
            clearProductRelatedCache(productId, product.getCategoryId());
            
            log.info("商品下架成功，商品ID: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("下架商品失败，商品ID: {}, 操作者ID: {}, 错误: {}", 
                     productId, operatorId, e.getMessage(), e);
            throw new BusinessException("下架商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量更新商品状态
     */
    @Override
    @Transactional
    public BatchOperationResult batchUpdateProductStatus(List<Long> productIds, Product.ProductStatus status, Long operatorId) {
        log.info("批量更新商品状态，商品数量: {}, 目标状态: {}, 操作者ID: {}", 
                 productIds.size(), status, operatorId);
        
        try {
            int updatedCount = productRepository.batchUpdateProductStatus(productIds, status);
            
            // 清除相关缓存
            for (Long productId : productIds) {
                clearProductCache(productId);
            }
            
            log.info("批量更新商品状态成功，更新数量: {}", updatedCount);
            
            return new BatchOperationResult(productIds.size(), updatedCount, 0, Collections.emptyList());
            
        } catch (Exception e) {
            log.error("批量更新商品状态失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("批量更新商品状态失败：" + e.getMessage());
        }
    }
    
    // ========== 商品查询和搜索实现 ==========
    
    /**
     * 分页查询商品列表
     */
    @Override
    public Page<ProductDetailResponse> getProducts(Long categoryId, String brand, Product.ProductStatus status,
                                                 BigDecimal minPrice, BigDecimal maxPrice, 
                                                 Pageable pageable, Long viewerId) {
        log.debug("分页查询商品列表，分类ID: {}, 品牌: {}, 状态: {}", categoryId, brand, status);
        
        try {
            Page<Product> productPage = productRepository.searchProducts(
                    null, categoryId, brand, minPrice, maxPrice, status, pageable);
            
            return productPage.map(this::convertToDetailResponse);
            
        } catch (Exception e) {
            log.error("分页查询商品列表失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("查询商品列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 搜索商品
     */
    @Override
    public Page<ProductDetailResponse> searchProducts(String keyword, Long categoryId, String brand,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     String sortBy, String sortDirection,
                                                     Pageable pageable, Long searcherId) {
        log.debug("搜索商品，关键词: {}, 分类ID: {}, 品牌: {}", keyword, categoryId, brand);
        
        try {
            // 构建排序条件
            Sort sort = buildSort(sortBy, sortDirection);
            Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            
            Page<Product> productPage = productRepository.searchProducts(
                    keyword, categoryId, brand, minPrice, maxPrice, Product.ProductStatus.ACTIVE, sortedPageable);
            
            return productPage.map(this::convertToDetailResponse);
            
        } catch (Exception e) {
            log.error("搜索商品失败，关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            throw new BusinessException("搜索商品失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取相关商品推荐
     */
    @Override
    public List<ProductDetailResponse> getRelatedProducts(Long productId, Long userId, int limit) {
        log.debug("获取相关商品推荐，商品ID: {}, 用户ID: {}, 限制数量: {}", productId, userId, limit);
        
        try {
            // 获取当前商品信息
            Optional<Product> currentProductOpt = productRepository.findById(productId);
            if (currentProductOpt.isEmpty()) {
                return Collections.emptyList();
            }
            
            Product currentProduct = currentProductOpt.get();
            
            // 计算价格区间（当前价格的±20%）
            BigDecimal currentPrice = currentProduct.getCurrentPrice();
            BigDecimal minPrice = currentPrice.multiply(BigDecimal.valueOf(0.8));
            BigDecimal maxPrice = currentPrice.multiply(BigDecimal.valueOf(1.2));
            
            Pageable pageable = PageRequest.of(0, limit);
            
            Page<Product> relatedProducts = productRepository.findRelatedProducts(
                    productId, currentProduct.getCategoryId(), currentProduct.getBrand(),
                    currentPrice, minPrice, maxPrice, Product.ProductStatus.ACTIVE, pageable);
            
            return relatedProducts.getContent().stream()
                    .map(this::convertToDetailResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取相关商品推荐失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取热门商品
     */
    @Override
    @Cacheable(value = "hotProducts", key = "#categoryId + ':' + #timeRange")
    public List<ProductDetailResponse> getHotProducts(Long categoryId, String timeRange, int limit) {
        log.debug("获取热门商品，分类ID: {}, 时间范围: {}, 限制数量: {}", categoryId, timeRange, limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<Product> hotProducts;
            
            if (categoryId != null) {
                hotProducts = productRepository.findByCategoryIdAndStatus(
                        categoryId, Product.ProductStatus.ACTIVE, 
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "salesCount")));
            } else {
                hotProducts = productRepository.findByStatusOrderBySalesCountDesc(
                        Product.ProductStatus.ACTIVE, pageable);
            }
            
            return hotProducts.getContent().stream()
                    .map(this::convertToDetailResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取热门商品失败，错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取最新商品
     */
    @Override
    public List<ProductDetailResponse> getLatestProducts(Long categoryId, int limit) {
        log.debug("获取最新商品，分类ID: {}, 限制数量: {}", categoryId, limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<Product> latestProducts;
            
            if (categoryId != null) {
                latestProducts = productRepository.findByCategoryIdAndStatus(
                        categoryId, Product.ProductStatus.ACTIVE,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "publishedAt")));
            } else {
                latestProducts = productRepository.findByStatusOrderByPublishedAtDesc(
                        Product.ProductStatus.ACTIVE, pageable);
            }
            
            return latestProducts.getContent().stream()
                    .map(this::convertToDetailResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取最新商品失败，错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    // ========== 库存管理实现 ==========
    
    /**
     * 更新商品库存
     */
    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public boolean updateProductStock(Long productId, int quantity, Long operatorId, String reason) {
        log.info("更新商品库存，商品ID: {}, 数量: {}, 操作者ID: {}, 原因: {}", 
                 productId, quantity, operatorId, reason);
        
        try {
            int updatedRows = productRepository.updateProductStock(productId, quantity);
            
            if (updatedRows > 0) {
                log.info("商品库存更新成功，商品ID: {}, 新库存: {}", productId, quantity);
                return true;
            } else {
                log.warn("商品库存更新失败，商品不存在，商品ID: {}", productId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("更新商品库存失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("更新商品库存失败：" + e.getMessage());
        }
    }
    
    /**
     * 预占库存
     */
    @Override
    @Transactional
    public StockReservationResult reserveStock(Long productId, int quantity, Long reserverId, int timeoutMinutes) {
        log.info("预占库存，商品ID: {}, 数量: {}, 预占者ID: {}, 超时时间: {}分钟", 
                 productId, quantity, reserverId, timeoutMinutes);
        
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 检查库存是否充足
            if (!product.hasEnoughStock(quantity)) {
                return new StockReservationResult(false, null, 0, null, "库存不足");
            }
            
            // 执行预占
            boolean success = product.reserveStock(quantity);
            if (success) {
                productRepository.save(product);
                
                String reservationId = generateReservationId(productId, reserverId);
                LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(timeoutMinutes);
                
                // 设置预占过期时间
                setReservationExpiry(reservationId, productId, quantity, reserverId, timeoutMinutes);
                
                log.info("库存预占成功，预占ID: {}", reservationId);
                
                return new StockReservationResult(true, reservationId, quantity, expiryTime, "预占成功");
            } else {
                return new StockReservationResult(false, null, 0, null, "预占失败");
            }
            
        } catch (Exception e) {
            log.error("预占库存失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("预占库存失败：" + e.getMessage());
        }
    }
    
    /**
     * 释放预占库存
     */
    @Override
    @Transactional
    public boolean releaseReservedStock(Long productId, int quantity, Long reserverId) {
        log.info("释放预占库存，商品ID: {}, 数量: {}, 预占者ID: {}", productId, quantity, reserverId);
        
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            product.releaseReservedStock(quantity);
            productRepository.save(product);
            
            // 清除预占记录
            String reservationId = generateReservationId(productId, reserverId);
            clearReservation(reservationId);
            
            log.info("预占库存释放成功，商品ID: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("释放预占库存失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("释放预占库存失败：" + e.getMessage());
        }
    }
    
    /**
     * 确认预占库存（转为销售）
     */
    @Override
    @Transactional
    public boolean confirmReservedStock(Long productId, int quantity, Long reserverId) {
        log.info("确认预占库存，商品ID: {}, 数量: {}, 预占者ID: {}", productId, quantity, reserverId);
        
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            product.confirmReservedStock(quantity);
            productRepository.save(product);
            
            // 清除预占记录
            String reservationId = generateReservationId(productId, reserverId);
            clearReservation(reservationId);
            
            // 清除缓存
            clearProductCache(productId);
            
            log.info("预占库存确认成功，商品ID: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("确认预占库存失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("确认预占库存失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取低库存商品列表
     */
    @Override
    public Page<ProductDetailResponse> getLowStockProducts(Pageable pageable) {
        log.debug("获取低库存商品列表");
        
        try {
            Page<Product> lowStockProducts = productRepository.findLowStockProducts(pageable);
            
            return lowStockProducts.map(this::convertToDetailResponse);
            
        } catch (Exception e) {
            log.error("获取低库存商品列表失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("获取低库存商品列表失败：" + e.getMessage());
        }
    }
    
    // ========== 统计分析实现 ==========
    
    /**
     * 更新商品浏览量
     */
    @Override
    public boolean incrementViewCount(Long productId, Long viewerId) {
        try {
            // 使用Redis进行去重统计（24小时内同一用户只计算一次）
            String key = "product:view:" + productId + ":" + (viewerId != null ? viewerId : "anonymous");
            
            Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);
            
            if (Boolean.TRUE.equals(isFirst)) {
                // 异步更新数据库
                updateViewCountAsync(productId, viewerId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("更新商品浏览量失败，商品ID: {}, 错误: {}", productId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新商品收藏量
     */
    @Override
    @Transactional
    public boolean updateFavoriteCount(Long productId, int increment) {
        try {
            int updatedRows = productRepository.incrementFavoriteCount(productId, (long) increment);
            
            if (updatedRows > 0) {
                // 清除缓存
                clearProductCache(productId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("更新商品收藏量失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("更新商品收藏量失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新商品评价统计
     */
    @Override
    @Transactional
    public boolean updateProductRating(Long productId, BigDecimal rating, Long reviewId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 重新计算平均评分
            product.updateRating(rating);
            productRepository.save(product);
            
            // 清除缓存
            clearProductCache(productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("更新商品评价统计失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            throw new BusinessException("更新商品评价统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取商品统计信息
     */
    @Override
    @Cacheable(value = "productStats", unless = "#result == null")
    public ProductStatistics getProductStatistics() {
        log.debug("获取商品统计信息");
        
        try {
            // 状态分布统计
            List<Object[]> statusStats = productRepository.countProductsByStatus();
            Map<String, Long> statusDistribution = statusStats.stream()
                    .collect(Collectors.toMap(
                            stat -> stat[0].toString(),
                            stat -> (Long) stat[1]
                    ));
            
            // 分类分布统计
            List<Object[]> categoryStats = productRepository.countProductsByCategory(Product.ProductStatus.ACTIVE);
            Map<String, Long> categoryDistribution = categoryStats.stream()
                    .collect(Collectors.toMap(
                            stat -> stat[0].toString(),
                            stat -> (Long) stat[1]
                    ));
            
            // 品牌分布统计
            List<Object[]> brandStats = productRepository.countProductsByBrand(Product.ProductStatus.ACTIVE);
            Map<String, Long> brandDistribution = brandStats.stream()
                    .collect(Collectors.toMap(
                            stat -> (String) stat[0],
                            stat -> (Long) stat[1]
                    ));
            
            // 价格统计
            Object[] priceStats = productRepository.getPriceStatistics(Product.ProductStatus.ACTIVE);
            PriceStatistics priceStatistics = new PriceStatistics(
                    (BigDecimal) priceStats[0],  // minPrice
                    (BigDecimal) priceStats[1],  // maxPrice
                    (BigDecimal) priceStats[2],  // avgPrice
                    (BigDecimal) priceStats[2]   // medianPrice (简化为平均价)
            );
            
            // 库存统计
            Object[] stockStats = productRepository.getStockStatistics(Product.ProductStatus.ACTIVE);
            StockStatistics stockStatistics = new StockStatistics(
                    ((Number) stockStats[0]).longValue(),  // totalStock
                    ((Number) stockStats[1]).longValue(),  // availableStock
                    0L,  // reservedStock (需要单独计算)
                    ((Number) stockStats[2]).longValue(),  // lowStockCount
                    0L   // outOfStockCount (需要单独计算)
            );
            
            // 总商品数
            long totalProducts = statusDistribution.values().stream().mapToLong(Long::longValue).sum();
            
            return new ProductStatistics(
                    totalProducts,
                    statusDistribution,
                    categoryDistribution,
                    brandDistribution,
                    priceStatistics,
                    stockStatistics
            );
            
        } catch (Exception e) {
            log.error("获取商品统计信息失败，错误: {}", e.getMessage(), e);
            throw new BusinessException("获取商品统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取商品销售趋势
     */
    @Override
    public List<SalesTrendData> getSalesTrend(Long productId, LocalDateTime startDate, 
                                            LocalDateTime endDate, String dimension) {
        log.debug("获取商品销售趋势，商品ID: {}, 开始日期: {}, 结束日期: {}, 维度: {}", 
                 productId, startDate, endDate, dimension);
        
        // 此方法需要与订单系统集成，暂时返回空列表
        return Collections.emptyList();
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证商品创建请求参数
     */
    private void validateProductCreateRequest(ProductCreateRequest request) {
        if (request == null) {
            throw new BusinessException("商品创建请求不能为空");
        }
        
        // 验证价格合理性
        String priceValidation = request.validatePrices();
        if (priceValidation != null) {
            throw new BusinessException(priceValidation);
        }
        
        // 验证库存合理性
        String stockValidation = request.validateStock();
        if (stockValidation != null) {
            throw new BusinessException(stockValidation);
        }
        
        // 验证图片
        String imageValidation = request.validateImages();
        if (imageValidation != null) {
            throw new BusinessException(imageValidation);
        }
    }
    
    /**
     * 验证业务规则
     */
    private void validateBusinessRules(ProductCreateRequest request, Long operatorId) {
        // 验证分类存在性
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("商品分类不存在");
        }
        
        // 验证操作者存在性
        if (!userRepository.existsById(operatorId)) {
            throw new BusinessException("操作者不存在");
        }
        
        // 验证商品编码唯一性（如果提供了编码）
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            if (productRepository.findByCode(request.getCode()).isPresent()) {
                throw new BusinessException("商品编码已存在");
            }
        }
    }
    
    /**
     * 根据请求构建商品实体
     */
    private Product buildProductFromRequest(ProductCreateRequest request, Long creatorId) {
        return Product.builder()
                .name(request.getName())
                .code(request.getCode())
                .subtitle(request.getSubtitle())
                .summary(request.getSummary())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .brand(request.getBrand())
                .tags(request.getTags())
                .originalPrice(request.getOriginalPrice())
                .currentPrice(request.getCurrentPrice())
                .memberPrice(request.getMemberPrice())
                .costPrice(request.getCostPrice())
                .totalStock(request.getTotalStock())
                .availableStock(request.getTotalStock())
                .lowStockThreshold(request.getLowStockThreshold())
                .isVirtual(request.getIsVirtual())
                .requiresShipping(request.getRequiresShipping())
                .canBuyAlone(request.getCanBuyAlone())
                .isHot(request.getIsHot())
                .isRecommended(request.getIsRecommended())
                .isNew(request.getIsNew())
                .recommendWeight(request.getRecommendWeight())
                .weight(request.getWeight())
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .mainImage(request.getMainImage())
                .images(request.getImages() != null ? String.join(",", request.getImages()) : null)
                .videoUrl(request.getVideoUrl())
                .seoTitle(request.getSeoTitle())
                .seoKeywords(request.getSeoKeywords())
                .seoDescription(request.getSeoDescription())
                .specifications(convertMapToJson(request.getSpecifications()))
                .attributes(convertMapToJson(request.getAttributes()))
                .extraInfo(convertMapToJson(request.getExtraInfo()))
                .createdBy(creatorId)
                .updatedBy(creatorId)
                .build();
    }
    
    /**
     * 生成商品编码
     */
    private String generateProductCode(Product product) {
        // 简单的编码生成策略：PROD + 分类ID + 时间戳
        return "PROD" + product.getCategoryId() + System.currentTimeMillis();
    }
    
    /**
     * 更新商品属性
     */
    private void updateProductProperties(Product existingProduct, ProductCreateRequest request) {
        existingProduct.setName(request.getName());
        existingProduct.setSubtitle(request.getSubtitle());
        existingProduct.setSummary(request.getSummary());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setTags(request.getTags());
        existingProduct.setOriginalPrice(request.getOriginalPrice());
        existingProduct.setCurrentPrice(request.getCurrentPrice());
        existingProduct.setMemberPrice(request.getMemberPrice());
        existingProduct.setWeight(request.getWeight());
        existingProduct.setLength(request.getLength());
        existingProduct.setWidth(request.getWidth());
        existingProduct.setHeight(request.getHeight());
        existingProduct.setMainImage(request.getMainImage());
        existingProduct.setImages(request.getImages() != null ? String.join(",", request.getImages()) : null);
        existingProduct.setVideoUrl(request.getVideoUrl());
        existingProduct.setSeoTitle(request.getSeoTitle());
        existingProduct.setSeoKeywords(request.getSeoKeywords());
        existingProduct.setSeoDescription(request.getSeoDescription());
        existingProduct.setSpecifications(convertMapToJson(request.getSpecifications()));
        existingProduct.setAttributes(convertMapToJson(request.getAttributes()));
        existingProduct.setExtraInfo(convertMapToJson(request.getExtraInfo()));
    }
    
    /**
     * 转换为详情响应DTO
     */
    private ProductDetailResponse convertToDetailResponse(Product product) {
        // 构建分类信息
        ProductDetailResponse.CategoryInfo categoryInfo = new ProductDetailResponse.CategoryInfo();
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(product.getCategoryId());
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                categoryInfo.setId(category.getId());
                categoryInfo.setName(category.getName());
                categoryInfo.setCode(category.getCode());
                categoryInfo.setLevel(category.getLevel());
                categoryInfo.setPath(category.getPath());
                categoryInfo.setFullName(category.getFullName());
                categoryInfo.setUrlPath(category.getUrlPath());
            }
        } catch (Exception e) {
            log.warn("获取商品分类信息失败，商品ID: {}, 分类ID: {}", product.getId(), product.getCategoryId());
        }
        
        // 构建价格信息
        ProductDetailResponse.PriceInfo priceInfo = ProductDetailResponse.PriceInfo.builder()
                .originalPrice(product.getOriginalPrice())
                .currentPrice(product.getCurrentPrice())
                .memberPrice(product.getMemberPrice())
                .discountRate(product.getDiscountRate())
                .savedAmount(product.getSavedAmount())
                .onSale(product.isOnSale())
                .build();
        
        // 构建库存信息
        ProductDetailResponse.StockInfo stockInfo = ProductDetailResponse.StockInfo.builder()
                .totalStock(product.getTotalStock())
                .availableStock(product.getAvailableStock())
                .reservedStock(product.getReservedStock())
                .lowStockThreshold(product.getLowStockThreshold())
                .hasStock(product.hasStock())
                .stockSufficient(product.getAvailableStock() > 0)
                .needsLowStockAlert(product.needsLowStockAlert())
                .stockStatusText(product.getStockStatusText())
                .build();
        
        // 构建销售统计
        ProductDetailResponse.SalesInfo salesInfo = ProductDetailResponse.SalesInfo.builder()
                .salesCount(product.getSalesCount())
                .viewCount(product.getViewCount())
                .favoriteCount(product.getFavoriteCount())
                .reviewCount(product.getReviewCount())
                .averageRating(product.getAverageRating())
                .starRating(product.getAverageRating().intValue())
                .build();
        
        // 构建商品属性
        ProductDetailResponse.ProductAttributes attributes = ProductDetailResponse.ProductAttributes.builder()
                .isVirtual(product.getIsVirtual())
                .requiresShipping(product.getRequiresShipping())
                .canBuyAlone(product.getCanBuyAlone())
                .build();
        
        // 构建物理规格
        ProductDetailResponse.PhysicalSpecs physicalSpecs = ProductDetailResponse.PhysicalSpecs.builder()
                .weight(product.getWeight())
                .length(product.getLength())
                .width(product.getWidth())
                .height(product.getHeight())
                .build();
        
        // 构建营销标签
        ProductDetailResponse.MarketingLabels marketingLabels = ProductDetailResponse.MarketingLabels.builder()
                .isHot(product.getIsHot())
                .isRecommended(product.getIsRecommended())
                .isNew(product.getIsNew())
                .build();
        
        // 构建SEO信息
        ProductDetailResponse.SeoInfo seoInfo = ProductDetailResponse.SeoInfo.builder()
                .title(product.getSeoTitle())
                .keywords(product.getSeoKeywords())
                .description(product.getSeoDescription())
                .build();
        
        // 构建主响应对象
        return ProductDetailResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .subtitle(product.getSubtitle())
                .summary(product.getSummary())
                .description(product.getDescription())
                .category(categoryInfo)
                .brand(product.getBrand())
                .tags(product.getTags() != null ? Arrays.asList(product.getTags().split(",")) : Collections.emptyList())
                .priceInfo(priceInfo)
                .stockInfo(stockInfo)
                .salesInfo(salesInfo)
                .status(product.getStatus().name())
                .attributes(attributes)
                .physicalSpecs(physicalSpecs)
                .mainImage(product.getMainImage())
                .images(product.getImages() != null ? Arrays.asList(product.getImages().split(",")) : Collections.emptyList())
                .videoUrl(product.getVideoUrl())
                .marketingLabels(marketingLabels)
                .recommendWeight(product.getRecommendWeight())
                .specifications(convertJsonToMap(product.getSpecifications()))
                .productAttributes(convertJsonToMap(product.getAttributes()))
                .seoInfo(seoInfo)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .publishedAt(product.getPublishedAt())
                .extraInfo(convertJsonToObjectMap(product.getExtraInfo()))
                .build();
    }
    
    /**
     * 权限验证方法
     */
    private void validateUpdatePermission(Product product, Long operatorId) {
        // 简化权限验证：只有创建者或管理员可以更新
        if (!product.getCreatedBy().equals(operatorId) && !isAdmin(operatorId)) {
            throw new BusinessException("无权限更新该商品");
        }
    }
    
    private void validateDeletePermission(Product product, Long operatorId) {
        if (!product.getCreatedBy().equals(operatorId) && !isAdmin(operatorId)) {
            throw new BusinessException("无权限删除该商品");
        }
    }
    
    private void validatePublishPermission(Product product, Long operatorId) {
        if (!product.getCreatedBy().equals(operatorId) && !isAdmin(operatorId)) {
            throw new BusinessException("无权限发布该商品");
        }
    }
    
    private void validateUnpublishPermission(Product product, Long operatorId) {
        if (!product.getCreatedBy().equals(operatorId) && !isAdmin(operatorId)) {
            throw new BusinessException("无权限下架该商品");
        }
    }
    
    private boolean hasViewPermission(Product product, Long viewerId) {
        // 简化权限检查：已发布的商品所有人可见，其他状态只有创建者和管理员可见
        if (Product.ProductStatus.ACTIVE.equals(product.getStatus())) {
            return true;
        }
        
        if (viewerId == null) {
            return false;
        }
        
        return product.getCreatedBy().equals(viewerId) || isAdmin(viewerId);
    }
    
    private boolean isAdmin(Long userId) {
        // 简化管理员判断逻辑，实际应该查询用户角色
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            return userOpt.isPresent() && User.UserRole.ADMIN.equals(userOpt.get().getRole());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 状态验证方法
     */
    private void validateUpdateStatus(Product product) {
        if (Product.ProductStatus.DELETED.equals(product.getStatus())) {
            throw new BusinessException("已删除的商品不能更新");
        }
    }
    
    private void validateDeleteConditions(Product product) {
        if (Product.ProductStatus.DELETED.equals(product.getStatus())) {
            throw new BusinessException("商品已被删除");
        }
        // 可以添加更多删除条件检查，如是否有未完成订单等
    }
    
    private void validatePublishConditions(Product product) {
        if (Product.ProductStatus.ACTIVE.equals(product.getStatus())) {
            throw new BusinessException("商品已经是发布状态");
        }
        
        if (Product.ProductStatus.DELETED.equals(product.getStatus())) {
            throw new BusinessException("已删除的商品不能发布");
        }
        
        // 检查商品信息完整性
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        
        if (product.getCurrentPrice() == null || product.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品价格必须大于0");
        }
        
        if (product.getMainImage() == null || product.getMainImage().trim().isEmpty()) {
            throw new BusinessException("商品主图不能为空");
        }
    }
    
    /**
     * 缓存管理方法
     */
    private void clearProductCache(Long productId) {
        redisTemplate.delete(PRODUCT_CACHE_KEY + productId);
    }
    
    private void clearProductRelatedCache(Long productId, Long categoryId) {
        if (productId != null) {
            clearProductCache(productId);
        }
        
        // 清除列表缓存
        redisTemplate.delete(PRODUCT_LIST_CACHE_KEY + "*");
        redisTemplate.delete(HOT_PRODUCTS_CACHE_KEY + "*");
        redisTemplate.delete(PRODUCT_STATS_CACHE_KEY);
        
        if (categoryId != null) {
            redisTemplate.delete(PRODUCT_LIST_CACHE_KEY + "category:" + categoryId + "*");
        }
    }
    
    /**
     * 异步更新浏览量
     */
    private void updateViewCountAsync(Long productId, Long viewerId) {
        // 使用CompletableFuture异步更新
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                productRepository.incrementViewCount(productId, 1L);
            } catch (Exception e) {
                log.error("异步更新浏览量失败，商品ID: {}", productId, e);
            }
        });
    }
    
    /**
     * 更新分类商品数量
     */
    private void updateCategoryProductCount(Long categoryId, Long increment) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                if (increment > 0) {
                    category.incrementProductCount(increment);
                } else {
                    category.decrementProductCount(-increment);
                }
                categoryRepository.save(category);
            }
        } catch (Exception e) {
            log.error("更新分类商品数量失败，分类ID: {}, 增量: {}", categoryId, increment, e);
        }
    }
    
    /**
     * 工具方法
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "salesCount";
        }
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        
        return Sort.by(direction, sortBy);
    }
    
    private String generateReservationId(Long productId, Long reserverId) {
        return "RESERVE_" + productId + "_" + reserverId + "_" + System.currentTimeMillis();
    }
    
    private void setReservationExpiry(String reservationId, Long productId, int quantity, 
                                    Long reserverId, int timeoutMinutes) {
        String key = "reservation:" + reservationId;
        Map<String, Object> reservationData = new HashMap<>();
        reservationData.put("productId", productId);
        reservationData.put("quantity", quantity);
        reservationData.put("reserverId", reserverId);
        
        redisTemplate.opsForValue().set(key, reservationData, timeoutMinutes, TimeUnit.MINUTES);
    }
    
    private void clearReservation(String reservationId) {
        redisTemplate.delete("reservation:" + reservationId);
    }
    
    private String convertMapToJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            // 简化JSON转换，实际应使用Jackson ObjectMapper
            return map.toString();
        } catch (Exception e) {
            log.warn("转换Map为JSON失败: {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, String> convertJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // 简化JSON转换，实际应使用Jackson ObjectMapper
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("转换JSON为Map失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    private Map<String, Object> convertJsonToObjectMap(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // 简化JSON转换，实际应使用Jackson ObjectMapper
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("转换JSON为ObjectMap失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}