/*
文件级分析：
- 职责：商品图片实体类，管理商品的图片信息
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：ProductImage明确表示商品图片实体
- 调用关系：多对一关联Product商品实体，被商品详情页使用

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 支持多种图片类型：主图、详情图、规格图等
3. 包含排序功能，控制图片展示顺序
4. 记录图片的物理信息，便于优化和管理
5. 支持图片的不同尺寸存储
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 商品图片实体类
 * 
 * 管理商品的图片信息，支持：
 * 1. 多种图片类型：主图、详情图、规格图
 * 2. 图片排序和展示控制
 * 3. 图片物理信息记录
 * 4. 多尺寸图片存储
 * 
 * 图片类型说明：
 * - 主图：商品列表页展示的主要图片，每个商品只能有一张
 * - 详情图：商品详情页轮播展示的图片，可以有多张
 * - 规格图：展示不同规格（如颜色）的图片
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"product"})
@ToString(callSuper = true, exclude = {"product"})
@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_product_sort", columnList = "product_id, sort_order"),
        @Index(name = "idx_is_main", columnList = "is_main"),
        @Index(name = "idx_image_type", columnList = "image_type")
})
public class ProductImage extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 关联的商品
     * 多对一关联，多张图片属于一个商品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_image_product"))
    @NotNull(message = "关联商品不能为空")
    @JsonIgnore
    private Product product;
    
    /**
     * 图片URL地址
     * 图片的完整访问地址
     */
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL长度不能超过500字符")
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    
    /**
     * 缩略图URL
     * 用于列表页展示的小尺寸图片
     */
    @Size(max = 500, message = "缩略图URL长度不能超过500字符")
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    /**
     * 中等尺寸图片URL
     * 用于详情页轮播的中等尺寸图片
     */
    @Size(max = 500, message = "中等尺寸图片URL长度不能超过500字符")
    @Column(name = "medium_url", length = 500)
    private String mediumUrl;
    
    /**
     * 大尺寸图片URL
     * 用于查看大图的高清图片
     */
    @Size(max = 500, message = "大尺寸图片URL长度不能超过500字符")
    @Column(name = "large_url", length = 500)
    private String largeUrl;
    
    /**
     * 图片标题
     * 图片的描述标题，用于SEO和无障碍访问
     */
    @Size(max = 200, message = "图片标题长度不能超过200字符")
    @Column(name = "title", length = 200)
    private String title;
    
    /**
     * 图片描述
     * 图片的详细描述信息
     */
    @Size(max = 500, message = "图片描述长度不能超过500字符")
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 图片类型
     * 1-主图，2-详情图，3-规格图
     */
    @Column(name = "image_type", nullable = false)
    private Integer imageType = 2; // 默认为详情图
    
    /**
     * 是否为主图
     * 标记是否为商品的主要展示图片
     */
    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;
    
    /**
     * 排序顺序
     * 图片的展示顺序，数值越小越靠前
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
    /**
     * 图片宽度（像素）
     * 原始图片的宽度
     */
    @Column(name = "width")
    private Integer width;
    
    /**
     * 图片高度（像素）
     * 原始图片的高度
     */
    @Column(name = "height")
    private Integer height;
    
    /**
     * 图片文件大小（字节）
     * 图片文件的大小，用于存储管理
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 图片格式
     * 图片的文件格式，如jpg、png、webp等
     */
    @Size(max = 10, message = "图片格式长度不能超过10字符")
    @Column(name = "format", length = 10)
    private String format;
    
    /**
     * 原始文件名
     * 用户上传时的原始文件名
     */
    @Size(max = 255, message = "原始文件名长度不能超过255字符")
    @Column(name = "original_filename")
    private String originalFilename;
    
    /**
     * 存储路径
     * 图片在服务器上的存储路径
     */
    @Size(max = 500, message = "存储路径长度不能超过500字符")
    @Column(name = "storage_path", length = 500)
    private String storagePath;
    
    /**
     * 图片颜色值
     * 如果是规格图，记录对应的颜色值（十六进制）
     */
    @Size(max = 7, message = "颜色值长度不能超过7字符")
    @Column(name = "color", length = 7)
    private String color;
    
    /**
     * 规格值
     * 如果是规格图，记录对应的规格值，如颜色名称、尺寸等
     */
    @Size(max = 100, message = "规格值长度不能超过100字符")
    @Column(name = "spec_value", length = 100)
    private String specValue;
    
    // ======================== 图片类型常量 ========================
    
    /** 主图类型 */
    public static final int TYPE_MAIN = 1;
    
    /** 详情图类型 */
    public static final int TYPE_DETAIL = 2;
    
    /** 规格图类型 */
    public static final int TYPE_SPEC = 3;
    
    // ======================== 业务方法 ========================
    
    /**
     * 判断是否为主图
     * 
     * @return true表示主图，false表示非主图
     */
    public boolean isMainImage() {
        return Boolean.TRUE.equals(this.isMain);
    }
    
    /**
     * 判断是否为详情图
     * 
     * @return true表示详情图，false表示非详情图
     */
    public boolean isDetailImage() {
        return TYPE_DETAIL == this.imageType;
    }
    
    /**
     * 判断是否为规格图
     * 
     * @return true表示规格图，false表示非规格图
     */
    public boolean isSpecImage() {
        return TYPE_SPEC == this.imageType;
    }
    
    /**
     * 设置为主图
     * 同时设置图片类型和主图标记
     */
    public void setAsMainImage() {
        this.imageType = TYPE_MAIN;
        this.isMain = true;
        this.sortOrder = 0; // 主图排序最靠前
    }
    
    /**
     * 设置为详情图
     */
    public void setAsDetailImage() {
        this.imageType = TYPE_DETAIL;
        this.isMain = false;
    }
    
    /**
     * 设置为规格图
     * 
     * @param specValue 规格值
     * @param color 颜色值（可选）
     */
    public void setAsSpecImage(String specValue, String color) {
        this.imageType = TYPE_SPEC;
        this.isMain = false;
        this.specValue = specValue;
        this.color = color;
    }
    
    /**
     * 获取适合的图片URL
     * 根据需要的尺寸返回对应的图片URL
     * 
     * @param size 图片尺寸：thumbnail、medium、large
     * @return 对应尺寸的图片URL
     */
    public String getImageUrlBySize(String size) {
        if (size == null) {
            return this.imageUrl;
        }
        
        return switch (size.toLowerCase()) {
            case "thumbnail" -> this.thumbnailUrl != null ? this.thumbnailUrl : this.imageUrl;
            case "medium" -> this.mediumUrl != null ? this.mediumUrl : this.imageUrl;
            case "large" -> this.largeUrl != null ? this.largeUrl : this.imageUrl;
            default -> this.imageUrl;
        };
    }
    
    /**
     * 获取图片的宽高比
     * 
     * @return 宽高比，如果宽度或高度为空则返回0
     */
    public double getAspectRatio() {
        if (this.width != null && this.height != null && this.height > 0) {
            return (double) this.width / this.height;
        }
        return 0.0;
    }
    
    /**
     * 获取文件大小的可读格式
     * 
     * @return 格式化后的文件大小字符串
     */
    public String getFormattedFileSize() {
        if (this.fileSize == null) {
            return "未知";
        }
        
        if (this.fileSize < 1024) {
            return this.fileSize + " B";
        } else if (this.fileSize < 1024 * 1024) {
            return String.format("%.1f KB", this.fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", this.fileSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取图片类型描述
     * 
     * @return 图片类型的中文描述
     */
    public String getImageTypeDescription() {
        return switch (this.imageType) {
            case TYPE_MAIN -> "主图";
            case TYPE_DETAIL -> "详情图";
            case TYPE_SPEC -> "规格图";
            default -> "未知";
        };
    }
}