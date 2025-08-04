package com.ecommerce.entity;

/*
 * 文件职责: 商品分类实体类，支持树形结构的分类管理
 * 
 * 开发心理活动：
 * 1. 树形结构设计考虑：
 *    - 自引用关系：parentId指向父分类
 *    - 层级深度控制：避免过深的分类层次
 *    - 路径维护：便于快速查找和展示
 *    - 排序支持：同级分类的显示顺序
 * 
 * 2. 业务场景分析：
 *    - 电商分类：服装->男装->衬衫->商务衬衫
 *    - 支持多级分类，通常3-4级较为合适
 *    - 分类禁用：软删除，保持数据完整性
 *    - 批量操作：分类树的批量启用/禁用
 * 
 * 3. 性能优化考虑：
 *    - 索引设计：parent_id, level, sort_order
 *    - 路径缓存：全路径字符串便于搜索
 *    - 子分类计数：避免重复查询
 *    - 懒加载：按需加载子分类数据
 * 
 * 4. 数据一致性：
 *    - 父子关系约束：防止循环引用
 *    - 层级更新：父分类变更时更新子分类
 *    - 状态传播：父分类禁用时子分类处理
 *    - 商品关联：分类删除时商品的处理策略
 * 
 * 包结构设计思路:
 * - 放在entity包下，作为核心业务实体
 * - 与Product实体形成一对多关系
 * 
 * 命名原因:
 * - Category明确表达商品分类功能
 * - 符合领域模型的通用命名
 * 
 * 依赖关系:
 * - 自引用：parent_id指向同表的主键
 * - 被Product引用：商品归属分类
 * - 支持树形查询和操作
 */

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类实体类
 * 
 * 功能说明：
 * 1. 支持无限级树形分类结构
 * 2. 提供分类的基本信息管理
 * 3. 支持分类的启用/禁用状态
 * 4. 提供分类路径和层级信息
 * 
 * 树形结构特性：
 * 1. 自引用关系：通过parent_id建立父子关系
 * 2. 层级控制：通过level字段标识分类深度
 * 3. 路径维护：通过path字段记录完整路径
 * 4. 排序支持：通过sort_order控制显示顺序
 * 
 * 业务特性：
 * 1. 分类信息：名称、描述、图标
 * 2. 状态管理：启用/禁用状态
 * 3. SEO支持：SEO标题、关键词、描述
 * 4. 统计信息：商品数量、子分类数量
 * 
 * 扩展特性：
 * 1. 多语言支持：预留国际化字段
 * 2. 个性化配置：分类模板、属性设置
 * 3. 营销支持：推荐标识、热门标识
 * 4. 数据分析：访问统计、转化跟踪
 * 
 * 数据一致性：
 * 1. 父子关系约束：防止循环引用
 * 2. 层级深度限制：避免过深分类
 * 3. 路径同步更新：父分类变更时级联更新
 * 4. 软删除策略：保持历史数据完整性
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_parent_id", columnList = "parent_id"),
    @Index(name = "idx_category_level", columnList = "level"),
    @Index(name = "idx_category_sort_order", columnList = "sort_order"),
    @Index(name = "idx_category_status", columnList = "status"),
    @Index(name = "idx_category_path", columnList = "path"),
    @Index(name = "idx_category_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    // ========== 基础字段 ==========

    /**
     * 分类ID - 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 分类名称
     * 
     * 业务规则：
     * - 必填字段，用于前端展示
     * - 同级分类名称不能重复
     * - 支持多语言扩展
     * - 长度限制：1-50字符
     * 
     * 使用场景：
     * - 分类导航展示
     * - 商品归属展示
     * - 搜索引擎优化
     */
    @Column(name = "name", nullable = false, length = 50)
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;

    /**
     * 分类编码
     * 
     * 业务价值：
     * - 系统内部标识，便于接口调用
     * - 支持多语言时的统一标识
     * - URL友好的分类标识
     * - 第三方系统集成的稳定标识
     * 
     * 命名规范：
     * - 英文字母和数字组合
     * - 下划线分隔，如：men_clothing
     * - 全局唯一，便于缓存和查询
     */
    @Column(name = "code", unique = true, length = 50)
    @Size(max = 50, message = "分类编码长度不能超过50个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "分类编码只能包含字母、数字和下划线")
    private String code;

    /**
     * 分类描述
     * 
     * 用途：
     * - 分类的详细说明
     * - SEO优化的描述内容
     * - 管理员备注信息
     * - 前端工具提示显示
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "分类描述长度不能超过500个字符")
    private String description;

    /**
     * 分类图标
     * 
     * 显示用途：
     * - 分类导航的图标展示
     * - 移动端分类选择器
     * - 商品详情的分类标识
     * - 管理后台的视觉区分
     * 
     * 存储格式：
     * - 图标URL地址
     * - 图标class名称（如FontAwesome）
     * - Base64编码的小图标
     */
    @Column(name = "icon", length = 200)
    @Size(max = 200, message = "分类图标长度不能超过200个字符")
    private String icon;

    /**
     * 分类封面图片
     * 
     * 使用场景：
     * - 分类页面的头部展示
     * - 分类推广的视觉素材
     * - 移动端分类展示
     * - 营销活动的分类标识
     */
    @Column(name = "cover_image", length = 500)
    @Size(max = 500, message = "分类封面图片URL长度不能超过500个字符")
    private String coverImage;

    // ========== 树形结构字段 ==========

    /**
     * 父分类ID
     * 
     * 树形结构核心：
     * - null表示根分类
     * - 指向父分类的主键ID
     * - 建立分类的层级关系
     * - 支持分类树的遍历查询
     * 
     * 约束规则：
     * - 不能指向自己（防止循环引用）
     * - 不能指向自己的子孙分类
     * - 父分类必须存在且未删除
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 分类层级
     * 
     * 层级定义：
     * - 0: 根分类（顶级分类）
     * - 1: 一级分类
     * - 2: 二级分类
     * - n: n级分类
     * 
     * 业务价值：
     * - 快速判断分类深度
     * - 层级查询的性能优化
     * - 前端展示的缩进控制
     * - 权限控制的层级限制
     */
    @Column(name = "level", nullable = false)
    @Min(value = 0, message = "分类层级不能小于0")
    @Max(value = 10, message = "分类层级不能超过10级")
    @Builder.Default
    private Integer level = 0;

    /**
     * 分类路径
     * 
     * 路径格式：
     * - 根分类：/1/
     * - 二级分类：/1/2/
     * - 三级分类：/1/2/3/
     * 
     * 业务优势：
     * - 快速查找所有子分类
     * - 高效的树形查询
     * - 面包屑导航生成
     * - 权限控制的路径匹配
     */
    @Column(name = "path", length = 500)
    @Size(max = 500, message = "分类路径长度不能超过500个字符")
    private String path;

    /**
     * 同级排序号
     * 
     * 排序规则：
     * - 数值越小越靠前
     * - 同级分类按此字段升序排列
     * - 默认值为0
     * - 支持负数实现置顶效果
     * 
     * 应用场景：
     * - 分类导航的显示顺序
     * - 重要分类的优先展示
     * - 季节性分类的临时调整
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 子分类数量
     * 
     * 冗余字段优化：
     * - 避免频繁的子查询统计
     * - 提升分类树展示性能
     * - 支持分类管理的快速判断
     * - 分类删除时的依赖检查
     * 
     * 维护策略：
     * - 子分类增删时自动更新
     * - 定期任务校验数据一致性
     */
    @Column(name = "children_count", nullable = false)
    @Min(value = 0, message = "子分类数量不能小于0")
    @Builder.Default
    private Integer childrenCount = 0;

    // ========== 状态和配置字段 ==========

    /**
     * 分类状态
     */
    public enum CategoryStatus {
        ACTIVE("正常", "分类正常，可以使用"),
        INACTIVE("禁用", "分类已禁用，不可使用"),
        DELETED("已删除", "分类已删除，软删除状态");

        private final String displayName;
        private final String description;

        CategoryStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分类状态
     * 
     * 状态说明：
     * - ACTIVE: 正常状态，可以正常使用
     * - INACTIVE: 禁用状态，不显示但保留数据
     * - DELETED: 删除状态，软删除标记
     * 
     * 业务逻辑：
     * - 禁用父分类时子分类自动禁用
     * - 删除分类时相关商品需要重新分类
     * - 状态变更需要清除相关缓存
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    /**
     * 是否显示在导航
     * 
     * 业务场景：
     * - 有些分类仅用于内部管理
     * - 临时分类不在前端显示
     * - 测试分类的隐藏显示
     * - 季节性分类的显示控制
     */
    @Column(name = "show_in_nav", nullable = false)
    @Builder.Default
    private Boolean showInNav = true;

    /**
     * 是否为热门分类
     * 
     * 营销标识：
     * - 首页推荐分类展示
     * - 搜索结果的优先排序
     * - 移动端的快速入口
     * - 营销活动的重点分类
     */
    @Column(name = "is_hot", nullable = false)
    @Builder.Default
    private Boolean isHot = false;

    /**
     * 是否为推荐分类
     * 
     * 推荐机制：
     * - 基于用户行为的推荐
     * - 运营活动的推荐标识
     * - 个性化推荐的候选分类
     * - A/B测试的分类标识
     */
    @Column(name = "is_recommended", nullable = false)
    @Builder.Default
    private Boolean isRecommended = false;

    // ========== SEO和营销字段 ==========

    /**
     * SEO标题
     * 
     * 搜索引擎优化：
     * - HTML页面的title标签
     * - 搜索结果的标题显示
     * - 社交分享的标题内容
     * - 浏览器标签页的显示文本
     */
    @Column(name = "seo_title", length = 200)
    @Size(max = 200, message = "SEO标题长度不能超过200个字符")
    private String seoTitle;

    /**
     * SEO关键词
     * 
     * 关键词策略：
     * - 页面meta keywords标签
     * - 搜索引擎的关键词匹配
     * - 内部搜索的相关性提升
     * - 长尾关键词的覆盖
     */
    @Column(name = "seo_keywords", length = 500)
    @Size(max = 500, message = "SEO关键词长度不能超过500个字符")
    private String seoKeywords;

    /**
     * SEO描述
     * 
     * 描述用途：
     * - HTML页面的meta description
     * - 搜索结果的描述摘要
     * - 社交分享的描述内容
     * - 提升点击率的关键因素
     */
    @Column(name = "seo_description", length = 500)
    @Size(max = 500, message = "SEO描述长度不能超过500个字符")
    private String seoDescription;

    // ========== 统计和扩展字段 ==========

    /**
     * 商品数量
     * 
     * 统计信息：
     * - 当前分类下的商品总数
     * - 包含子分类的商品统计
     * - 分类权重的计算依据
     * - 分类删除的依赖检查
     * 
     * 维护策略：
     * - 商品增删时实时更新
     * - 定期任务校验一致性
     * - 缓存策略减少查询压力
     */
    @Column(name = "product_count", nullable = false)
    @Min(value = 0, message = "商品数量不能小于0")
    @Builder.Default
    private Long productCount = 0L;

    /**
     * 访问次数
     * 
     * 统计用途：
     * - 分类热度的量化指标
     * - 推荐算法的权重因子
     * - 运营分析的数据基础
     * - 分类优化的决策依据
     */
    @Column(name = "view_count", nullable = false)
    @Min(value = 0, message = "访问次数不能小于0")
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * 扩展属性JSON
     * 
     * 扩展用途：
     * - 分类的自定义属性
     * - 第三方集成的额外数据
     * - A/B测试的配置参数
     * - 动态配置的存储字段
     * 
     * 存储格式：
     * - JSON字符串格式
     * - 键值对结构
     * - 支持嵌套对象
     */
    @Column(name = "extra_attributes", columnDefinition = "TEXT")
    private String extraAttributes;

    // ========== 审计字段 ==========

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 更新者ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    // ========== 关联关系 ==========

    /**
     * 父分类对象
     * 
     * 关联关系：
     * - 多对一关系，多个子分类对应一个父分类
     * - 懒加载策略，按需加载父分类信息
     * - 用于分类树的向上遍历
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Category parent;

    /**
     * 子分类列表
     * 
     * 关联关系：
     * - 一对多关系，一个父分类对应多个子分类
     * - 懒加载策略，按需加载子分类列表
     * - 用于分类树的向下遍历
     * - 按排序号升序排列
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, name ASC")
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    // ========== 业务方法 ==========

    /**
     * 是否为根分类
     * 
     * @return true-根分类，false-非根分类
     */
    public boolean isRoot() {
        return parentId == null || level == 0;
    }

    /**
     * 是否为叶子分类（没有子分类）
     * 
     * @return true-叶子分类，false-有子分类
     */
    public boolean isLeaf() {
        return childrenCount == 0;
    }

    /**
     * 是否为激活状态
     * 
     * @return true-激活，false-未激活
     */
    public boolean isActive() {
        return CategoryStatus.ACTIVE.equals(status);
    }

    /**
     * 是否已删除
     * 
     * @return true-已删除，false-未删除
     */
    public boolean isDeleted() {
        return CategoryStatus.DELETED.equals(status);
    }

    /**
     * 获取完整的分类名称路径
     * 用于面包屑导航和SEO优化
     * 
     * @return 分类路径字符串，如："服装 > 男装 > 衬衫"
     */
    public String getFullName() {
        if (parent == null) {
            return name;
        }
        return parent.getFullName() + " > " + name;
    }

    /**
     * 生成分类URL路径
     * 用于SEO友好的URL生成
     * 
     * @return URL路径，如："/category/men-clothing/shirts"
     */
    public String getUrlPath() {
        if (code == null) {
            return "/category/" + id;
        }
        
        if (parent == null) {
            return "/category/" + code;
        }
        
        return parent.getUrlPath() + "/" + code;
    }

    /**
     * 更新分类路径
     * 当父分类变更时调用此方法更新路径
     */
    public void updatePath() {
        if (parent == null) {
            this.path = "/" + id + "/";
        } else {
            this.path = parent.getPath() + id + "/";
        }
    }

    /**
     * 增加子分类数量
     */
    public void incrementChildrenCount() {
        this.childrenCount++;
    }

    /**
     * 减少子分类数量
     */
    public void decrementChildrenCount() {
        if (this.childrenCount > 0) {
            this.childrenCount--;
        }
    }

    /**
     * 增加商品数量
     * 
     * @param count 增加的数量
     */
    public void incrementProductCount(Long count) {
        this.productCount += count;
    }

    /**
     * 减少商品数量
     * 
     * @param count 减少的数量
     */
    public void decrementProductCount(Long count) {
        if (this.productCount >= count) {
            this.productCount -= count;
        } else {
            this.productCount = 0L;
        }
    }

    /**
     * 增加访问次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 检查是否可以删除
     * 有子分类或商品的分类不能删除
     * 
     * @return true-可以删除，false-不能删除
     */
    public boolean canDelete() {
        return childrenCount == 0 && productCount == 0;
    }

    /**
     * 软删除分类
     */
    public void softDelete() {
        this.status = CategoryStatus.DELETED;
    }

    /**
     * 恢复分类
     */
    public void restore() {
        this.status = CategoryStatus.ACTIVE;
    }

    /**
     * 禁用分类
     */
    public void disable() {
        this.status = CategoryStatus.INACTIVE;
    }

    /**
     * 启用分类
     */
    public void enable() {
        this.status = CategoryStatus.ACTIVE;
    }

    // ========== 重写方法 ==========

    /**
     * 重写equals方法
     * 基于ID进行比较，避免在集合操作中出现问题
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Category category = (Category) obj;
        return id != null && id.equals(category.id);
    }

    /**
     * 重写hashCode方法
     * 基于ID计算哈希码
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 重写toString方法
     * 提供友好的字符串表示
     */
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", level=" + level +
                ", status=" + status +
                ", childrenCount=" + childrenCount +
                ", productCount=" + productCount +
                '}';
    }
}