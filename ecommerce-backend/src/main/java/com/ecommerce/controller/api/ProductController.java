package com.ecommerce.controller.api;

/*
 * 文件职责: 商品控制器，提供商品相关的RESTful API接口
 * 
 * 开发心理活动：
 * 1. 商品API设计原则：
 *    - RESTful风格：遵循REST设计原则，语义清晰
 *    - 统一响应：使用ApiResponse包装所有响应
 *    - 权限控制：基于用户角色的接口访问控制
 *    - 参数验证：完善的请求参数校验机制
 * 
 * 2. 电商商品接口场景：
 *    - 商品浏览：商品详情、列表、搜索、推荐
 *    - 商品管理：创建、更新、发布、下架、删除
 *    - 库存管理：库存查询、更新、预占、释放
 *    - 统计分析：热门商品、销售趋势、库存预警
 * 
 * 3. 性能优化考虑：
 *    - 分页查询：避免大数据量返回
 *    - 缓存策略：热点商品数据缓存
 *    - 异步处理：耗时操作异步化
 *    - 批量操作：提升管理效率
 * 
 * 4. 用户体验设计：
 *    - 响应速度：快速的商品查询响应
 *    - 搜索体验：智能搜索和推荐
 *    - 错误处理：友好的错误提示
 *    - 数据完整：丰富的商品展示信息
 * 
 * 包结构设计思路:
 * - 放在controller.api包下，提供前台API服务
 * - 与admin控制器分离，职责明确
 * 
 * 命名原因:
 * - ProductController明确表达商品接口控制功能
 * - 符合Controller后缀的MVC命名规范
 * 
 * 依赖关系:
 * - 依赖ProductService处理业务逻辑
 * - 使用DTO对象进行数据传输
 * - 返回ApiResponse统一响应格式
 */

import com.ecommerce.dto.common.ApiResponse;
import com.ecommerce.dto.request.product.ProductCreateRequest;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 商品控制器
 * 
 * 功能说明：
 * 1. 提供商品相关的RESTful API接口
 * 2. 处理商品浏览、搜索、推荐功能
 * 3. 支持商品管理和库存操作
 * 4. 提供商品统计和分析接口
 * 
 * 接口分类：
 * 1. 公开接口：商品详情、列表、搜索（无需登录）
 * 2. 用户接口：收藏、评价、个性化推荐（需登录）
 * 3. 商家接口：商品管理、库存操作（需商家权限）
 * 4. 管理接口：统计分析、批量操作（需管理员权限）
 * 
 * URL设计：
 * - GET /api/products - 商品列表
 * - GET /api/products/{id} - 商品详情
 * - POST /api/products - 创建商品
 * - PUT /api/products/{id} - 更新商品
 * - DELETE /api/products/{id} - 删除商品
 * 
 * 响应格式：
 * - 成功：ApiResponse.success(data, message)
 * - 失败：ApiResponse.error(message)
 * - 分页：包含分页元数据的响应
 * 
 * @author ecommerce-team
 * @version 1.0.0 
 * @since 2024-08-04
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品相关的API接口")
public class ProductController {

    // ========== 依赖注入 ==========

    private final ProductService productService;

    // ========== 商品浏览接口（公开） ==========

