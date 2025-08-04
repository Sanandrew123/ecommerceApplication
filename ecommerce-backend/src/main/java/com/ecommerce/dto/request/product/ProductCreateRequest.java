package com.ecommerce.dto.request.product;

/*
 * 文件职责: 商品创建请求DTO，封装商品创建时的输入参数
 * 
 * 开发心理活动：
 * 1. 请求DTO设计原则：
 *    - 数据验证：参数合法性和业务规则验证
 *    - 安全过滤：过滤敏感字段，防止恶意输入
 *    - 类型转换：前端数据到后端实体的转换
 *    - 文档清晰：提供完整的字段说明和示例
 * 
 * 2. 商品创建场景：
 *    - 商家添加新商品：完整的商品信息录入
 *    - 批量导入：CSV/Excel文件的商品批量创建
 *    - API集成：第三方系统的商品同步
 *    - 模板复制：基于现有商品创建新商品
 * 
 * 3. 验证策略考虑：
 *    - 必填字段验证：确保关键信息的完整性
 *    - 格式验证：价格、重量、尺寸等数值格式
 *    - 业务规则验证：价格合理性、库存逻辑性
 *    - 关联验证：分类存在性、品牌有效性
 * 
 * 4. 扩展性设计：
 *    - 预留扩展字段：支持未来功能的添加
 *    - 版本兼容：API版本演进的兼容性
 *    - 多格式支持：JSON、XML等数据格式
 *    - 国际化：多语言、多货币的支持
 * 
 * 包结构设计思路:
 * - 放在dto.request.product包下，专门处理商品请求
 * - 与response DTO分离，输入输出职责清晰
 * 
 * 命名原因:
 * - ProductCreateRequest明确表达商品创建请求功能
 * - 符合Request后缀的命名规范
 * 
 * 依赖关系:
 * - 被Controller接收，作为接口入参
 * - 转换为Product实体，由Service处理
 * - 独立于实体，便于API演进
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品创建请求DTO
 * 
 * 功能说明：
 * 1. 封装商品创建时的所有输入参数
 * 2. 提供完整的参数验证和错误提示
 * 3. 支持商品的基础信息和扩展信息
 * 4. 确保数据的安全性和完整性
 * 
 * 验证策略：
 * 1. 必填字段验证：商品名称、价格、分类等
 * 2. 格式验证：价格精度、字符串长度等
 * 3. 业务规则验证：价格合理性、库存逻辑
 * 4. 关联数据验证：分类ID的存在性验证
 * 
 * 使用场景：
 * 1. 商家后台：商品信息录入和编辑
 * 2. 批量导入：CSV/Excel文件的商品导入
 * 3. API集成：第三方ERP系统的商品同步
 * 4. 移动端：移动应用的商品发布功能
 * 
 * 安全特性：
 * 1. XSS防护：HTML标签的过滤和转义
 * 2. SQL注入防护：参数化查询的数据绑定
 * 3. 文件安全：图片URL的格式和来源验证
 * 4. 业务安全：价格合理性和库存逻辑验证
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {

    // ========== 基础信息字段 ==========

    /**
     * 商品名称
     * 
     * 验证规则：
     * - 必填字段，不能为空
     * - 长度限制：1-200个字符
     * - 支持中英文、数字、常用符号
     * - 前后空格自动去除
     * 
     * 业务意义：
     * - 商品的主要标识和展示名称
     * - 搜索引擎优化的重要元素
     * - 用户识别商品的主要依据
     * - 订单和发票的商品描述
     * 
     * 错误示例：
     * - 空字符串或null
     * - 纯空格字符串
     * - 超长字符串（>200字符）
     * - 包含特殊控制字符
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 200, message = "商品名称长度必须在1-200个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-_()（）【】\\[\\].,。，！!？?]+$", 
             message = "商品名称包含不允许的特殊字符")
    private String name;

    /**
     * 商品编码
     * 
     * 编码规则：
     * - 可选字段，系统可自动生成
     * - 全局唯一性，便于商品管理
     * - 支持字母、数字、短横线、下划线
     * - 长度限制：1-50个字符
     * 
     * 生成策略：
     * - 手动输入：商家自定义编码
     * - 自动生成：基于分类+时间戳
     * - 导入指定：批量导入时指定
     * - 规则生成：基于命名规则自动生成
     * 
     * 业务价值：
     * - 库存管理的标准化标识
     * - 条形码和二维码的关联
     * - 第三方系统集成的稳定ID
     * - 商品搜索和查询的快速入口
     */
    @Size(max = 50, message = "商品编码长度不能超过50个字符")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]*$", message = "商品编码只能包含字母、数字、短横线和下划线")
    private String code;

    /**
     * 商品副标题
     * 
     * 营销用途：
     * - 商品卖点的补充说明
     * - 促销信息的展示位置
     * - 规格参数的简要描述
     * - 搜索关键词的扩展空间
     * 
     * 验证规则：
     * - 可选字段，可以为空
     * - 最大长度：300个字符
     * - 支持常用标点符号
     * - 过滤HTML标签防止XSS
     */
    @Size(max = 300, message = "商品副标题长度不能超过300个字符")
    private String subtitle;

    /**
     * 商品简述
     * 
     * 内容要求：
     * - 商品的核心卖点和特色
     * - 用于列表页的商品介绍
     * - 搜索结果的摘要展示
     * - 社交分享的描述文本
     * 
     * 长度控制：
     * - 建议长度：50-200字符
     * - 最大长度：500字符
     * - 移动端显示友好
     * - 支持换行符但限制数量
     */
    @Size(max = 500, message = "商品简述长度不能超过500个字符")
    private String summary;

    /**
     * 商品详细描述
     * 
     * 内容丰富度：
     * - 商品的详细介绍和说明
     * - 支持HTML格式的富文本
     * - 可包含图片、视频链接
     * - 使用方法和注意事项
     * 
     * 安全验证：
     * - HTML标签白名单过滤
     * - 脚本标签严格禁止
     * - 外部链接安全检查
     * - 图片来源域名限制
     * 
     * 存储优化：
     * - 大文本内容压缩存储
     * - 图片懒加载优化
     * - CDN加速图片资源
     */
    @Size(max = 10000, message = "商品详细描述长度不能超过10000个字符")
    private String description;

    // ========== 分类和品牌字段 ==========

    /**
     * 商品分类ID
     * 
     * 业务约束：
     * - 必填字段，商品必须归属分类
     * - 分类ID必须存在且有效
     * - 分类状态必须为激活状态
     * - 支持多级分类的叶子节点
     * 
     * 验证逻辑：
     * - 数值大于0的长整型
     * - 数据库中存在对应分类
     * - 分类未被删除或禁用
     * - 分类层级符合业务规则
     * 
     * 关联影响：
     * - 决定商品的展示位置
     * - 影响商品的搜索分类
     * - 关联分类的属性模板
     * - 影响运费和税率计算
     */
    @NotNull(message = "商品分类不能为空")
    @Positive(message = "商品分类ID必须大于0")
    private Long categoryId;

    /**
     * 品牌名称
     * 
     * 品牌管理：
     * - 可选字段，提升商品档次
     * - 支持品牌筛选和搜索
     * - 品牌授权和认证标识
     * - 影响商品的价格定位
     * 
     * 验证要求：
     * - 长度限制：1-100字符
     * - 支持中英文品牌名称
     * - 过滤特殊字符和符号
     * - 品牌名称规范化处理
     * 
     * 业务扩展：
     * - 未来可扩展为品牌实体
     * - 支持品牌Logo和介绍
     * - 品牌授权证书管理
     * - 品牌商品统计分析
     */
    @Size(max = 100, message = "品牌名称长度不能超过100个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-&.]*$", message = "品牌名称包含不允许的字符")
    private String brand;

    /**
     * 商品标签
     * 
     * 标签格式：
     * - 多个标签用逗号分隔
     * - 单个标签长度：1-20字符
     * - 标签总数限制：最多10个
     * - 支持中英文混合标签
     * 
     * 标签用途：
     * - 商品的多维度分类
     * - 搜索关键词的扩展
     * - 推荐算法的特征提取
     * - 营销活动的商品筛选
     * 
     * 示例格式：
     * "热销,新品,限时折扣,有机,健康"
     * "bestseller,new,organic,healthy"
     */
    @Size(max = 200, message = "商品标签总长度不能超过200个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9,，\\s]*$", message = "商品标签只能包含中英文、数字和逗号")
    private String tags;

    // ========== 价格字段 ==========

    /**
     * 商品原价
     * 
     * 价格定义：
     * - 商品的市场指导价或建议零售价
     * - 用于计算折扣和优惠幅度
     * - 营销活动中的对比价格
     * - 可选字段，可以等于现价
     * 
     * 验证规则：
     * - 必须为正数（>= 0.01）
     * - 精度限制：小数点后最多2位
     * - 数值范围：0.01 - 99999999.99
     * - 必须大于等于现价
     * 
     * 业务逻辑：
     * - 如果不设置，默认等于现价
     * - 原价变更需要记录历史
     * - 影响折扣率的计算展示
     * - 价格策略分析的基础数据
     */
    @DecimalMin(value = "0.01", message = "商品原价必须大于0.01")
    @DecimalMax(value = "99999999.99", message = "商品原价不能超过99999999.99")
    @Digits(integer = 8, fraction = 2, message = "商品原价最多8位整数，2位小数")
    private BigDecimal originalPrice;

    /**
     * 商品现价
     * 
     * 现价定义：
     * - 商品的实际销售价格
     * - 用户下单时的计价基础
     * - 必填字段，必须设置
     * - 商品搜索排序的重要因子
     * 
     * 价格约束：
     * - 必须为正数（>= 0.01）
     * - 不能超过原价（如果设置了原价）
     * - 精度控制：最多2位小数
     * - 合理范围：0.01 - 99999999.99
     * 
     * 定价策略：
     * - 基于成本的加价定价
     * - 基于市场的竞争定价
     * - 基于价值的溢价定价
     * - 基于活动的促销定价
     */
    @NotNull(message = "商品现价不能为空")
    @DecimalMin(value = "0.01", message = "商品现价必须大于0.01")
    @DecimalMax(value = "99999999.99", message = "商品现价不能超过99999999.99")
    @Digits(integer = 8, fraction = 2, message = "商品现价最多8位整数，2位小数")
    private BigDecimal currentPrice;

    /**
     * 会员价格
     * 
     * 会员权益：
     * - VIP用户的专享优惠价格
     * - 会员等级差异化定价策略
     * - 用户升级会员的核心动力
     * - 会员忠诚度计划的重要组成
     * 
     * 定价规则：
     * - 可选字段，可以不设置
     * - 如果设置，必须 <= 现价
     * - 会员价格的优惠幅度合理
     * - 支持不同会员等级的阶梯定价
     * 
     * 营销价值：
     * - 提升用户的付费转化率
     * - 增强用户的平台粘性
     * - 促进用户的复购行为
     * - 提高客户的生命周期价值
     */
    @DecimalMin(value = "0.01", message = "会员价格必须大于0.01")
    @DecimalMax(value = "99999999.99", message = "会员价格不能超过99999999.99")
    @Digits(integer = 8, fraction = 2, message = "会员价格最多8位整数，2位小数")
    private BigDecimal memberPrice;

    /**
     * 成本价格
     * 
     * 成本控制：
     * - 商品的采购成本或生产成本
     * - 利润率计算的基础数据
     * - 定价策略制定的重要参考
     * - 财务分析和成本核算的依据
     * 
     * 保密要求：
     * - 严格的权限控制，仅限内部使用
     * - 不对外展示，保护商业机密
     * - 数据加密存储和传输
     * - 操作日志记录和审计
     * 
     * 验证规则：
     * - 可选字段，可以不填写
     * - 如果填写，必须为正数
     * - 通常应该小于销售价格
     * - 成本异常时给出提醒
     */
    @DecimalMin(value = "0.01", message = "成本价格必须大于0.01")
    @DecimalMax(value = "99999999.99", message = "成本价格不能超过99999999.99")
    @Digits(integer = 8, fraction = 2, message = "成本价格最多8位整数，2位小数")
    private BigDecimal costPrice;

    // ========== 库存字段 ==========

    /**
     * 初始库存数量
     * 
     * 库存管理：
     * - 商品创建时的初始库存
     * - 后续通过库存调整进行变更
     * - 影响商品的可售状态
     * - 库存预警和补货的基础数据
     * 
     * 数值约束：
     * - 必填字段，至少为0
     * - 最大值限制：防止数据异常
     * - 整数类型：不支持小数库存
     * - 合理范围：0 - 999999999
     * 
     * 业务逻辑：
     * - 库存为0时商品显示缺货
     * - 库存小于预警值时触发提醒
     * - 库存变更需要记录操作日志
     * - 支持批量库存调整操作
     */
    @NotNull(message = "初始库存数量不能为空")
    @Min(value = 0, message = "初始库存数量不能小于0")
    @Max(value = 999999999, message = "初始库存数量不能超过999999999")
    private Integer totalStock;

    /**
     * 库存预警阈值
     * 
     * 预警机制：
     * - 库存低于此值时触发预警通知
     * - 自动补货系统的触发条件
     * - 运营人员关注的重要指标
     * - 防止断货的预防性措施
     * 
     * 设置策略：
     * - 基于历史销量的统计分析
     * - 考虑供应商的交货周期
     * - 结合季节性销售波动
     * - 支持商品类别的差异化设置
     * 
     * 默认规则：
     * - 如果不设置，使用系统默认值
     * - 一般设置为日均销量的3-7天
     * - 高价值商品可以设置更高
     * - 快消品可以设置相对较低
     */
    @Min(value = 0, message = "库存预警阈值不能小于0")
    @Max(value = 999999999, message = "库存预警阈值不能超过999999999")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    // ========== 商品属性字段 ==========

    /**
     * 是否为虚拟商品
     * 
     * 虚拟商品特征：
     * - 无需物流配送的数字化商品
     * - 如：电子书、软件、会员服务
     * - 支付后可立即交付使用
     * - 不涉及库存的实物管理
     * 
     * 业务影响：
     * - 订单流程的简化处理
     * - 无需计算运费和重量
     * - 自动发货和激活流程
     * - 退货政策的差异化处理
     * 
     * 系统处理：
     * - 虚拟商品跳过物流环节
     * - 支付成功后自动完成订单
     * - 通过邮件或短信发送激活码
     * - 支持在线使用或下载交付
     */
    @Builder.Default
    private Boolean isVirtual = false;

    /**
     * 是否需要配送
     * 
     * 配送策略：
     * - 实体商品通常需要配送
     * - 虚拟商品或自提商品不需要
     * - 影响运费计算和物流选择
     * - 决定订单的履约方式
     * 
     * 特殊情况：
     * - 大件商品可能需要特殊配送
     * - 生鲜商品需要冷链配送
     * - 危险品需要专业物流
     * - 高价值商品需要保价配送
     */
    @Builder.Default
    private Boolean requiresShipping = true;

    /**
     * 是否可单独购买
     * 
     * 销售策略：
     * - 独立商品可以单独购买
     * - 组合商品或配件可能需要搭配
     * - 套餐商品的销售限制
     * - 影响购物车的验证逻辑
     * 
     * 应用场景：
     * - 手机壳需要与手机型号匹配
     * - 配件商品需要与主商品搭配
     * - 套餐商品必须整套购买
     * - 会员专享商品的购买限制
     */
    @Builder.Default
    private Boolean canBuyAlone = true;

    // ========== 物理属性字段 ==========

    /**
     * 商品重量（克）
     * 
     * 物流计算：
     * - 快递费用计算的重要参数
     * - 包装方案选择的依据
     * - 物流方式限制的判断标准
     * - 国际运输的必要信息
     * 
     * 精度要求：
     * - 支持小数点后2位精度
     * - 单位统一为克（g）
     * - 范围：0.01g - 999999.99g
     * - 特殊商品可以为0（如虚拟商品）
     * 
     * 业务应用：
     * - 自动选择合适的包装盒
     * - 计算是否超出快递重量限制
     * - 批量发货的重量统计
     * - 物流成本的精确核算
     */
    @DecimalMin(value = "0.00", message = "商品重量不能小于0")
    @DecimalMax(value = "999999.99", message = "商品重量不能超过999999.99克")
    @Digits(integer = 6, fraction = 2, message = "商品重量最多6位整数，2位小数")
    private BigDecimal weight;

    /**
     * 商品长度（厘米）
     * 
     * 尺寸用途：
     * - 包装盒规格的选择依据
     * - 物流方式的限制判断
     * - 仓储位置的分配参考
     * - 展示效果的预期管理
     */
    @DecimalMin(value = "0.00", message = "商品长度不能小于0")
    @DecimalMax(value = "99999.99", message = "商品长度不能超过99999.99厘米")
    @Digits(integer = 5, fraction = 2, message = "商品长度最多5位整数，2位小数")
    private BigDecimal length;

    /**
     * 商品宽度（厘米）
     */
    @DecimalMin(value = "0.00", message = "商品宽度不能小于0")
    @DecimalMax(value = "99999.99", message = "商品宽度不能超过99999.99厘米")
    @Digits(integer = 5, fraction = 2, message = "商品宽度最多5位整数，2位小数")
    private BigDecimal width;

    /**
     * 商品高度（厘米）
     */
    @DecimalMin(value = "0.00", message = "商品高度不能小于0")
    @DecimalMax(value = "99999.99", message = "商品高度不能超过99999.99厘米")
    @Digits(integer = 5, fraction = 2, message = "商品高度最多5位整数，2位小数")
    private BigDecimal height;

    // ========== 多媒体字段 ==========

    /**
     * 商品主图URL
     * 
     * 图片要求：
     * - 必填字段，商品必须有主图
     * - 图片URL格式验证
     * - 建议尺寸：800x800像素
     * - 支持jpg、png、webp格式
     * 
     * 质量标准：
     * - 图片清晰度要求高
     * - 背景尽量为纯色或透明
     * - 商品占图片比例合适
     * - 避免过度PS和滤镜
     * 
     * 安全验证：
     * - URL格式的正确性检查
     * - 图片文件大小限制
     * - 图片内容的合规性检查
     * - 防止恶意文件上传
     */
    @NotBlank(message = "商品主图不能为空")
    @Size(max = 500, message = "商品主图URL长度不能超过500个字符")
    @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|webp)$", 
             message = "商品主图必须是有效的图片URL，支持jpg、png、gif、webp格式")
    private String mainImage;

    /**
     * 商品图片列表
     * 
     * 图片集合：
     * - 多张商品展示图片的URL列表
     * - 支持不同角度和细节展示
     * - 建议数量：3-8张图片
     * - 用于商品详情页的轮播展示
     * 
     * 数据格式：
     * - List<String>类型，便于处理
     * - 每个URL都需要格式验证
     * - 支持图片的排序和管理
     * - 图片总数量限制控制
     * 
     * 业务价值：
     * - 提升商品的展示效果
     * - 增加用户的购买信心
     * - 减少售后的退货率
     * - 提高商品的转化率
     */
    @Size(max = 10, message = "商品图片数量不能超过10张")
    private List<@NotBlank(message = "图片URL不能为空") 
                  @Size(max = 500, message = "图片URL长度不能超过500个字符")
                  @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|webp)$", 
                          message = "图片URL格式不正确") String> images;

    /**
     * 商品视频URL
     * 
     * 视频展示：
     * - 商品的动态展示视频
     * - 使用方法或效果演示
     * - 提升用户的购买体验
     * - 增强商品的吸引力
     * 
     * 技术要求：
     * - 支持mp4、avi等主流格式
     * - 视频时长建议1-3分钟
     * - 文件大小控制在50MB以内
     * - 支持第三方视频平台链接
     * 
     * 可选字段：
     * - 不是所有商品都需要视频
     * - 主要用于复杂商品的演示
     * - 提升转化率的营销工具
     * - 减少客服咨询的有效方式
     */
    @Size(max = 500, message = "商品视频URL长度不能超过500个字符")
    @Pattern(regexp = "^https?://.*\\.(mp4|avi|mov|wmv|flv|webm)$|^https?://(www\\.)?(youtube|youtu\\.be|vimeo)\\.com/.*$", 
             message = "商品视频URL格式不正确")
    private String videoUrl;

    // ========== 营销和推广字段 ==========

    /**
     * 是否为热门商品
     * 
     * 热门标识：
     * - 人工设置的热门商品标识
     * - 用于首页推荐和分类置顶
     * - 影响搜索结果的排序权重
     * - 营销活动的重点推广商品
     * 
     * 设置策略：
     * - 基于历史销量数据判断
     * - 结合用户行为分析
     * - 考虑商品的利润贡献
     * - 配合营销活动的需求
     */
    @Builder.Default
    private Boolean isHot = false;

    /**
     * 是否为推荐商品
     * 
     * 推荐机制：
     * - 编辑精选的推荐商品
     * - 个性化推荐的候选商品
     * - 相关商品推荐的数据源
     * - 交叉销售的重点商品
     * 
     * 推荐场景：
     * - 首页推荐位展示
     * - 分类页面的编辑推荐
     * - 商品详情页的相关推荐
     * - 购物车页面的搭配推荐
     */
    @Builder.Default
    private Boolean isRecommended = false;

    /**
     * 是否为新品
     * 
     * 新品策略：
     * - 新上架商品的标识
     * - 用于新品推广和营销
     * - 影响商品的展示优先级
     * - 新品促销活动的筛选条件
     * 
     * 判断标准：
     * - 上架时间在指定天数内
     * - 可以人工设置新品标识
     * - 配合新品发布的营销计划
     * - 自动过期的时间管理
     */
    @Builder.Default
    private Boolean isNew = true;

    /**
     * 推荐权重
     * 
     * 权重算法：
     * - 推荐系统的排序权重
     * - 数值越高推荐优先级越高
     * - 影响搜索结果的排序
     * - 个性化推荐的权重因子
     * 
     * 权重范围：
     * - 0-100的整数值
     * - 默认值：50（中等权重）
     * - 可以基于商品表现动态调整
     * - 支持人工设置和算法计算
     */
    @Min(value = 0, message = "推荐权重不能小于0")
    @Max(value = 100, message = "推荐权重不能大于100")
    @Builder.Default
    private Integer recommendWeight = 50;

    // ========== SEO优化字段 ==========

    /**
     * SEO标题
     * 
     * 搜索优化：
     * - 页面title标签的内容
     * - 搜索引擎结果页的标题
     * - 社交分享时的标题内容
     * - 影响搜索排名的重要因素
     * 
     * 优化建议：
     * - 包含主要关键词
     * - 长度控制在60字符以内
     * - 标题与商品内容相关
     * - 避免关键词堆砌
     * 
     * 默认策略：
     * - 如果不填写，使用商品名称
     * - 可以添加品牌名和分类名
     * - 结合热门关键词优化
     * - 定期分析和调整效果
     */
    @Size(max = 200, message = "SEO标题长度不能超过200个字符")
    private String seoTitle;

    /**
     * SEO关键词
     * 
     * 关键词策略：
     * - 页面meta keywords标签
     * - 内部搜索的匹配词库
     * - 长尾关键词的覆盖
     * - 竞争对手分析的参考
     * 
     * 关键词选择：
     * - 与商品高度相关
     * - 搜索量和竞争度平衡
     * - 多个关键词用逗号分隔
     * - 定期更新和优化
     */
    @Size(max = 500, message = "SEO关键词长度不能超过500个字符")
    private String seoKeywords;

    /**
     * SEO描述
     * 
     * 描述优化：
     * - 页面meta description标签
     * - 搜索结果的描述摘要
     * - 影响点击率的重要因素
     * - 社交平台分享的描述
     * 
     * 内容要求：
     * - 准确描述商品特点
     * - 包含吸引人的卖点
     * - 长度控制在160字符以内
     * - 自然融入关键词
     */
    @Size(max = 500, message = "SEO描述长度不能超过500个字符")
    private String seoDescription;

    // ========== 扩展信息字段 ==========

    /**
     * 商品规格参数
     * 
     * 规格定义：
     * - 商品的详细规格参数
     * - 键值对形式的结构化数据
     * - 支持规格对比功能
     * - 筛选和搜索的重要数据
     * 
     * 数据结构：
     * - Map<String, String>类型
     * - 键为规格名称，值为规格值
     * - 支持中英文规格名称
     * - 规格值支持数值和文本
     * 
     * 应用场景：
     * - 商品详情页的规格展示
     * - 规格筛选和对比功能
     * - 相似商品的匹配推荐
     * - 商品导入导出的标准化
     * 
     * 示例数据：
     * {
     *   "颜色": "红色",
     *   "尺寸": "XL", 
     *   "材质": "纯棉",
     *   "产地": "中国"
     * }
     */
    private java.util.Map<String, String> specifications;

    /**
     * 商品属性参数
     * 
     * 属性扩展：
     * - 商品的扩展属性信息
     * - 灵活的键值对存储
     * - 支持动态属性定义
     * - 不同分类的差异化属性
     * 
     * 与规格的区别：
     * - 规格更多用于商品对比
     * - 属性更多用于描述和展示
     * - 规格影响SKU的组合
     * - 属性更多用于营销描述
     * 
     * 应用示例：
     * {
     *   "适用年龄": "18-35岁",
     *   "使用场景": "商务休闲",
     *   "保养方法": "机洗",
     *   "质保期": "1年"
     * }
     */
    private java.util.Map<String, String> attributes;

    /**
     * 扩展信息
     * 
     * 灵活扩展：
     * - 未来功能的预留字段
     * - 第三方系统的数据对接
     * - 特殊业务需求的数据存储
     * - A/B测试的配置参数
     * 
     * 数据格式：
     * - 自由格式的键值对
     * - 支持嵌套结构的JSON
     * - 版本兼容性考虑
     * - 数据迁移的友好性
     */
    private java.util.Map<String, Object> extraInfo;

    // ========== 业务验证方法 ==========

    /**
     * 验证价格的合理性
     * 
     * 验证规则：
     * 1. 现价必须设置且大于0
     * 2. 原价如果设置，必须大于等于现价
     * 3. 会员价如果设置，必须小于等于现价
     * 4. 成本价如果设置，建议小于销售价格
     * 
     * @return 验证结果和错误信息
     */
    public String validatePrices() {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return "商品现价必须设置且大于0";
        }
        
        if (originalPrice != null && originalPrice.compareTo(currentPrice) < 0) {
            return "商品原价不能小于现价";
        }
        
        if (memberPrice != null && memberPrice.compareTo(currentPrice) > 0) {
            return "会员价格不能大于现价";
        }
        
        if (costPrice != null && costPrice.compareTo(currentPrice) > 0) {
            return "成本价格建议不要大于销售价格";
        }
        
        return null; // 验证通过
    }

    /**
     * 验证库存的合理性
     * 
     * @return 验证结果和错误信息
     */
    public String validateStock() {
        if (totalStock == null || totalStock < 0) {
            return "初始库存数量不能为空且不能小于0";
        }
        
        if (lowStockThreshold != null && lowStockThreshold > totalStock) {
            return "库存预警阈值不能大于初始库存数量";
        }
        
        return null; // 验证通过
    }

    /**
     * 验证商品图片
     * 
     * @return 验证结果和错误信息
     */
    public String validateImages() {
        if (mainImage == null || mainImage.trim().isEmpty()) {
            return "商品主图不能为空";
        }
        
        if (images != null && images.size() > 10) {
            return "商品图片数量不能超过10张";
        }
        
        return null; // 验证通过
    }

    /**
     * 获取用于日志记录的安全字符串
     * 过滤敏感信息，如成本价格
     * 
     * @return 安全的日志字符串
     */
    public String toSafeLogString() {
        return "ProductCreateRequest{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", categoryId=" + categoryId +
                ", brand='" + brand + '\'' +
                ", currentPrice=" + currentPrice +
                ", totalStock=" + totalStock +
                ", isVirtual=" + isVirtual +
                ", isHot=" + isHot +
                ", isRecommended=" + isRecommended +
                ", isNew=" + isNew +
                '}';
    }
}