/*
文件级分析：
- 职责：定义支付方式枚举，规范系统支持的各种支付渠道
- 包结构考虑：位于enums包下，与其他业务枚举统一管理
- 命名原因：PaymentMethod清晰表明这是支付方式枚举
- 调用关系：被Payment实体类使用，在支付渠道选择和费率计算中使用

设计思路：
1. 覆盖主流的支付方式：支付宝、微信、银行卡等
2. 包含支付方式的基本信息：名称、费率、是否启用等
3. 支持支付方式的动态配置和管理
4. 为不同支付方式预留扩展空间
*/
package com.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

/**
 * 支付方式枚举
 * 
 * 定义系统支持的各种支付渠道，包括：
 * - ALIPAY: 支付宝支付
 * - WECHAT_PAY: 微信支付
 * - BANK_CARD: 银行卡支付
 * - CREDIT_CARD: 信用卡支付
 * - BALANCE: 余额支付
 * - POINTS: 积分支付
 * - CASH_ON_DELIVERY: 货到付款
 * - BANK_TRANSFER: 银行转账
 * 
 * 每种支付方式包含：
 * - 支付方式代码和名称
 * - 支付费率信息
 * - 是否启用状态
 * - 支付限额信息
 * - 适用场景说明
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum PaymentMethod {
    
    /**
     * 支付宝支付
     * 使用支付宝进行在线支付
     */
    ALIPAY(1, "支付宝", "alipay", new BigDecimal("0.006"), true, 
           new BigDecimal("0.01"), new BigDecimal("50000.00"), "在线支付"),
    
    /**
     * 微信支付
     * 使用微信进行在线支付
     */
    WECHAT_PAY(2, "微信支付", "wechat", new BigDecimal("0.006"), true,
               new BigDecimal("0.01"), new BigDecimal("50000.00"), "在线支付"),
    
    /**
     * 银行卡支付
     * 使用借记卡进行在线支付
     */
    BANK_CARD(3, "银行卡", "bankcard", new BigDecimal("0.008"), true,
              new BigDecimal("0.01"), new BigDecimal("50000.00"), "在线支付"),
    
    /**
     * 信用卡支付
     * 使用信用卡进行在线支付
     */
    CREDIT_CARD(4, "信用卡", "creditcard", new BigDecimal("0.012"), true,
                new BigDecimal("1.00"), new BigDecimal("100000.00"), "在线支付"),
    
    /**
     * 余额支付
     * 使用账户余额进行支付
     */
    BALANCE(5, "余额支付", "balance", new BigDecimal("0.000"), true,
            new BigDecimal("0.01"), new BigDecimal("999999.99"), "账户余额"),
    
    /**
     * 积分支付
     * 使用会员积分进行支付
     */
    POINTS(6, "积分支付", "points", new BigDecimal("0.000"), true,
           new BigDecimal("1.00"), new BigDecimal("99999.99"), "会员积分"),
    
    /**
     * 货到付款
     * 商品送达时现金支付
     */
    CASH_ON_DELIVERY(7, "货到付款", "cod", new BigDecimal("0.000"), true,
                     new BigDecimal("1.00"), new BigDecimal("5000.00"), "线下支付"),
    
    /**
     * 银行转账
     * 通过银行转账进行支付
     */
    BANK_TRANSFER(8, "银行转账", "transfer", new BigDecimal("0.000"), false,
                  new BigDecimal("1.00"), new BigDecimal("1000000.00"), "线下支付");
    
    /** 支付方式代码 */
    private final Integer code;
    
    /** 支付方式名称 */
    private final String name;
    
    /** 支付方式标识符（用于第三方接口） */
    private final String identifier;
    
    /** 支付费率（百分比，如0.006表示0.6%） */
    private final BigDecimal feeRate;
    
    /** 是否启用 */
    private final Boolean enabled;
    
    /** 最小支付金额 */
    private final BigDecimal minAmount;
    
    /** 最大支付金额 */
    private final BigDecimal maxAmount;
    
    /** 支付方式类型描述 */
    private final String typeDescription;
    
    /**
     * 根据代码获取支付方式枚举
     * 
     * @param code 支付方式代码
     * @return 对应的支付方式枚举，如果找不到则返回null
     */
    public static PaymentMethod fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据标识符获取支付方式枚举
     * 
     * @param identifier 支付方式标识符
     * @return 对应的支付方式枚举，如果找不到则返回null
     */
    public static PaymentMethod fromIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.getIdentifier().equals(identifier.trim()))
                .findFirst()
                .orElse(null);
    }
    
    // ======================== 支付方式分组定义 ========================
    
    /** 在线支付方式 */
    private static final Set<PaymentMethod> ONLINE_METHODS = Set.of(
            ALIPAY, WECHAT_PAY, BANK_CARD, CREDIT_CARD
    );
    
    /** 账户支付方式 */
    private static final Set<PaymentMethod> ACCOUNT_METHODS = Set.of(
            BALANCE, POINTS
    );
    
    /** 线下支付方式 */
    private static final Set<PaymentMethod> OFFLINE_METHODS = Set.of(
            CASH_ON_DELIVERY, BANK_TRANSFER
    );
    
    /** 需要手续费的支付方式 */
    private static final Set<PaymentMethod> FEE_REQUIRED_METHODS = Set.of(
            ALIPAY, WECHAT_PAY, BANK_CARD, CREDIT_CARD
    );
    
    /** 即时到账的支付方式 */
    private static final Set<PaymentMethod> INSTANT_METHODS = Set.of(
            ALIPAY, WECHAT_PAY, BANK_CARD, CREDIT_CARD, BALANCE, POINTS
    );
    
    // ======================== 业务判断方法 ========================
    
    /**
     * 判断是否为在线支付方式
     * 
     * @return true表示在线支付，false表示非在线支付
     */
    public boolean isOnline() {
        return ONLINE_METHODS.contains(this);
    }
    
    /**
     * 判断是否为账户支付方式
     * 
     * @return true表示账户支付，false表示非账户支付
     */
    public boolean isAccount() {
        return ACCOUNT_METHODS.contains(this);
    }
    
    /**
     * 判断是否为线下支付方式
     * 
     * @return true表示线下支付，false表示非线下支付
     */
    public boolean isOffline() {
        return OFFLINE_METHODS.contains(this);
    }
    
    /**
     * 判断是否需要支付手续费
     * 
     * @return true表示需要手续费，false表示不需要手续费
     */
    public boolean requiresFee() {
        return FEE_REQUIRED_METHODS.contains(this);
    }
    
    /**
     * 判断是否为即时到账
     * 
     * @return true表示即时到账，false表示非即时到账
     */
    public boolean isInstant() {
        return INSTANT_METHODS.contains(this);
    }
    
    /**
     * 验证支付金额是否在允许范围内
     * 
     * @param amount 支付金额
     * @return true表示金额有效，false表示金额无效
     */
    public boolean isAmountValid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        return amount.compareTo(this.minAmount) >= 0 && 
               amount.compareTo(this.maxAmount) <= 0;
    }
    
    /**
     * 计算支付手续费
     * 
     * @param amount 支付金额
     * @return 手续费金额
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (!requiresFee()) {
            return BigDecimal.ZERO;
        }
        
        return amount.multiply(this.feeRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * 计算实际支付金额（含手续费）
     * 
     * @param amount 原始金额
     * @return 实际支付金额
     */
    public BigDecimal calculateTotalAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        return amount.add(calculateFee(amount));
    }
    
    /**
     * 获取支付方式的显示顺序
     * 用于前端展示时的排序
     * 
     * @return 显示顺序（数值越小越靠前）
     */
    public int getDisplayOrder() {
        return switch (this) {
            case ALIPAY -> 1;
            case WECHAT_PAY -> 2;
            case BALANCE -> 3;
            case BANK_CARD -> 4;
            case CREDIT_CARD -> 5;
            case POINTS -> 6;
            case CASH_ON_DELIVERY -> 7;
            case BANK_TRANSFER -> 8;
        };
    }
    
    /**
     * 获取支付方式的图标类名
     * 用于前端显示支付方式图标
     * 
     * @return CSS图标类名
     */
    public String getIconClass() {
        return switch (this) {
            case ALIPAY -> "icon-alipay";
            case WECHAT_PAY -> "icon-wechat";
            case BANK_CARD -> "icon-bankcard";
            case CREDIT_CARD -> "icon-creditcard";
            case BALANCE -> "icon-balance";
            case POINTS -> "icon-points";
            case CASH_ON_DELIVERY -> "icon-cod";
            case BANK_TRANSFER -> "icon-transfer";
        };
    }
    
    /**
     * 获取支付方式的颜色主题
     * 用于前端展示时的颜色配置
     * 
     * @return 颜色主题标识
     */
    public String getColorTheme() {
        return switch (this) {
            case ALIPAY -> "blue";
            case WECHAT_PAY -> "green";
            case BANK_CARD, CREDIT_CARD -> "gray";
            case BALANCE -> "orange";
            case POINTS -> "purple";
            case CASH_ON_DELIVERY -> "brown";
            case BANK_TRANSFER -> "teal";
        };
    }
}