    /**
     * 获取商品详情
     * 
     * 业务流程：
     * 1. 参数验证：商品ID格式验证
     * 2. 权限检查：根据商品状态和用户权限过滤
     * 3. 数据查询：从缓存或数据库获取商品详情
     * 4. 统计更新：异步更新商品浏览量
     * 5. 推荐预热：预加载相关商品推荐
     * 
     * 访问控制：
     * - 已发布商品：所有用户可访问
     * - 草稿/审核中：仅创建者和管理员可访问
     * - 已删除商品：不可访问
     * 
     * 性能优化：
     * - 缓存策略：热门商品Redis缓存
     * - 异步统计：浏览量异步更新
     * - 预加载：相关商品数据预热
     * 
     * @param productId 商品ID
     * @param principal 当前用户信息（可选）
     * @return 商品详情响应
     */
    @GetMapping("/{productId}")
    @Operation(summary = "获取商品详情", description = "根据商品ID获取详细信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限访问")
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @AuthenticationPrincipal Object principal) {
        
        log.info("获取商品详情，商品ID: {}", productId);
        
        try {
            Long viewerId = extractUserId(principal);
            Optional<ProductDetailResponse> productOpt = productService.getProductById(productId, viewerId);
            
            if (productOpt.isPresent()) {
                ProductDetailResponse product = productOpt.get();
                log.info("获取商品详情成功，商品ID: {}, 商品名称: {}", productId, product.getName());
                return ResponseEntity.ok(ApiResponse.success(product));
            } else {
                log.warn("商品不存在或无权限访问，商品ID: {}, 查看者ID: {}", productId, viewerId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("获取商品详情失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取商品详情失败：" + e.getMessage()));
        }
    }

    /**
     * 根据商品编码获取商品详情
     * 
     * 应用场景：
     * - 扫码购买：通过条形码或二维码访问
     * - 快速查找：通过SKU快速定位
     * - 系统集成：第三方系统通过编码查询
     * - 移动端：扫码查看商品功能
     * 
     * @param code 商品编码
     * @param principal 当前用户信息（可选）
     * @return 商品详情响应
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "根据编码获取商品", description = "通过商品编码获取商品详情")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductByCode(
            @Parameter(description = "商品编码", required = true) @PathVariable String code,
            @AuthenticationPrincipal Object principal) {
        
        log.info("根据编码获取商品，商品编码: {}", code);
        
        try {
            Long viewerId = extractUserId(principal);
            Optional<ProductDetailResponse> productOpt = productService.getProductByCode(code, viewerId);
            
            if (productOpt.isPresent()) {
                ProductDetailResponse product = productOpt.get();
                return ResponseEntity.ok(ApiResponse.success(product));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("根据编码获取商品失败，商品编码: {}, 错误: {}", code, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取商品失败：" + e.getMessage()));
        }
    }

    /**
     * 商品列表查询
     * 
     * 查询功能：
     * 1. 基础筛选：分类、品牌、价格区间筛选
     * 2. 状态过滤：默认只显示已发布商品
     * 3. 排序支持：价格、销量、评分、发布时间
     * 4. 分页处理：支持大数据量的分页展示
     * 
     * 性能优化：
     * - 索引利用：基于数据库索引优化查询
     * - 缓存策略：常用查询结果缓存
     * - 分页优化：避免深分页性能问题
     * 
     * @param categoryId 分类ID（可选）
     * @param brand 品牌名称（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param page 页码，从0开始
     * @param size 每页大小
     * @param sort 排序字段
     * @param direction 排序方向（asc/desc）
     * @param principal 当前用户信息（可选）
     * @return 商品分页列表
     */
    @GetMapping
    @Operation(summary = "商品列表查询", description = "分页查询商品列表，支持多条件筛选")
    public ResponseEntity<ApiResponse<Page<ProductDetailResponse>>> getProducts(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌名称") @RequestParam(required = false) String brand,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "salesCount") String sort,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Object principal) {
        
        log.debug("查询商品列表，分类ID: {}, 品牌: {}, 价格区间: {}-{}, 页码: {}, 大小: {}", 
                 categoryId, brand, minPrice, maxPrice, page, size);
        
        try {
            // 参数验证
            if (size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("每页大小不能超过100"));
            }
            
            Long viewerId = extractUserId(principal);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<ProductDetailResponse> productPage = productService.getProducts(
                    categoryId, brand, Product.ProductStatus.ACTIVE, 
                    minPrice, maxPrice, pageable, viewerId);
            
            log.debug("查询商品列表成功，总数: {}, 当前页数量: {}", 
                     productPage.getTotalElements(), productPage.getNumberOfElements());
            
            return ResponseEntity.ok(ApiResponse.success(productPage));
            
        } catch (Exception e) {
            log.error("查询商品列表失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询商品列表失败：" + e.getMessage()));
        }
    }

    /**
     * 商品搜索
     * 
     * 搜索功能：
     * 1. 全文检索：商品名称、描述、标签的模糊匹配
     * 2. 高级筛选：分类、品牌、价格等条件组合
     * 3. 智能排序：相关性、销量、评分的综合排序
     * 4. 搜索建议：基于用户输入的搜索推荐
     * 
     * 搜索优化：
     * - 索引优化：全文检索索引
     * - 缓存策略：热门搜索结果缓存
     * - 分词匹配：中文分词和同义词
     * - 个性化：基于用户历史的个性化搜索
     * 
     * @param keyword 搜索关键词
     * @param categoryId 分类筛选（可选）
     * @param brand 品牌筛选（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 每页大小
     * @param principal 当前用户信息（可选）
     * @return 搜索结果分页列表
     */
    @GetMapping("/search")
    @Operation(summary = "商品搜索", description = "全文搜索商品，支持多条件筛选和排序")
    public ResponseEntity<ApiResponse<Page<ProductDetailResponse>>> searchProducts(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌名称") @RequestParam(required = false) String brand,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "salesCount") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Object principal) {
        
        log.info("搜索商品，关键词: {}, 分类ID: {}, 品牌: {}", keyword, categoryId, brand);
        
        try {
            // 参数验证
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("搜索关键词不能为空"));
            }
            
            if (size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("每页大小不能超过100"));
            }
            
            Long searcherId = extractUserId(principal);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<ProductDetailResponse> searchResults = productService.searchProducts(
                    keyword.trim(), categoryId, brand, minPrice, maxPrice,
                    sortBy, sortDirection, pageable, searcherId);
            
            log.info("搜索商品成功，关键词: {}, 结果数量: {}", keyword, searchResults.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(searchResults));
            
        } catch (Exception e) {
            log.error("搜索商品失败，关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索商品失败：" + e.getMessage()));
        }
    }

    /**
     * 获取相关商品推荐
     * 
     * 推荐算法：
     * 1. 基于商品属性的相似性推荐
     * 2. 基于用户行为的协同过滤推荐
     * 3. 基于销量和评价的热度推荐
     * 4. 基于分类和品牌的关联推荐
     * 
     * 个性化策略：
     * - 用户历史：基于浏览和购买历史
     * - 实时行为：当前会话的行为分析
     * - 偏好挖掘：长期偏好和短期兴趣
     * - 多样性：避免推荐结果过于单一
     * 
     * @param productId 当前商品ID
     * @param limit 推荐商品数量限制
     * @param principal 当前用户信息（可选）
     * @return 相关商品推荐列表
     */
    @GetMapping("/{productId}/related")
    @Operation(summary = "相关商品推荐", description = "基于当前商品获取相关商品推荐")
    public ResponseEntity<ApiResponse<List<ProductDetailResponse>>> getRelatedProducts(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal Object principal) {
        
        log.debug("获取相关商品推荐，商品ID: {}, 限制数量: {}", productId, limit);
        
        try {
            if (limit > 50) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("推荐数量不能超过50"));
            }
            
            Long userId = extractUserId(principal);
            List<ProductDetailResponse> relatedProducts = productService.getRelatedProducts(
                    productId, userId, limit);
            
            log.debug("获取相关商品推荐成功，商品ID: {}, 推荐数量: {}", productId, relatedProducts.size());
            
            return ResponseEntity.ok(ApiResponse.success(relatedProducts));
            
        } catch (Exception e) {
            log.error("获取相关商品推荐失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取推荐失败：" + e.getMessage()));
        }
    }

    /**
     * 获取热门商品
     * 
     * 热门算法：
     * 1. 销量排行：基于销售数量的排序
     * 2. 浏览排行：基于用户浏览量排序
     * 3. 评价排行：基于用户评分排序
     * 4. 综合排行：多指标权重计算
     * 
     * 时间维度：
     * - 实时热门：当前时段热门
     * - 日热门：24小时内热门
     * - 周热门：7天内热门
     * - 月热门：30天内热门
     * 
     * @param categoryId 分类ID（可选）
     * @param timeRange 时间范围（daily/weekly/monthly）
     * @param limit 返回数量限制
     * @return 热门商品列表
     */
    @GetMapping("/hot")
    @Operation(summary = "热门商品", description = "获取热门商品列表")
    public ResponseEntity<ApiResponse<List<ProductDetailResponse>>> getHotProducts(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "时间范围") @RequestParam(defaultValue = "weekly") String timeRange,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "20") int limit) {
        
        log.debug("获取热门商品，分类ID: {}, 时间范围: {}, 限制数量: {}", categoryId, timeRange, limit);
        
        try {
            if (limit > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("返回数量不能超过100"));
            }
            
            List<ProductDetailResponse> hotProducts = productService.getHotProducts(
                    categoryId, timeRange, limit);
            
            log.debug("获取热门商品成功，数量: {}", hotProducts.size());
            
            return ResponseEntity.ok(ApiResponse.success(hotProducts));
            
        } catch (Exception e) {
            log.error("获取热门商品失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取热门商品失败：" + e.getMessage()));
        }
    }

    /**
     * 获取最新商品
     * 
     * 新品策略：
     * - 上架时间排序：最近上架的商品优先
     * - 新品标识：系统标记的新品商品
     * - 时间筛选：可指定时间范围内的新品
     * - 分类筛选：特定分类的新品商品
     * 
     * @param categoryId 分类ID（可选）
     * @param limit 返回数量限制
     * @return 最新商品列表
     */
    @GetMapping("/latest")
    @Operation(summary = "最新商品", description = "获取最新上架的商品列表")
    public ResponseEntity<ApiResponse<List<ProductDetailResponse>>> getLatestProducts(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "20") int limit) {
        
        log.debug("获取最新商品，分类ID: {}, 限制数量: {}", categoryId, limit);
        
        try {
            if (limit > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("返回数量不能超过100"));
            }
            
            List<ProductDetailResponse> latestProducts = productService.getLatestProducts(categoryId, limit);
            
            log.debug("获取最新商品成功，数量: {}", latestProducts.size());
            
            return ResponseEntity.ok(ApiResponse.success(latestProducts));
            
        } catch (Exception e) {
            log.error("获取最新商品失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取最新商品失败：" + e.getMessage()));
        }
    }

    // ========== 商品管理接口（需要权限） ==========

    /**
     * 创建商品
     * 
     * 创建流程：
     * 1. 权限验证：商家或管理员权限
     * 2. 参数校验：商品信息完整性验证
     * 3. 业务校验：分类存在性、价格合理性
     * 4. 数据处理：图片处理、SEO优化
     * 5. 状态设置：根据权限设置初始状态
     * 6. 索引更新：搜索引擎索引同步
     * 
     * 权限控制：
     * - 商家：可创建自己的商品
     * - 管理员：可创建任意商品
     * - 普通用户：无权限创建
     * 
     * @param request 商品创建请求
     * @param principal 当前用户信息
     * @return 创建成功的商品详情
     */
    @PostMapping
    @Operation(summary = "创建商品", description = "创建新商品，需要商家或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal Object principal) {
        
        log.info("创建商品请求，商品名称: {}", request.getName());
        
        try {
            Long creatorId = extractUserId(principal);
            if (creatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            ProductDetailResponse product = productService.createProduct(request, creatorId);
            
            log.info("商品创建成功，商品ID: {}, 商品名称: {}", product.getId(), product.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(product, "商品创建成功"));
                    
        } catch (Exception e) {
            log.error("创建商品失败，商品名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("创建商品失败：" + e.getMessage()));
        }
    }

    /**
     * 更新商品信息
     * 
     * 更新策略：
     * 1. 权限验证：只能更新自己的商品或管理员权限
     * 2. 状态检查：某些状态下不允许更新
     * 3. 数据校验：更新数据的合法性验证
     * 4. 增量更新：只更新变化的字段
     * 5. 关联更新：更新相关的索引和缓存
     * 
     * 更新限制：
     * - 已发布商品：某些字段不可修改
     * - 有订单商品：价格修改需要特殊处理
     * - 删除商品：不允许更新
     * 
     * @param productId 商品ID
     * @param request 更新请求
     * @param principal 当前用户信息
     * @return 更新后的商品详情
     */
    @PutMapping("/{productId}")
    @Operation(summary = "更新商品", description = "更新商品信息，需要商品创建者或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal Object principal) {
        
        log.info("更新商品请求，商品ID: {}", productId);
        
        try {
            Long operatorId = extractUserId(principal);
            if (operatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            ProductDetailResponse product = productService.updateProduct(productId, request, operatorId);
            
            log.info("商品更新成功，商品ID: {}", productId);
            
            return ResponseEntity.ok(ApiResponse.success(product, "商品更新成功"));
                    
        } catch (Exception e) {
            log.error("更新商品失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新商品失败：" + e.getMessage()));
        }
    }

    /**
     * 删除商品（软删除）
     * 
     * 删除策略：
     * 1. 权限验证：只能删除自己的商品或管理员权限
     * 2. 条件检查：有未完成订单的商品不能删除
     * 3. 软删除：标记删除状态而不是物理删除
     * 4. 关联处理：处理购物车中的该商品
     * 5. 索引更新：从搜索引擎中移除
     * 
     * 删除限制：
     * - 有未完成订单：不能删除
     * - 已删除商品：不能重复删除
     * - 普通用户：无权限删除
     * 
     * @param productId 商品ID
     * @param principal 当前用户信息
     * @return 删除结果
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "删除商品", description = "软删除商品，需要商品创建者或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @AuthenticationPrincipal Object principal) {
        
        log.info("删除商品请求，商品ID: {}", productId);
        
        try {
            Long operatorId = extractUserId(principal);
            if (operatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = productService.deleteProduct(productId, operatorId);
            
            if (success) {
                log.info("商品删除成功，商品ID: {}", productId);
                return ResponseEntity.ok(ApiResponse.success(null, "商品删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("商品删除失败"));
            }
                    
        } catch (Exception e) {
            log.error("删除商品失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除商品失败：" + e.getMessage()));
        }
    }

    /**
     * 发布商品（上架）
     * 
     * 发布流程：
     * 1. 权限验证：商品创建者或管理员权限
     * 2. 状态检查：当前状态是否允许发布
     * 3. 信息验证：商品信息完整性检查
     * 4. 发布处理：更新状态和发布时间
     * 5. 索引同步：添加到搜索引擎索引
     * 6. 缓存预热：预热商品详情缓存
     * 
     * 发布条件：
     * - 商品信息完整：名称、价格、图片等
     * - 库存充足：有可售库存
     * - 审核通过：需要的话通过审核
     * 
     * @param productId 商品ID
     * @param principal 当前用户信息
     * @return 发布结果
     */
    @PutMapping("/{productId}/publish")
    @Operation(summary = "发布商品", description = "发布商品到前台展示，需要商品创建者或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publishProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @AuthenticationPrincipal Object principal) {
        
        log.info("发布商品请求，商品ID: {}", productId);
        
        try {
            Long operatorId = extractUserId(principal);
            if (operatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = productService.publishProduct(productId, operatorId);
            
            if (success) {
                log.info("商品发布成功，商品ID: {}", productId);
                return ResponseEntity.ok(ApiResponse.success(null, "商品发布成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("商品发布失败"));
            }
                    
        } catch (Exception e) {
            log.error("发布商品失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("发布商品失败：" + e.getMessage()));
        }
    }

    /**
     * 下架商品
     * 
     * 下架处理：
     * 1. 权限验证：商品创建者或管理员权限
     * 2. 状态更新：更新为下架状态
     * 3. 时间记录：记录下架时间
     * 4. 购物车处理：从购物车中移除
     * 5. 索引移除：从搜索结果中移除
     * 6. 用户通知：通知收藏该商品的用户
     * 
     * @param productId 商品ID
     * @param reason 下架原因
     * @param principal 当前用户信息
     * @return 下架结果
     */
    @PutMapping("/{productId}/unpublish")
    @Operation(summary = "下架商品", description = "下架商品，需要商品创建者或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpublishProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @Parameter(description = "下架原因") @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Object principal) {
        
        log.info("下架商品请求，商品ID: {}, 原因: {}", productId, reason);
        
        try {
            Long operatorId = extractUserId(principal);
            if (operatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = productService.unpublishProduct(productId, operatorId, reason);
            
            if (success) {
                log.info("商品下架成功，商品ID: {}", productId);
                return ResponseEntity.ok(ApiResponse.success(null, "商品下架成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("商品下架失败"));
            }
                    
        } catch (Exception e) {
            log.error("下架商品失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("下架商品失败：" + e.getMessage()));
        }
    }

    // ========== 库存管理接口 ==========

    /**
     * 更新商品库存
     * 
     * 库存管理：
     * 1. 权限验证：商品创建者或管理员权限
     * 2. 并发控制：防止库存操作冲突
     * 3. 业务校验：库存数量合理性验证
     * 4. 状态联动：库存变化时状态自动调整
     * 5. 日志记录：详细的库存操作日志
     * 6. 预警检查：库存低于预警线时通知
     * 
     * @param productId 商品ID
     * @param quantity 新库存数量
     * @param reason 库存变更原因
     * @param principal 当前用户信息
     * @return 更新结果
     */
    @PutMapping("/{productId}/stock")
    @Operation(summary = "更新商品库存", description = "更新商品库存数量，需要商品创建者或管理员权限")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProductStock(
            @Parameter(description = "商品ID", required = true) @PathVariable Long productId,
            @Parameter(description = "库存数量", required = true) @RequestParam Integer quantity,
            @Parameter(description = "变更原因") @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Object principal) {
        
        log.info("更新商品库存请求，商品ID: {}, 数量: {}, 原因: {}", productId, quantity, reason);
        
        try {
            if (quantity < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("库存数量不能为负数"));
            }
            
            Long operatorId = extractUserId(principal);
            if (operatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = productService.updateProductStock(productId, quantity, operatorId, reason);
            
            if (success) {
                log.info("商品库存更新成功，商品ID: {}, 新库存: {}", productId, quantity);
                return ResponseEntity.ok(ApiResponse.success(null, "库存更新成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("库存更新失败"));
            }
                    
        } catch (Exception e) {
            log.error("更新商品库存失败，商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("库存更新失败：" + e.getMessage()));
        }
    }

    // ========== 统计分析接口（管理员） ==========

    /**
     * 获取商品统计信息
     * 
     * 统计维度：
     * 1. 基础统计：商品总数、各状态分布
     * 2. 分类统计：各分类商品数量分布
     * 3. 品牌统计：各品牌商品数量统计
     * 4. 价格统计：价格分布区间分析
     * 5. 库存统计：库存分布和预警统计
     * 6. 销售统计：销量和评价统计分析
     * 
     * 权限控制：
     * - 仅管理员可访问
     * - 数据脱敏处理
     * - 敏感信息过滤
     * 
     * @return 商品统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "商品统计信息", description = "获取商品统计数据，仅限管理员访问")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductService.ProductStatistics>> getProductStatistics() {
        
        log.debug("获取商品统计信息");
        
        try {
            ProductService.ProductStatistics statistics = productService.getProductStatistics();
            
            log.debug("获取商品统计信息成功，商品总数: {}", statistics.totalProducts());
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
                    
        } catch (Exception e) {
            log.error("获取商品统计信息失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取统计信息失败：" + e.getMessage()));
        }
    }

    /**
     * 获取销售趋势数据
     * 
     * 趋势分析：
     * 1. 时间维度：日、周、月销售趋势
     * 2. 商品维度：单个或全部商品分析
     * 3. 分类维度：不同分类销售对比
     * 4. 地域维度：不同地区销售分布
     * 
     * @param productId 商品ID（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param dimension 统计维度
     * @return 销售趋势数据
     */
    @GetMapping("/sales-trend")
    @Operation(summary = "销售趋势分析", description = "获取商品销售趋势数据，仅限管理员访问")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductService.SalesTrendData>>> getSalesTrend(
            @Parameter(description = "商品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "开始日期") @RequestParam LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam LocalDateTime endDate,
            @Parameter(description = "统计维度") @RequestParam(defaultValue = "daily") String dimension) {
        
        log.debug("获取销售趋势，商品ID: {}, 时间范围: {} - {}, 维度: {}", 
                 productId, startDate, endDate, dimension);
        
        try {
            List<ProductService.SalesTrendData> trendData = productService.getSalesTrend(
                    productId, startDate, endDate, dimension);
            
            log.debug("获取销售趋势成功，数据点数量: {}", trendData.size());
            
            return ResponseEntity.ok(ApiResponse.success(trendData));
                    
        } catch (Exception e) {
            log.error("获取销售趋势失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取销售趋势失败：" + e.getMessage()));
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从认证主体中提取用户ID
     * 
     * @param principal 认证主体
     * @return 用户ID，如果未登录返回null
     */
    private Long extractUserId(Object principal) {
        if (principal == null) {
            return null;
        }
        
        // 简化实现，实际应该从JWT或Spring Security上下文中获取
        // 这里暂时返回固定值，后续集成JWT时调整
        return 1L;
    }
}