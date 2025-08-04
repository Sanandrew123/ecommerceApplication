package com.ecommerce.dto.request.order;

/*
 * 文件职责: 订单创建请求DTO，定义用户下单时需要提交的参数信息
 * 
 * 开发心理活动：
 * 1. 下单请求设计原则：
 *    - 参数完整性：包含创建订单所需的所有必要信息
 *    - 数据验证：严格的参数校验确保数据质量
 *    - 业务规则：体现电商下单的业务约束和规则
 *    - 安全性：防止恶意参数和数据篡改
 * 
 * 2. 电商下单核心要素：
 *    - 商品信息：用户选择的商品及其规格、数量
 *    - 收货信息：收货人、收货地址等物流信息
 *    - 支付信息：支付方式、优惠券使用等
 *    - 用户偏好：配送时间、特殊要求等
 * 
 * 3. 参数验证策略：
 *    - 基础验证：非空、长度、格式等基础约束
 *    - 业务验证：库存检查、价格校验、优惠规则等
 *    - 安全验证：防止SQL注入、XSS攻击等
 *    - 完整性验证：数据关联性和一致性检查
 * 
 * 4. 用户体验考虑：
 *    - 错误提示：友好的参数错误提示信息
 *    - 默认值：合理的默认参数减少用户输入
 *    - 灵活性：支持多种下单场景和用户选择
 *    - 容错性：对用户输入错误的容错处理
 * 
 * 包结构设计思路:
 * - 放在dto.request.order包下，表示订单相关的请求DTO
 * - 与响应DTO分离，职责明确
 * 
 * 命名原因:
 * - OrderCreateRequest明确表达订单创建请求的功能
 * - 符合Request后缀的DTO命名规范
 * 
 * 依赖关系:
 * - 被OrderController接收并验证
 * - 被OrderService处理业务逻辑
 * - 转换为Order和OrderItem实体
 */

