package com.ecommerce.controller.api;

/*
 * 文件职责: 订单控制器，提供订单相关的RESTful API接口
 * 
 * 开发心理活动：
 * 1. 订单API设计原则：
 *    - RESTful风格：遵循REST设计规范，语义清晰
 *    - 状态安全：确保订单状态变更的安全性
 *    - 权限控制：严格的用户权限和数据隔离
 *    - 异常处理：完善的错误处理和用户提示
 * 
 * 2. 电商订单接口场景：
 *    - 订单创建：用户下单和参数验证
 *    - 订单查询：订单详情和列表查询
 *    - 订单管理：取消、删除、状态变更
 *    - 支付管理：支付发起和回调处理
 *    - 订单履行：发货、收货、完成流程
 *    - 售后服务：退货、退款、换货处理
 * 
 * 3. 接口安全考虑：
 *    - 参数验证：严格的请求参数校验
 *    - 权限控制：用户只能操作自己的订单
 *    - 状态验证：订单状态流转的业务验证
 *    - 防重复：重复请求的幂等性处理
 * 
 * 4. 用户体验设计：
 *    - 响应速度：快速的订单操作响应
 *    - 错误提示：友好的错误信息提示
 *    - 数据完整：丰富的订单展示信息
 *    - 状态同步：实时的订单状态同步
 * 
 * 包结构设计思路:
 * - 放在controller.api包下，提供前台用户API
 * - 与admin控制器分离，职责明确
 * 
 * 命名原因:
 * - OrderController明确表达订单接口控制功能
 * - 符合Controller后缀的MVC命名规范
 * 
 * 依赖关系:
 * - 依赖OrderService处理业务逻辑
 * - 使用DTO对象进行数据传输
 * - 返回ApiResponse统一响应格式
 */

import com.ecommerce.dto.common.ApiResponse;
import com.ecommerce.dto.request.order.OrderCreateRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.service.OrderService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单控制器
 * 
 * 功能说明：
 * 1. 提供订单相关的RESTful API接口
 * 2. 处理订单创建、查询、管理功能
 * 3. 支持订单支付和履行操作
 * 4. 提供订单统计和分析接口
 * 
 * 接口分类：
 * 1. 订单创建：下单、参数验证、库存检查
 * 2. 订单查询：详情查询、列表查询、状态统计
 * 3. 订单管理：取消、删除、状态变更
 * 4. 支付管理：支付发起、回调处理、退款操作
 * 5. 订单履行：发货、收货、完成流程
 * 6. 售后服务：退货、退款、换货申请
 * 7. 统计分析：订单统计、趋势分析、排行榜
 * 
 * URL设计：
 * - POST /api/orders - 创建订单
 * - GET /api/orders/{id} - 获取订单详情
 * - GET /api/orders - 查询订单列表
 * - PUT /api/orders/{id}/cancel - 取消订单
 * - POST /api/orders/{id}/pay - 订单支付
 * - PUT /api/orders/{id}/confirm-delivery - 确认收货
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关的API接口")
public class OrderController {

    // ========== 依赖注入 ==========

    private final OrderService orderService;

    // ========== 订单创建接口 ==========

