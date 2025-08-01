/*
文件级分析：
- 职责：定义商品状态枚举，规范商品在系统中的各种状态
- 包结构考虑：位于enums包下，与其他业务枚举统一管理
- 命名原因：ProductStatus清晰表明这是商品状态枚举
- 调用关系：被Product实体类使用，在商品管理业务中进行状态判断和流转

设计思路：
1. 涵盖商品从创建到下架的完整生命周期
2. 支持商品审核流程，满足平台型电商需求
3. 提供便捷的状态判断方法，简化业务逻辑
4. 考虑商品上下架的运营需求
*/
package com.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商品状态枚举
 * 
 * 定义商品在系统中的各种状态，包括：
 * - DRAFT: 草稿状态，商品信息未完善
 * - PENDING: 待审核状态，等待平台审核
 * - ACTIVE: 上架状态，正常销售中
 * - INACTIVE: 下架状态，暂停销售
 * - OUT_OF_STOCK: 缺货状态，库存为0
 * - DISCONTINUED: 停产状态，不再销售
 * 
 * 状态流转规则：
 * DRAFT -> PENDING (提交审核)
 * PENDING -> ACTIVE (审核通过)
 * PENDING -> DRAFT (审核驳回)
 * ACTIVE -> INACTIVE (手动下架)
 * ACTIVE -> OUT_OF_STOCK (库存耗尽)
 * INACTIVE -> ACTIVE (重新上架)
 * OUT_OF_STOCK -> ACTIVE (补充库存)
 * * -> DISCONTINUED (停产处理)
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {
    
    /**
     * 草稿状态
     * 商品信息不完整，正在编辑中，不对外展示
     */
    DRAFT(1, "草稿"),
    
    /**
     * 待审核状态
     * 商品信息已完善，等待平台审核通过
     */
    PENDING(2, "待审核"),
    
    /**
     * 上架状态
     * 商品审核通过，正常销售中，用户可见可购买
     */
    ACTIVE(3, "上架"),
    
    /**
     * 下架状态
     * 商品暂时停止销售，用户不可见，商家可重新上架
     */
    INACTIVE(4, "下架"),
    
    /**
     * 缺货状态
     * 商品库存为0，暂时无法购买，但商品页面仍可访问
     */
    OUT_OF_STOCK(5, "缺货"),
    
    /**
     * 停产状态
     * 商品永久停止销售，不再补货，仅供历史查询
     */
    DISCONTINUED(6, "停产");
    
    /** 状态码 */
    private final Integer code;
    
    /** 状态描述 */
    private final String description;
    
    /**
     * 根据状态码获取商品状态枚举
     * 
     * @param code 状态码
     * @return 对应的商品状态枚举，如果找不到则返回null
     */
    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(ProductStatus.values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据状态描述获取商品状态枚举
     * 
     * @param description 状态描述
     * @return 对应的商品状态枚举，如果找不到则返回null
     */
    public static ProductStatus fromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.stream(ProductStatus.values())
                .filter(status -> status.getDescription().equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 判断是否为可销售状态
     * 只有上架状态的商品才能正常销售
     * 
     * @return true表示可销售，false表示不可销售
     */
    public boolean isSaleable() {
        return this == ACTIVE;
    }
    
    /**
     * 判断是否为可见状态
     * 上架和缺货状态的商品对用户可见
     * 
     * @return true表示用户可见，false表示用户不可见
     */
    public boolean isVisible() {
        return this == ACTIVE || this == OUT_OF_STOCK;
    }
    
    /**
     * 判断是否为可编辑状态
     * 草稿和待审核状态的商品可以编辑
     * 
     * @return true表示可编辑，false表示不可编辑
     */
    public boolean isEditable() {
        return this == DRAFT || this == PENDING;
    }
    
    /**
     * 判断是否需要审核
     * 
     * @return true表示需要审核，false表示不需要审核
     */
    public boolean needsApproval() {
        return this == PENDING;
    }
    
    /**
     * 判断是否已停产
     * 
     * @return true表示已停产，false表示未停产
     */
    public boolean isDiscontinued() {
        return this == DISCONTINUED;
    }
    
    /**
     * 判断是否缺货
     * 
     * @return true表示缺货，false表示有库存
     */
    public boolean isOutOfStock() {
        return this == OUT_OF_STOCK;
    }
}