import com.ecommerce.validator.annotation.Phone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建请求DTO
 * 
 * 功能说明：
 * 1. 接收用户下单时提交的所有参数信息
 * 2. 进行严格的参数验证确保数据质量
 * 3. 提供订单创建所需的完整数据结构
 * 4. 支持多种下单场景和业务需求
 * 
 * 业务特性：
 * 1. 商品选择：支持多商品、多规格的订单创建
 * 2. 收货信息：完整的收货人和收货地址信息
 * 3. 支付选择：支付方式和优惠信息的选择
 * 4. 物流选择：配送方式和配送时间的选择
 * 5. 特殊需求：用户备注和特殊要求
 * 6. 价格校验：前端计算价格的后端校验
 * 7. 库存验证：下单时的库存可用性检查
 * 8. 优惠应用：优惠券、积分等优惠的应用
 * 
 * 验证规则：
 * 1. 基础验证：字段非空、长度限制、格式校验
 * 2. 业务验证：商品存在性、库存充足性、价格一致性
 * 3. 关联验证：地址有效性、优惠券可用性
 * 4. 安全验证：防止恶意参数和数据篡改
 * 5. 完整性验证：订单项与总金额的一致性
 * 6. 权限验证：用户权限和操作合法性
 * 7. 限制验证：下单频率、订单限额等限制
 * 8. 状态验证：商品上架状态、用户账户状态
 * 
 * 使用场景：
 * 1. 立即下单：用户直接购买商品
 * 2. 购物车下单：从购物车批量下单
 * 3. 预订下单：预售商品的预订
 * 4. 团购下单：参与团购活动下单
 * 5. 秒杀下单：参与秒杀活动下单
 * 6. 代购下单：代他人购买商品
 * 7. 企业下单：企业批量采购
 * 8. 分期下单：使用分期付款下单
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    // ========== 订单商品信息 ==========

    /**
     * 订单商品项列表
     * 
     * 验证规则：
     * - 不能为空：用户必须选择至少一个商品
     * - 数量限制：单次下单商品种类不能过多
     * - 内容验证：每个商品项都必须通过验证
     */
    @Valid
    @NotEmpty(message = "订单商品不能为空")
    @Size(max = 50, message = "单次下单商品种类不能超过50种")
    private List<OrderItemRequest> orderItems;

    // ========== 收货信息 ==========

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(min = 1, max = 100, message = "收货人姓名长度应在1-100字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$", message = "收货人姓名只能包含中文、英文和空格")
    private String receiverName;

    /**
     * 收货人手机号
     */
    @NotBlank(message = "收货人手机号不能为空")
    @Phone(message = "收货人手机号格式不正确")
    private String receiverPhone;

    /**
     * 收货人邮箱（可选）
     */
    @Email(message = "收货人邮箱格式不正确")
    @Size(max = 255, message = "收货人邮箱长度不能超过255字符")
    private String receiverEmail;

    /**
     * 收货省份
     */
    @NotBlank(message = "收货省份不能为空")
    @Size(max = 50, message = "收货省份长度不能超过50字符")
    private String receiverProvince;

    /**
     * 收货城市
     */
    @NotBlank(message = "收货城市不能为空")
    @Size(max = 50, message = "收货城市长度不能超过50字符")
    private String receiverCity;

    /**
     * 收货区县
     */
    @NotBlank(message = "收货区县不能为空")
    @Size(max = 50, message = "收货区县长度不能超过50字符")
    private String receiverDistrict;

    /**
     * 详细收货地址
     */
    @NotBlank(message = "详细收货地址不能为空")
    @Size(min = 5, max = 500, message = "详细收货地址长度应在5-500字符之间")
    private String receiverAddress;

    /**
     * 邮政编码（可选）
     */
    @Pattern(regexp = "^\\d{6}$", message = "邮政编码必须是6位数字")
    private String receiverZipCode;

    // ========== 支付和优惠信息 ==========

    /**
     * 支付方式
     * 
     * 支付方式：
     * - ALIPAY: 支付宝
     * - WECHAT: 微信支付
     * - UNION_PAY: 银联支付
     * - CREDIT_CARD: 信用卡
     * - BANK_TRANSFER: 银行转账
     * - WALLET: 钱包余额
     * - INSTALLMENT: 分期付款
     */
    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(ALIPAY|WECHAT|UNION_PAY|CREDIT_CARD|BANK_TRANSFER|WALLET|INSTALLMENT)$", 
             message = "支付方式无效")
    private String paymentMethod;

    /**
     * 优惠券ID（可选）
     */
    private Long couponId;

    /**
     * 优惠券编码（可选）
     */
    @Size(max = 50, message = "优惠券编码长度不能超过50字符")
    private String couponCode;

    /**
     * 使用积分数量（可选）
     */
    @Min(value = 0, message = "使用积分数量不能为负数")
    @Max(value = 999999, message = "使用积分数量不能超过999999")
    private Integer pointsToUse = 0;

    // ========== 物流信息 ==========

    /**
     * 配送方式
     * 
     * 配送方式：
     * - STANDARD: 标准配送
     * - EXPRESS: 快速配送
     * - SAME_DAY: 当日达
     * - NEXT_DAY: 次日达
     * - PICKUP: 到店自提
     * - LOGISTICS: 物流配送
     */
    @NotBlank(message = "配送方式不能为空")
    @Pattern(regexp = "^(STANDARD|EXPRESS|SAME_DAY|NEXT_DAY|PICKUP|LOGISTICS)$", 
             message = "配送方式无效")
    private String shippingMethod = "STANDARD";

    /**
     * 期望配送时间
     * 
     * 时间选择：
     * - ANY_TIME: 任意时间
     * - MORNING: 上午配送
     * - AFTERNOON: 下午配送
     * - EVENING: 晚上配送
     * - WEEKEND: 周末配送
     * - WORKDAY: 工作日配送
     */
    @Pattern(regexp = "^(ANY_TIME|MORNING|AFTERNOON|EVENING|WEEKEND|WORKDAY)$", 
             message = "期望配送时间无效")
    private String preferredDeliveryTime = "ANY_TIME";

    /**
     * 是否需要发票
     */
    private Boolean needInvoice = false;

    /**
     * 发票类型（需要发票时必填）
     * 
     * 发票类型：
     * - PERSONAL: 个人发票
     * - COMPANY: 企业发票
     * - ELECTRONIC: 电子发票
     */
    @Pattern(regexp = "^(PERSONAL|COMPANY|ELECTRONIC)$", message = "发票类型无效")
    private String invoiceType;

    /**
     * 发票抬头（需要发票时必填）
     */
    @Size(max = 200, message = "发票抬头长度不能超过200字符")
    private String invoiceTitle;

    /**
     * 纳税人识别号（企业发票时必填）
     */
    @Size(max = 50, message = "纳税人识别号长度不能超过50字符")
    private String taxNumber;

    // ========== 价格信息（用于校验） ==========

    /**
     * 前端计算的商品总金额
     * 
     * 用途：
     * - 与后端计算结果对比，确保价格一致性
     * - 防止前端价格被恶意篡改
     * - 提供价格校验的依据
     */
    @NotNull(message = "商品总金额不能为空")
    @DecimalMin(value = "0.01", message = "商品总金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "商品总金额格式不正确")
    private BigDecimal totalAmount;

    /**
     * 前端计算的运费
     */
    @NotNull(message = "运费不能为空")
    @DecimalMin(value = "0.00", message = "运费不能为负数")
    @Digits(integer = 8, fraction = 2, message = "运费格式不正确")
    private BigDecimal shippingFee;

    /**
     * 前端计算的优惠金额
     */
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", message = "优惠金额不能为负数")
    @Digits(integer = 8, fraction = 2, message = "优惠金额格式不正确")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 前端计算的实付金额
     */
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.01", message = "实付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "实付金额格式不正确")
    private BigDecimal actualAmount;

    // ========== 备注信息 ==========

    /**
     * 用户备注
     */
    @Size(max = 500, message = "用户备注长度不能超过500字符")
    private String userRemark;

    /**
     * 特殊要求
     */
    @Size(max = 500, message = "特殊要求长度不能超过500字符")
    private String specialRequirements;

    // ========== 业务控制参数 ==========

    /**
     * 订单来源
     * 
     * 来源类型：
     * - WEB: 网页端
     * - MOBILE: 手机端
     * - APP_IOS: iOS应用
     * - APP_ANDROID: Android应用
     * - MINI_PROGRAM: 小程序
     * - API: API接口
     */
    @Pattern(regexp = "^(WEB|MOBILE|APP_IOS|APP_ANDROID|MINI_PROGRAM|API)$", 
             message = "订单来源无效")
    private String orderSource = "WEB";

    /**
     * 是否立即支付
     */
    private Boolean immediatePayment = true;

    /**
     * 订单类型
     * 
     * 订单类型：
     * - NORMAL: 普通订单
     * - PRE_SALE: 预售订单
     * - GROUP_BUY: 团购订单
     * - FLASH_SALE: 秒杀订单
     * - GIFT: 赠品订单
     * - EXCHANGE: 换货订单
     */
    @Pattern(regexp = "^(NORMAL|PRE_SALE|GROUP_BUY|FLASH_SALE|GIFT|EXCHANGE)$", 
             message = "订单类型无效")
    private String orderType = "NORMAL";

    /**
     * 活动ID（参与活动时填写）
     */
    private Long activityId;

    // ========== 内部类：订单项请求 ==========

    /**
     * 订单商品项请求DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {

        /**
         * 商品ID
         */
        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID必须大于0")
        private Long productId;

        /**
         * 商品规格（可选）
         * 
         * 规格格式：JSON字符串
         * 示例：{"color":"红色","size":"L"}
         */
        @Size(max = 1000, message = "商品规格信息长度不能超过1000字符")
        private String productSpec;

        /**
         * 购买数量
         */
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于0")
        @Max(value = 99999, message = "购买数量不能超过99999")
        private Integer quantity;

        /**
         * 商品单价（用于价格校验）
         * 
         * 用途：
         * - 与后端商品价格对比
         * - 防止价格被恶意修改
         * - 确保订单金额计算准确
         */
        @NotNull(message = "商品单价不能为空")
        @DecimalMin(value = "0.01", message = "商品单价必须大于0")
        @Digits(integer = 8, fraction = 2, message = "商品单价格式不正确")
        private BigDecimal unitPrice;

        /**
         * 商品小计（用于价格校验）
         * 
         * 计算规则：小计 = 单价 × 数量
         */
        @NotNull(message = "商品小计不能为空")
        @DecimalMin(value = "0.01", message = "商品小计必须大于0")
        @Digits(integer = 10, fraction = 2, message = "商品小计格式不正确")
        private BigDecimal subtotal;

        /**
         * 商品优惠金额（可选）
         */
        @DecimalMin(value = "0.00", message = "商品优惠金额不能为负数")
        @Digits(integer = 8, fraction = 2, message = "商品优惠金额格式不正确")
        private BigDecimal discountAmount = BigDecimal.ZERO;

        /**
         * 备注信息（可选）
         */
        @Size(max = 200, message = "商品备注长度不能超过200字符")
        private String remark;

        // ========== 业务方法 ==========

        /**
         * 验证价格计算是否正确
         * 
         * @return 是否正确
         */
        public boolean isSubtotalCorrect() {
            if (unitPrice == null || quantity == null || subtotal == null) {
                return false;
            }
            BigDecimal calculatedSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            return calculatedSubtotal.compareTo(subtotal) == 0;
        }

        /**
         * 计算实付金额
         * 
         * @return 实付金额
         */
        public BigDecimal getActualAmount() {
            BigDecimal actualAmount = subtotal;
            if (discountAmount != null) {
                actualAmount = actualAmount.subtract(discountAmount);
            }
            return actualAmount.compareTo(BigDecimal.ZERO) > 0 ? actualAmount : BigDecimal.ZERO;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 验证发票信息完整性
     * 
     * @return 验证结果和错误信息
     */
    public String validateInvoiceInfo() {
        if (!Boolean.TRUE.equals(needInvoice)) {
            return null; // 不需要发票，验证通过
        }

        if (invoiceType == null || invoiceType.trim().isEmpty()) {
            return "需要发票时，发票类型不能为空";
        }

        if (invoiceTitle == null || invoiceTitle.trim().isEmpty()) {
            return "需要发票时，发票抬头不能为空";
        }

        if ("COMPANY".equals(invoiceType) && 
            (taxNumber == null || taxNumber.trim().isEmpty())) {
            return "企业发票时，纳税人识别号不能为空";
        }

        return null; // 验证通过
    }

    /**
     * 验证价格计算一致性
     * 
     * @return 验证结果和错误信息
     */
    public String validatePriceConsistency() {
        if (orderItems == null || orderItems.isEmpty()) {
            return "订单商品不能为空";
        }

        // 验证每个商品项的价格计算
        for (OrderItemRequest item : orderItems) {
            if (!item.isSubtotalCorrect()) {
                return "商品ID " + item.getProductId() + " 的价格计算不正确";
            }
        }

        // 验证总金额计算
        BigDecimal calculatedTotal = orderItems.stream()
                .map(OrderItemRequest::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(totalAmount) != 0) {
            return "商品总金额计算不正确";
        }

        // 验证实付金额计算
        BigDecimal calculatedActual = totalAmount
                .add(shippingFee != null ? shippingFee : BigDecimal.ZERO)
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);

        if (calculatedActual.compareTo(actualAmount) != 0) {
            return "实付金额计算不正确";
        }

        return null; // 验证通过
    }

    /**
     * 获取订单商品总数量
     * 
     * @return 商品总数量
     */
    public int getTotalQuantity() {
        if (orderItems == null) {
            return 0;
        }
        return orderItems.stream()
                .mapToInt(OrderItemRequest::getQuantity)
                .sum();
    }

    /**
     * 获取订单商品种类数
     * 
     * @return 商品种类数
     */
    public int getProductTypeCount() {
        if (orderItems == null) {
            return 0;
        }
        return orderItems.size();
    }

    /**
     * 获取完整收货地址
     * 
     * @return 完整地址字符串
     */
    public String getFullAddress() {
        return receiverProvince + receiverCity + receiverDistrict + receiverAddress;
    }

    /**
     * 检查是否使用优惠券
     * 
     * @return 是否使用优惠券
     */
    public boolean isUsingCoupon() {
        return couponId != null || (couponCode != null && !couponCode.trim().isEmpty());
    }

    /**
     * 检查是否使用积分
     * 
     * @return 是否使用积分
     */
    public boolean isUsingPoints() {
        return pointsToUse != null && pointsToUse > 0;
    }

    /**
     * 检查是否需要特殊处理
     * 
     * @return 是否需要特殊处理
     */
    public boolean needsSpecialHandling() {
        return Boolean.TRUE.equals(needInvoice) || 
               isUsingCoupon() || 
               isUsingPoints() ||
               (specialRequirements != null && !specialRequirements.trim().isEmpty());
    }
}