    /**
     * 创建订单
     * 
     * 业务流程：
     * 1. 参数验证：严格验证订单创建请求参数
     * 2. 用户验证：验证用户登录状态和权限
     * 3. 商品验证：验证商品存在性和库存状态
     * 4. 价格校验：校验前端计算的价格准确性
     * 5. 库存预占：原子性预占商品库存
     * 6. 订单创建：创建订单主记录和明细记录
     * 7. 支付准备：生成支付参数和链接
     * 8. 响应返回：返回订单信息和支付参数
     * 
     * 安全控制：
     * - 用户认证：必须登录才能下单
     * - 参数验证：防止恶意参数和SQL注入
     * - 价格校验：防止前端价格被篡改
     * - 重复控制：防止重复提交订单
     * 
     * @param request 订单创建请求
     * @param principal 当前登录用户
     * @return 订单创建结果
     */
    @PostMapping
    @Operation(summary = "创建订单", description = "用户下单，创建新订单")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "订单创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未登录"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "库存不足")
    })
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.OrderCreationResult>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户下单请求，商品数量: {}, 实付金额: {}", 
                request.getOrderItems().size(), request.getActualAmount());
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            OrderService.OrderCreationResult result = orderService.createOrder(request, userId);
            
            if (result.success()) {
                log.info("订单创建成功，订单号: {}, 用户ID: {}", result.orderNo(), userId);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(result, "订单创建成功"));
            } else {
                log.warn("订单创建失败，用户ID: {}, 错误: {}", userId, result.message());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.message()));
            }
            
        } catch (Exception e) {
            log.error("创建订单异常，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建订单失败：" + e.getMessage()));
        }
    }

    // ========== 订单查询接口 ==========

    /**
     * 获取订单详情
     * 
     * 查询逻辑：
     * 1. 权限验证：用户只能查看自己的订单
     * 2. 数据查询：从缓存或数据库获取订单详情
     * 3. 信息完整：包含订单项、支付、物流等信息
     * 4. 状态计算：计算订单当前可执行的操作
     * 5. 数据脱敏：过滤敏感信息
     * 
     * @param orderId 订单ID
     * @param principal 当前用户
     * @return 订单详情
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详细信息")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.OrderDetailResponse>> getOrderDetail(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @AuthenticationPrincipal Object principal) {
        
        log.debug("查询订单详情，订单ID: {}", orderId);
        
        try {
            Long userId = extractUserId(principal);
            Optional<OrderService.OrderDetailResponse> orderOpt = 
                orderService.getOrderById(orderId, userId);
            
            if (orderOpt.isPresent()) {
                OrderService.OrderDetailResponse order = orderOpt.get();
                log.debug("查询订单详情成功，订单号: {}", order.orderNo());
                return ResponseEntity.ok(ApiResponse.success(order));
            } else {
                log.warn("订单不存在或无权限访问，订单ID: {}, 用户ID: {}", orderId, userId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("查询订单详情失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询订单详情失败：" + e.getMessage()));
        }
    }

    /**
     * 根据订单编号获取订单详情
     * 
     * @param orderNo 订单编号
     * @param principal 当前用户
     * @return 订单详情
     */
    @GetMapping("/no/{orderNo}")
    @Operation(summary = "根据订单编号获取详情", description = "通过订单编号查询订单详情")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.OrderDetailResponse>> getOrderByNo(
            @Parameter(description = "订单编号", required = true) @PathVariable String orderNo,
            @AuthenticationPrincipal Object principal) {
        
        log.debug("根据订单编号查询订单，订单编号: {}", orderNo);
        
        try {
            Long userId = extractUserId(principal);
            Optional<OrderService.OrderDetailResponse> orderOpt = 
                orderService.getOrderByNo(orderNo, userId);
            
            if (orderOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(orderOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("根据订单编号查询失败，订单编号: {}, 错误: {}", orderNo, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询订单失败：" + e.getMessage()));
        }
    }

    /**
     * 查询用户订单列表
     * 
     * 查询功能：
     * 1. 分页查询：支持大数据量的分页展示
     * 2. 状态筛选：按订单状态和支付状态筛选
     * 3. 时间范围：按创建时间范围筛选
     * 4. 关键词搜索：按订单编号或商品名称搜索
     * 5. 排序支持：按时间、金额等字段排序
     * 
     * @param status 订单状态（可选）
     * @param paymentStatus 支付状态（可选）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @param keyword 搜索关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @param sort 排序字段
     * @param direction 排序方向
     * @param principal 当前用户
     * @return 订单分页列表
     */
    @GetMapping
    @Operation(summary = "获取用户订单列表", description = "分页查询当前用户的订单列表")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderService.OrderListResponse>>> getUserOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) Order.OrderStatus status,
            @Parameter(description = "支付状态") @RequestParam(required = false) Order.PaymentStatus paymentStatus,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Object principal) {
        
        log.debug("查询用户订单列表，状态: {}, 页码: {}, 大小: {}", status, page, size);
        
        try {
            if (size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("每页大小不能超过100"));
            }
            
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderService.OrderListResponse> orderPage = orderService.getUserOrders(
                userId, status, paymentStatus, startDate, endDate, keyword, pageable);
            
            log.debug("查询用户订单列表成功，总数: {}, 当前页数量: {}", 
                     orderPage.getTotalElements(), orderPage.getNumberOfElements());
            
            return ResponseEntity.ok(ApiResponse.success(orderPage));
            
        } catch (Exception e) {
            log.error("查询用户订单列表失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询订单列表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户订单统计
     * 
     * @param principal 当前用户
     * @return 订单数量统计
     */
    @GetMapping("/counts")
    @Operation(summary = "获取订单数量统计", description = "获取当前用户各状态订单的数量统计")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderCounts(
            @AuthenticationPrincipal Object principal) {
        
        log.debug("查询用户订单统计");
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            Map<String, Long> counts = orderService.getUserOrderCounts(userId);
            
            log.debug("查询用户订单统计成功，用户ID: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(counts));
            
        } catch (Exception e) {
            log.error("查询用户订单统计失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询订单统计失败：" + e.getMessage()));
        }
    }

    // ========== 订单管理接口 ==========

    /**
     * 取消订单
     * 
     * 取消条件：
     * 1. 用户权限：只能取消自己的订单
     * 2. 状态限制：只有特定状态的订单可以取消
     * 3. 时间限制：支付后一定时间内可以取消
     * 4. 商品限制：某些特殊商品不支持取消
     * 
     * 取消处理：
     * 1. 状态更新：更新订单状态为已取消
     * 2. 库存释放：释放预占的商品库存
     * 3. 优惠回滚：回滚优惠券和积分使用
     * 4. 退款处理：已支付订单发起退款
     * 5. 通知发送：发送取消通知给用户
     * 
     * @param orderId 订单ID
     * @param reason 取消原因
     * @param principal 当前用户
     * @return 取消结果
     */
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "取消订单", description = "用户取消自己的订单")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "取消原因") @RequestParam(required = false, defaultValue = "用户主动取消") String reason,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户取消订单请求，订单ID: {}, 取消原因: {}", orderId, reason);
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = orderService.cancelOrder(orderId, userId, reason);
            
            if (success) {
                log.info("订单取消成功，订单ID: {}, 用户ID: {}", orderId, userId);
                return ResponseEntity.ok(ApiResponse.success(null, "订单取消成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("订单取消失败"));
            }
            
        } catch (Exception e) {
            log.error("取消订单失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消订单失败：" + e.getMessage()));
        }
    }

    /**
     * 删除订单
     * 
     * 删除条件：
     * - 订单状态为已取消或已完成
     * - 用户本人或管理员权限
     * - 超过一定时间的历史订单
     * 
     * @param orderId 订单ID
     * @param principal 当前用户
     * @return 删除结果
     */
    @DeleteMapping("/{orderId}")
    @Operation(summary = "删除订单", description = "删除已取消或已完成的订单")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户删除订单请求，订单ID: {}", orderId);
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = orderService.deleteOrder(orderId, userId);
            
            if (success) {
                log.info("订单删除成功，订单ID: {}, 用户ID: {}", orderId, userId);
                return ResponseEntity.ok(ApiResponse.success(null, "订单删除成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("订单删除失败"));
            }
            
        } catch (Exception e) {
            log.error("删除订单失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除订单失败：" + e.getMessage()));
        }
    }

    // ========== 支付管理接口 ==========

    /**
     * 发起订单支付
     * 
     * 支付流程：
     * 1. 订单验证：验证订单状态和支付条件
     * 2. 金额验证：验证支付金额的准确性
     * 3. 支付发起：调用第三方支付接口
     * 4. 状态更新：更新订单支付状态
     * 5. 参数返回：返回支付参数给前端
     * 
     * @param orderId 订单ID
     * @param paymentMethod 支付方式
     * @param principal 当前用户
     * @return 支付参数
     */
    @PostMapping("/{orderId}/pay")
    @Operation(summary = "发起订单支付", description = "为订单发起支付流程")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.PaymentResult>> payOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "支付方式", required = true) @RequestParam String paymentMethod,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户发起支付请求，订单ID: {}, 支付方式: {}", orderId, paymentMethod);
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            OrderService.PaymentResult result = orderService.initiatePayment(orderId, userId, paymentMethod);
            
            if (result.success()) {
                log.info("订单支付发起成功，订单ID: {}, 支付ID: {}", orderId, result.paymentId());
                return ResponseEntity.ok(ApiResponse.success(result, "支付发起成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.message()));
            }
            
        } catch (Exception e) {
            log.error("发起订单支付失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("发起支付失败：" + e.getMessage()));
        }
    }

    /**
     * 支付回调处理（内部接口）
     * 
     * @param paymentData 支付回调数据
     * @return 处理结果
     */
    @PostMapping("/payment/callback")
    @Operation(summary = "支付回调处理", description = "处理第三方支付的回调通知")
    public ResponseEntity<ApiResponse<OrderService.PaymentCallbackResult>> handlePaymentCallback(
            @RequestBody Map<String, Object> paymentData) {
        
        log.info("收到支付回调，订单编号: {}", paymentData.get("orderNo"));
        
        try {
            OrderService.PaymentCallbackResult result = orderService.handlePaymentCallback(paymentData);
            
            if (result.success()) {
                log.info("支付回调处理成功，订单ID: {}", result.orderId());
                return ResponseEntity.ok(ApiResponse.success(result, "支付回调处理成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.message()));
            }
            
        } catch (Exception e) {
            log.error("处理支付回调失败，错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("处理支付回调失败：" + e.getMessage()));
        }
    }

    // ========== 订单履行接口 ==========

    /**
     * 确认收货
     * 
     * 确认条件：
     * 1. 订单状态为已签收
     * 2. 用户本人确认
     * 3. 在确认期限内
     * 
     * 确认处理：
     * 1. 状态更新：更新订单状态为已完成
     * 2. 时间记录：记录确认收货时间
     * 3. 资金释放：释放平台担保资金
     * 4. 评价提醒：发送商品评价提醒
     * 
     * @param orderId 订单ID
     * @param principal 当前用户
     * @return 确认结果
     */
    @PutMapping("/{orderId}/confirm-delivery")
    @Operation(summary = "确认收货", description = "用户确认收到商品")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> confirmDelivery(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户确认收货请求，订单ID: {}", orderId);
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            boolean success = orderService.confirmDelivery(orderId, userId);
            
            if (success) {
                log.info("确认收货成功，订单ID: {}, 用户ID: {}", orderId, userId);
                return ResponseEntity.ok(ApiResponse.success(null, "确认收货成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("确认收货失败"));
            }
            
        } catch (Exception e) {
            log.error("确认收货失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("确认收货失败：" + e.getMessage()));
        }
    }

    // ========== 售后服务接口 ==========

    /**
     * 申请退货
     * 
     * 退货条件：
     * 1. 在退货期限内
     * 2. 商品支持退货
     * 3. 商品状态良好
     * 
     * @param orderId 订单ID
     * @param request 退货申请请求
     * @param principal 当前用户
     * @return 退货申请结果
     */
    @PostMapping("/{orderId}/return")
    @Operation(summary = "申请退货", description = "用户申请退货")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.ReturnResult>> applyReturn(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequest request,
            @AuthenticationPrincipal Object principal) {
        
        log.info("用户申请退货，订单ID: {}, 退货原因: {}", orderId, request.reason());
        
        try {
            Long userId = extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            
            OrderService.ReturnResult result = orderService.applyReturn(
                orderId, request.itemIds(), request.reason(), request.description(), userId);
            
            if (result.success()) {
                log.info("退货申请成功，订单ID: {}, 退货ID: {}", orderId, result.returnId());
                return ResponseEntity.ok(ApiResponse.success(result, "退货申请提交成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.message()));
            }
            
        } catch (Exception e) {
            log.error("申请退货失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("申请退货失败：" + e.getMessage()));
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

    // ========== 内部类定义 ==========

    /**
     * 退货申请请求
     */
    public record ReturnRequest(
        List<Long> itemIds,      // 退货商品项ID列表
        String reason,           // 退货原因
        String description       // 退货描述
    ) {}
}