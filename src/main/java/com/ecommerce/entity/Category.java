/*
文件级分析：
- 职责：商品分类实体类，映射数据库中的分类表，支持多级分类结构
- 包结构考虑：位于entity包下，与其他实体类统一管理
- 命名原因：Category是电商领域通用的分类概念
- 调用关系：被Product实体关联，被CategoryService使用，自关联实现树形结构

设计思路：
1. 继承BaseEntity，获得审计和软删除功能
2. 使用自关联实现无限级分类结构
3. 包含排序字段，支持分类顺序管理
4. 设计分类路径字段，便于查询和展示
5. 预留扩展字段，支持分类属性和模板
*/
package com.ecommerce.entity;

import com.ecommerce.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类实体类
 * 
 * 支持多级分类的树形结构设计，包含：
 * 1. 基本信息：分类名称、描述、图标等
 * 2. 层级关系：父分类、子分类列表
 * 3. 显示控制：排序、是否显示
 * 4. 路径信息：便于面包屑导航和查询优化
 * 
 * 分类层级设计：
 * - 根分类：parent_id为null的顶级分类
 * - 子分类：通过parent_id关联上级分类
 * - 叶子分类：没有子分类的最底层分类，可以关联商品
 * 
 * 使用场景：
 * - 商品分类管理
 * - 分类树形展示
 * - 商品检索和筛选
 * - 导航菜单构建
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"parent", "children", "products"})
@ToString(callSuper = true, exclude = {"parent", "children", "products"})
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_parent_id", columnList = "parent_id"),
        @Index(name = "idx_level_sort", columnList = "level, sort_order"),
        @Index(name = "idx_path", columnList = "path"),
        @Index(name = "idx_is_visible", columnList = "is_visible")
})
public class Category extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    // ======================== 基本信息字段 ========================
    
    /**
     * 分类名称
     * 分类的显示名称，支持中英文和特殊字符
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1-50字符之间")
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    /**
     * 分类编码
     * 分类的唯一标识符，用于系统内部识别，通常为英文
     */
    @Size(max = 50, message = "分类编码长度不能超过50字符")
    @Column(name = "code", unique = true, length = 50)
    private String code;
    
    /**
     * 分类描述
     * 分类的详细说明，用于SEO和用户理解
     */
    @Size(max = 500, message = "分类描述长度不能超过500字符")
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 分类图标
     * 分类的图标URL或图标类名，用于前端展示
     */
    @Column(name = "icon", length = 200)
    private String icon;
    
    /**
     * 分类图片
     * 分类的封面图片URL，用于分类页面展示
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    // ======================== 层级关系字段 ========================
    
    /**
     * 父分类
     * 多对一关联，实现分类的层级结构
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_category_parent"))
    @JsonIgnore // 避免JSON序列化时的循环引用
    private Category parent;
    
    /**
     * 子分类列表
     * 一对多关联，获取当前分类的所有直接子分类
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    @JsonIgnore // 避免JSON序列化时的循环引用
    private List<Category> children = new ArrayList<>();
    
    /**
     * 分类层级
     * 从1开始，根分类为1，每下一级递增1
     */
    @Column(name = "level", nullable = false)
    private Integer level = 1;
    
    /**
     * 分类路径
     * 存储从根分类到当前分类的ID路径，用斜杠分隔，如：/1/2/3/
     * 便于查询某个分类下的所有子孙分类
     */
    @Column(name = "path", length = 500)
    private String path;
    
    /**
     * 分类全路径名称
     * 存储从根分类到当前分类的名称路径，用斜杠分隔，如：/电子产品/手机/智能手机/
     * 便于面包屑導航和SEO
     */
    @Column(name = "path_name", length = 1000)
    private String pathName;
    
    // ======================== 显示控制字段 ========================
    
    /**
     * 排序字段
     * 同级分类的排序权重，数值越小越靠前
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
    /**
     * 是否可见
     * 控制分类是否在前端显示，隐藏的分类不影响已关联的商品
     */
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;
    
    /**
     * 是否允许关联商品
     * 通常只有叶子节点（最底层分类）才允许关联商品
     */
    @Column(name = "is_leaf", nullable = false)
    private Boolean isLeaf = false;
    
    // ======================== 统计信息字段 ========================
    
    /**
     * 商品数量
     * 该分类下的商品总数（包括子分类），定期统计更新
     */
    @Column(name = "product_count", nullable = false)
    private Integer productCount = 0;
    
    /**
     * 子分类数量
     * 该分类的直接子分类数量
     */
    @Column(name = "children_count", nullable = false)
    private Integer childrenCount = 0;
    
    // ======================== 扩展信息字段 ========================
    
    /**
     * SEO关键词
     * 用于搜索引擎优化的关键词
     */
    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;
    
    /**
     * SEO描述
     * 用于搜索引擎优化的描述信息
     */
    @Column(name = "seo_description", length = 500)
    private String seoDescription;
    
    /**
     * 扩展属性
     * JSON格式存储分类的额外属性，如颜色、图标样式等
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    
    // ======================== 关联关系 ========================
    
    /**
     * 商品列表
     * 该分类下的所有商品
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();
    
    // ======================== 业务方法 ========================
    
    /**
     * 判断是否为根分类
     * 
     * @return true表示根分类，false表示非根分类
     */
    public boolean isRoot() {
        return this.parent == null;
    }
    
    /**
     * 判断是否为叶子分类
     * 
     * @return true表示叶子分类，false表示有子分类
     */
    public boolean isLeafCategory() {
        return this.children == null || this.children.isEmpty();
    }
    
    /**
     * 获取父分类ID
     * 
     * @return 父分类ID，根分类返回null
     */
    public Long getParentId() {
        return this.parent != null ? this.parent.getId() : null;
    }
    
    /**
     * 添加子分类
     * 
     * @param child 子分类
     */
    public void addChild(Category child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        
        this.children.add(child);
        child.setParent(this);
        child.setLevel(this.level + 1);
        
        // 更新子分类数量
        this.childrenCount = this.children.size();
    }
    
    /**
     * 移除子分类
     * 
     * @param child 子分类
     */
    public void removeChild(Category child) {
        if (this.children != null) {
            this.children.remove(child);
            child.setParent(null);
            
            // 更新子分类数量
            this.childrenCount = this.children.size();
        }
    }
    
    /**
     * 构建分类路径
     * 根据父分类递归构建路径字符串
     */
    public void buildPath() {
        if (this.parent == null) {
            // 根分类
            this.path = "/" + this.getId() + "/";
            this.pathName = "/" + this.name + "/";
        } else {
            // 子分类
            this.path = this.parent.getPath() + this.getId() + "/";
            this.pathName = this.parent.getPathName() + this.name + "/";
        }
    }
    
    /**
     * 更新商品数量统计
     * 
     * @param count 商品数量变化值（可为负数）
     */
    public void updateProductCount(int count) {
        this.productCount = Math.max(0, this.productCount + count);
        
        // 递归更新父分类的商品数量
        if (this.parent != null) {
            this.parent.updateProductCount(count);
        }
    }
    
    /**
     * 检查是否可以删除
     * 有子分类或有商品的分类不能删除
     * 
     * @return true表示可以删除，false表示不能删除
     */
    public boolean canDelete() {
        return (this.children == null || this.children.isEmpty()) && 
               (this.products == null || this.products.isEmpty());
    }
}