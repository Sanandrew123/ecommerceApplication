# 电商应用开发项目规范

## 项目概述

这是一个基于Spring Boot 3.x + Java 21的现代化电商应用学习项目，集成了最新的AI技术栈，旨在构建企业级的电商解决方案。

### 技术栈

- **后端框架**: Spring Boot 3.x
- **Java版本**: Java 21 (LTS)
- **数据库**: MySQL 8.0 + Redis 7.0
- **AI集成**: Spring AI + LangChain4j
- **消息队列**: RabbitMQ
- **搜索引擎**: Elasticsearch
- **构建工具**: Maven 3.9+
- **API文档**: OpenAPI 3.0 (Swagger)

### 项目特点

1. **学习导向**: 专注于技术学习和实践，不追求生产部署
2. **AI集成**: 深度集成Spring AI和LangChain4j，探索AI在电商中的应用场景
3. **企业级架构**: 采用分层架构和领域驱动设计原则
4. **现代化技术**: 使用最新的Java特性和Spring生态技术
5. **完整文档**: 详细的代码注释和开发思路记录

## 完整项目结构

```
ecommerce-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ecommerce/
│   │   │           ├── EcommerceApplication.java                    # 启动类
│   │   │           ├── config/                                      # 配置包
│   │   │           │   ├── security/                               # 安全配置
│   │   │           │   │   ├── SecurityConfig.java                 # Spring Security配置
│   │   │           │   │   ├── JwtAuthenticationFilter.java        # JWT过滤器
│   │   │           │   │   ├── JwtAuthenticationEntryPoint.java    # JWT认证入口
│   │   │           │   │   └── CustomUserDetailsService.java       # 用户详情服务
│   │   │           │   ├── redis/                                  # Redis配置
│   │   │           │   │   ├── RedisConfig.java                    # Redis基础配置
│   │   │           │   │   └── CacheConfig.java                    # 缓存配置
│   │   │           │   ├── database/                               # 数据库配置
│   │   │           │   │   ├── JpaConfig.java                      # JPA配置
│   │   │           │   │   └── DruidConfig.java                    # 数据库连接池配置
│   │   │           │   ├── web/                                    # Web配置
│   │   │           │   │   ├── WebConfig.java                      # Web基础配置
│   │   │           │   │   ├── CorsConfig.java                     # 跨域配置
│   │   │           │   │   └── Jackson2Config.java                 # JSON序列化配置
│   │   │           │   ├── mq/                                     # 消息队列配置
│   │   │           │   │   ├── RabbitConfig.java                   # RabbitMQ配置
│   │   │           │   │   └── MessageProducer.java                # 消息生产者
│   │   │           │   ├── elasticsearch/                          # 搜索配置
│   │   │           │   │   └── ElasticsearchConfig.java            # ES配置
│   │   │           │   ├── payment/                                # 支付配置
│   │   │           │   │   ├── AlipayConfig.java                   # 支付宝配置
│   │   │           │   │   └── WechatPayConfig.java                # 微信支付配置
│   │   │           │   └── file/                                   # 文件配置
│   │   │           │       ├── FileUploadConfig.java               # 文件上传配置
│   │   │           │       └── OssConfig.java                      # OSS配置
│   │   │           ├── controller/                                  # 控制器包
│   │   │           │   ├── admin/                                  # 管理端控制器
│   │   │           │   │   ├── AdminUserController.java            # 管理员用户管理
│   │   │           │   │   ├── AdminProductController.java         # 管理员商品管理
│   │   │           │   │   ├── AdminOrderController.java           # 管理员订单管理
│   │   │           │   │   ├── AdminCategoryController.java        # 管理员分类管理
│   │   │           │   │   └── AdminStatisticsController.java      # 管理员统计管理
│   │   │           │   ├── api/                                    # 前台API控制器
│   │   │           │   │   ├── AuthController.java                 # 认证控制器
│   │   │           │   │   ├── UserController.java                 # 用户控制器
│   │   │           │   │   ├── ProductController.java              # 商品控制器
│   │   │           │   │   ├── CategoryController.java             # 分类控制器
│   │   │           │   │   ├── CartController.java                 # 购物车控制器
│   │   │           │   │   ├── OrderController.java                # 订单控制器
│   │   │           │   │   ├── PaymentController.java              # 支付控制器
│   │   │           │   │   ├── SearchController.java               # 搜索控制器
│   │   │           │   │   └── FileController.java                 # 文件控制器
│   │   │           │   └── common/                                 # 通用控制器
│   │   │           │       ├── HealthController.java               # 健康检查
│   │   │           │       └── CommonController.java               # 通用接口
│   │   │           ├── service/                                     # 服务层包
│   │   │           │   ├── impl/                                   # 服务实现类
│   │   │           │   │   ├── UserServiceImpl.java                # 用户服务实现
│   │   │           │   │   ├── ProductServiceImpl.java             # 商品服务实现
│   │   │           │   │   ├── CategoryServiceImpl.java            # 分类服务实现
│   │   │           │   │   ├── CartServiceImpl.java                # 购物车服务实现
│   │   │           │   │   ├── OrderServiceImpl.java               # 订单服务实现
│   │   │           │   │   ├── PaymentServiceImpl.java             # 支付服务实现
│   │   │           │   │   ├── SearchServiceImpl.java              # 搜索服务实现
│   │   │           │   │   ├── FileServiceImpl.java                # 文件服务实现
│   │   │           │   │   └── EmailServiceImpl.java               # 邮件服务实现
│   │   │           │   ├── UserService.java                        # 用户服务接口
│   │   │           │   ├── ProductService.java                     # 商品服务接口
│   │   │           │   ├── CategoryService.java                    # 分类服务接口
│   │   │           │   ├── CartService.java                        # 购物车服务接口
│   │   │           │   ├── OrderService.java                       # 订单服务接口
│   │   │           │   ├── PaymentService.java                     # 支付服务接口
│   │   │           │   ├── SearchService.java                      # 搜索服务接口
│   │   │           │   ├── FileService.java                        # 文件服务接口
│   │   │           │   └── EmailService.java                       # 邮件服务接口
│   │   │           ├── repository/                                  # 数据访问层包
│   │   │           │   ├── jpa/                                    # JPA仓库
│   │   │           │   │   ├── UserRepository.java                 # 用户仓库
│   │   │           │   │   ├── ProductRepository.java              # 商品仓库
│   │   │           │   │   ├── CategoryRepository.java             # 分类仓库
│   │   │           │   │   ├── OrderRepository.java                # 订单仓库
│   │   │           │   │   ├── OrderItemRepository.java            # 订单项仓库
│   │   │           │   │   ├── PaymentRepository.java              # 支付仓库
│   │   │           │   │   └── UserAddressRepository.java          # 用户地址仓库
│   │   │           │   ├── elasticsearch/                          # ES仓库
│   │   │           │   │   └── ProductSearchRepository.java        # 商品搜索仓库
│   │   │           │   └── redis/                                  # Redis仓库
│   │   │           │       ├── CartRedisRepository.java            # 购物车Redis仓库
│   │   │           │       └── CacheRepository.java                # 缓存仓库
│   │   │           ├── entity/                                      # 实体类包
│   │   │           │   ├── base/                                   # 基础实体
│   │   │           │   │   ├── BaseEntity.java                     # 基础实体类
│   │   │           │   │   └── TreeEntity.java                     # 树形实体类
│   │   │           │   ├── User.java                               # 用户实体
│   │   │           │   ├── Product.java                            # 商品实体
│   │   │           │   ├── Category.java                           # 分类实体
│   │   │           │   ├── Order.java                              # 订单实体
│   │   │           │   ├── OrderItem.java                          # 订单项实体
│   │   │           │   ├── Payment.java                            # 支付实体
│   │   │           │   ├── UserAddress.java                        # 用户地址实体
│   │   │           │   ├── ProductImage.java                       # 商品图片实体
│   │   │           │   └── OperationLog.java                       # 操作日志实体
│   │   │           ├── dto/                                         # 数据传输对象包
│   │   │           │   ├── request/                                # 请求DTO
│   │   │           │   │   ├── auth/                               # 认证相关请求
│   │   │           │   │   │   ├── LoginRequest.java               # 登录请求
│   │   │           │   │   │   ├── RegisterRequest.java            # 注册请求
│   │   │           │   │   │   └── ChangePasswordRequest.java      # 修改密码请求
│   │   │           │   │   ├── user/                               # 用户相关请求
│   │   │           │   │   │   ├── UserUpdateRequest.java          # 用户更新请求
│   │   │           │   │   │   └── AddressRequest.java             # 地址请求
│   │   │           │   │   ├── product/                            # 商品相关请求
│   │   │           │   │   │   ├── ProductCreateRequest.java       # 商品创建请求
│   │   │           │   │   │   ├── ProductUpdateRequest.java       # 商品更新请求
│   │   │           │   │   │   └── ProductSearchRequest.java       # 商品搜索请求
│   │   │           │   │   ├── order/                              # 订单相关请求
│   │   │           │   │   │   ├── OrderCreateRequest.java         # 订单创建请求
│   │   │           │   │   │   └── OrderItemRequest.java           # 订单项请求
│   │   │           │   │   ├── cart/                               # 购物车相关请求
│   │   │           │   │   │   └── CartItemRequest.java            # 购物车项请求
│   │   │           │   │   └── payment/                            # 支付相关请求
│   │   │           │   │       └── PaymentRequest.java             # 支付请求
│   │   │           │   ├── response/                               # 响应DTO
│   │   │           │   │   ├── auth/                               # 认证相关响应
│   │   │           │   │   │   └── LoginResponse.java              # 登录响应
│   │   │           │   │   ├── user/                               # 用户相关响应
│   │   │           │   │   │   └── UserInfoResponse.java           # 用户信息响应
│   │   │           │   │   ├── product/                            # 商品相关响应
│   │   │           │   │   │   ├── ProductDetailResponse.java      # 商品详情响应
│   │   │           │   │   │   └── ProductListResponse.java        # 商品列表响应
│   │   │           │   │   ├── order/                              # 订单相关响应
│   │   │           │   │   │   ├── OrderDetailResponse.java        # 订单详情响应
│   │   │           │   │   │   └── OrderListResponse.java          # 订单列表响应
│   │   │           │   │   └── cart/                               # 购物车相关响应
│   │   │           │   │       └── CartResponse.java               # 购物车响应
│   │   │           │   └── common/                                 # 通用DTO
│   │   │           │       ├── PageRequest.java                    # 分页请求
│   │   │           │       ├── PageResult.java                     # 分页结果
│   │   │           │       ├── ApiResponse.java                    # 统一响应
│   │   │           │       └── TreeNode.java                       # 树形节点
│   │   │           ├── enums/                                       # 枚举包
│   │   │           │   ├── UserStatus.java                         # 用户状态枚举
│   │   │           │   ├── OrderStatus.java                        # 订单状态枚举
│   │   │           │   ├── PaymentStatus.java                      # 支付状态枚举
│   │   │           │   ├── PaymentMethod.java                      # 支付方式枚举
│   │   │           │   └── ResponseCode.java                       # 响应码枚举
│   │   │           ├── exception/                                   # 异常处理包
│   │   │           │   ├── GlobalExceptionHandler.java             # 全局异常处理器
│   │   │           │   ├── BusinessException.java                  # 业务异常
│   │   │           │   ├── AuthenticationException.java            # 认证异常
│   │   │           │   └── ValidationException.java                # 验证异常
│   │   │           ├── utils/                                       # 工具类包
│   │   │           │   ├── jwt/                                    # JWT工具
│   │   │           │   │   ├── JwtUtil.java                        # JWT工具类
│   │   │           │   │   └── JwtConstants.java                   # JWT常量
│   │   │           │   ├── security/                               # 安全工具
│   │   │           │   │   ├── PasswordUtil.java                   # 密码工具
│   │   │           │   │   └── SecurityUtil.java                   # 安全工具
│   │   │           │   ├── redis/                                  # Redis工具
│   │   │           │   │   ├── RedisUtil.java                      # Redis工具类
│   │   │           │   │   └── CacheKeyUtil.java                   # 缓存键工具
│   │   │           │   ├── file/                                   # 文件工具
│   │   │           │   │   ├── FileUtil.java                       # 文件工具类
│   │   │           │   │   ├── ImageUtil.java                      # 图片工具类
│   │   │           │   │   └── OssUtil.java                        # OSS工具类
│   │   │           │   ├── common/                                 # 通用工具
│   │   │           │   │   ├── DateUtil.java                       # 日期工具类
│   │   │           │   │   ├── StringUtil.java                     # 字符串工具类
│   │   │           │   │   ├── JsonUtil.java                       # JSON工具类
│   │   │           │   │   ├── BeanUtil.java                       # Bean工具类
│   │   │           │   │   ├── ValidationUtil.java                 # 验证工具类
│   │   │           │   │   └── HttpUtil.java                       # HTTP工具类
│   │   │           │   ├── order/                                  # 订单工具
│   │   │           │   │   ├── OrderNoGenerator.java               # 订单号生成器
│   │   │           │   │   └── OrderStatusUtil.java                # 订单状态工具
│   │   │           │   └── payment/                                # 支付工具
│   │   │           │       ├── AlipayUtil.java                     # 支付宝工具
│   │   │           │       ├── WechatPayUtil.java                  # 微信支付工具
│   │   │           │       └── SignUtil.java                       # 签名工具
│   │   │           ├── converter/                                   # 转换器包
│   │   │           │   ├── JsonConverter.java                      # JSON转换器
│   │   │           │   ├── DateConverter.java                      # 日期转换器
│   │   │           │   └── EnumConverter.java                      # 枚举转换器
│   │   │           ├── validator/                                   # 验证器包
│   │   │           │   ├── annotation/                             # 自定义注解
│   │   │           │   │   ├── Phone.java                          # 手机号验证注解
│   │   │           │   │   ├── IdCard.java                         # 身份证验证注解
│   │   │           │   │   └── EnumValue.java                      # 枚举值验证注解
│   │   │           │   └── impl/                                   # 验证器实现
│   │   │           │       ├── PhoneValidator.java                 # 手机号验证器
│   │   │           │       ├── IdCardValidator.java                # 身份证验证器
│   │   │           │       └── EnumValueValidator.java             # 枚举值验证器
│   │   │           ├── aspect/                                      # 切面包
│   │   │           │   ├── LogAspect.java                          # 日志切面
│   │   │           │   ├── CacheAspect.java                        # 缓存切面
│   │   │           │   ├── RateLimitAspect.java                    # 限流切面
│   │   │           │   └── PermissionAspect.java                   # 权限切面
│   │   │           ├── listener/                                    # 监听器包
│   │   │           │   ├── mq/                                     # 消息队列监听器
│   │   │           │   │   ├── OrderEventListener.java             # 订单事件监听器
│   │   │           │   │   ├── PaymentEventListener.java           # 支付事件监听器
│   │   │           │   │   ├── EmailEventListener.java             # 邮件事件监听器
│   │   │           │   │   └── StockEventListener.java             # 库存事件监听器
│   │   │           │   └── event/                                  # 事件监听器
│   │   │           │       ├── ApplicationStartListener.java       # 应用启动监听器
│   │   │           │       └── UserLoginListener.java              # 用户登录监听器
│   │   │           ├── scheduled/                                   # 定时任务包
│   │   │           │   ├── OrderScheduledTask.java                 # 订单定时任务
│   │   │           │   ├── CacheScheduledTask.java                 # 缓存定时任务
│   │   │           │   ├── StatisticsScheduledTask.java            # 统计定时任务
│   │   │           │   └── CleanupScheduledTask.java               # 清理定时任务
│   │   │           ├── interceptor/                                 # 拦截器包
│   │   │           │   ├── AuthInterceptor.java                    # 认证拦截器
│   │   │           │   ├── RateLimitInterceptor.java               # 限流拦截器
│   │   │           │   └── LogInterceptor.java                     # 日志拦截器
│   │   │           └── constants/                                   # 常量包
│   │   │               ├── CacheConstants.java                     # 缓存常量
│   │   │               ├── SecurityConstants.java                  # 安全常量
│   │   │               ├── OrderConstants.java                     # 订单常量
│   │   │               ├── PaymentConstants.java                   # 支付常量
│   │   │               ├── FileConstants.java                      # 文件常量
│   │   │               └── CommonConstants.java                    # 通用常量
│   │   └── resources/
│   │       ├── application.yml                                     # 主配置文件
│   │       ├── application-dev.yml                                 # 开发环境配置
│   │       ├── application-test.yml                                # 测试环境配置
│   │       ├── application-prod.yml                                # 生产环境配置
│   │       ├── db/                                                 # 数据库相关
│   │       │   ├── migration/                                      # 数据库迁移脚本
│   │       │   │   ├── V1__init_user_tables.sql                   # 初始化用户表
│   │       │   │   ├── V2__init_product_tables.sql                # 初始化商品表
│   │       │   │   ├── V3__init_order_tables.sql                  # 初始化订单表
│   │       │   │   ├── V4__init_payment_tables.sql                # 初始化支付表
│   │       │   │   └── V5__add_indexes.sql                        # 添加索引
│   │       │   └── data/                                          # 初始化数据
│   │       │       ├── categories.sql                             # 分类初始化数据
│   │       │       ├── admin_users.sql                            # 管理员初始化数据
│   │       │       └── demo_products.sql                          # 演示商品数据
│   │       ├── elasticsearch/                                      # ES配置
│   │       │   ├── mappings/                                      # 索引映射
│   │       │   │   └── product_mapping.json                       # 商品索引映射
│   │       │   └── settings/                                      # 索引设置
│   │       │       └── product_setting.json                       # 商品索引设置
│   │       ├── templates/                                          # 模板文件
│   │       │   ├── email/                                         # 邮件模板
│   │       │   │   ├── register_success.html                      # 注册成功邮件
│   │       │   │   ├── order_confirm.html                         # 订单确认邮件
│   │       │   │   └── payment_success.html                       # 支付成功邮件
│   │       │   └── sms/                                           # 短信模板
│   │       │       ├── login_code.txt                             # 登录验证码
│   │       │       └── order_status.txt                           # 订单状态通知
│   │       ├── static/                                            # 静态资源
│   │       │   ├── images/                                        # 图片资源
│   │       │   │   ├── default/                                   # 默认图片
│   │       │   │   │   ├── avatar.png                             # 默认头像
│   │       │   │   │   └── product.png                            # 默认商品图片
│   │       │   │   └── icons/                                     # 图标
│   │       │   │       └── logo.png                               # 系统Logo
│   │       │   ├── css/                                           # 样式文件
│   │       │   │   └── admin.css                                  # 管理后台样式
│   │       │   └── js/                                            # JavaScript文件
│   │       │       └── common.js                                  # 通用JS
│   │       ├── config/                                            # 配置文件
│   │       │   ├── logback-spring.xml                             # 日志配置
│   │       │   ├── mybatis-config.xml                             # MyBatis配置
│   │       │   └── ehcache.xml                                    # EhCache配置
│   │       └── i18n/                                              # 国际化资源
│   │           ├── messages.properties                            # 默认消息
│   │           ├── messages_zh_CN.properties                      # 中文消息
│   │           └── messages_en_US.properties                      # 英文消息
│   └── test/
│       └── java/
│           └── com/
│               └── ecommerce/
│                   ├── controller/                                # 控制器测试
│                   │   ├── UserControllerTest.java               # 用户控制器测试
│                   │   ├── ProductControllerTest.java            # 商品控制器测试
│                   │   ├── OrderControllerTest.java              # 订单控制器测试
│                   │   └── PaymentControllerTest.java            # 支付控制器测试
│                   ├── service/                                   # 服务层测试
│                   │   ├── UserServiceTest.java                  # 用户服务测试
│                   │   ├── ProductServiceTest.java               # 商品服务测试
│                   │   ├── OrderServiceTest.java                 # 订单服务测试
│                   │   └── PaymentServiceTest.java               # 支付服务测试
│                   ├── repository/                               # 数据访问层测试
│                   │   ├── UserRepositoryTest.java               # 用户仓库测试
│                   │   ├── ProductRepositoryTest.java            # 商品仓库测试
│                   │   └── OrderRepositoryTest.java              # 订单仓库测试
│                   └── integration/                              # 集成测试
│                       ├── UserIntegrationTest.java              # 用户集成测试
│                       ├── OrderIntegrationTest.java             # 订单集成测试
│                       └── PaymentIntegrationTest.java           # 支付集成测试
├── docker/                                                       # Docker相关文件
│   ├── Dockerfile                                               # 应用Docker文件
│   ├── docker-compose.yml                                       # Docker Compose配置
│   ├── docker-compose-dev.yml                                   # 开发环境Docker配置
│   ├── docker-compose-prod.yml                                  # 生产环境Docker配置
│   └── init/                                                    # 初始化脚本
│       ├── mysql/                                               # MySQL初始化
│       │   └── init.sql                                         # 数据库初始化脚本
│       ├── redis/                                               # Redis初始化
│       │   └── redis.conf                                       # Redis配置文件
│       └── nginx/                                               # Nginx配置
│           ├── nginx.conf                                       # Nginx主配置
│           └── default.conf                                     # 默认站点配置
├── docs/                                                        # 文档目录
│   ├── api/                                                     # API文档
│   │   ├── user-api.md                                          # 用户API文档
│   │   ├── product-api.md                                       # 商品API文档
│   │   ├── order-api.md                                         # 订单API文档
│   │   └── payment-api.md                                       # 支付API文档
│   ├── database/                                                # 数据库文档
│   │   ├── database-design.md                                   # 数据库设计文档
│   │   └── er-diagram.png                                       # ER图
│   ├── deployment/                                              # 部署文档
│   │   ├── docker-deployment.md                                 # Docker部署文档
│   │   ├── kubernetes-deployment.md                             # K8s部署文档
│   │   └── manual-deployment.md                                 # 手动部署文档
│   └── development/                                             # 开发文档
│       ├── coding-standards.md                                  # 编码规范
│       ├── git-workflow.md                                      # Git工作流
│       └── testing-guide.md                                     # 测试指南
├── scripts/                                                     # 脚本目录
│   ├── build/                                                   # 构建脚本
│   │   ├── build.sh                                            # 构建脚本
│   │   └── package.sh                                          # 打包脚本
│   ├── deploy/                                                  # 部署脚本
│   │   ├── deploy-dev.sh                                       # 开发环境部署
│   │   ├── deploy-test.sh                                      # 测试环境部署
│   │   └── deploy-prod.sh                                      # 生产环境部署
│   ├── database/                                               # 数据库脚本
│   │   ├── backup.sh                                           # 数据库备份脚本
│   │   ├── restore.sh                                          # 数据库恢复脚本
│   │   └── migrate.sh                                          # 数据库迁移脚本
│   └── maintenance/                                            # 维护脚本
│       ├── cleanup-logs.sh                                     # 日志清理脚本
│       ├── health-check.sh                                     # 健康检查脚本
│       └── performance-monitor.sh                              # 性能监控脚本
├── config/                                                      # 外部配置文件目录
│   ├── dev/                                                    # 开发环境配置
│   │   ├── application-dev.yml                                 # 开发环境主配置
│   │   └── logback-dev.xml                                     # 开发环境日志配置
│   ├── test/                                                   # 测试环境配置
│   │   ├── application-test.yml                                # 测试环境主配置
│   │   └── logback-test.xml                                    # 测试环境日志配置
│   └── prod/                                                   # 生产环境配置
│       ├── application-prod.yml                                # 生产环境主配置
│       └── logback-prod.xml                                    # 生产环境日志配置
├── logs/                                                        # 日志目录
│   ├── application.log                                         # 应用日志
│   ├── error.log                                               # 错误日志
│   ├── access.log                                              # 访问日志
│   └── sql.log                                                 # SQL日志
├── uploads/                                                     # 上传文件目录
│   ├── images/                                                 # 图片上传目录
│   │   ├── products/                                           # 商品图片
│   │   ├── avatars/                                            # 用户头像
│   │   └── temp/                                               # 临时文件
│   └── documents/                                              # 文档上传目录
├── .gitignore                                                  # Git忽略文件
├── .dockerignore                                               # Docker忽略文件
├── README.md                                                   # 项目说明文档
├── CHANGELOG.md                                                # 版本更新日志
├── pom.xml                                                     # Maven配置文件
└── mvnw                                                        # Maven包装器
```

## 开发规范

### 代码规范

1. **命名规范**
   - 类名：PascalCase (如：UserService)
   - 方法名：camelCase (如：getUserById)
   - 常量：UPPER_SNAKE_CASE (如：MAX_RETRY_COUNT)
   - 包名：全小写 (如：com.ecommerce.service)

2. **注释规范**
   - 类注释：详细说明功能、作者、版本、创建时间
   - 方法注释：使用Javadoc格式，说明参数、返回值、异常
   - 重要业务逻辑：添加详细的实现思路注释

3. **异常处理**
   - 使用自定义业务异常：BusinessException
   - 统一异常处理：GlobalExceptionHandler
   - 记录详细的错误日志

### 架构设计

1. **分层架构**
   ```
   Controller层 -> Service层 -> Repository层 -> Entity层
   ```

2. **数据传输**
   - 接口层使用DTO进行数据传输
   - 实体类仅在内部层使用
   - 避免实体类直接暴露给前端

3. **依赖注入**
   - 使用构造器注入方式
   - 使用@RequiredArgsConstructor减少样板代码

### 数据库设计

1. **表命名规范**
   - 使用复数形式：users, products, categories
   - 使用下划线分隔：user_profiles, order_items

2. **字段命名规范**
   - 主键：id
   - 外键：关联表名_id (如：user_id, category_id)
   - 时间字段：created_at, updated_at
   - 布尔字段：is_前缀 (如：is_active, is_deleted)

3. **索引设计**
   - 主键自动创建主键索引
   - 外键字段创建普通索引
   - 查询频繁的字段创建索引
   - 组合查询创建复合索引

### API设计

1. **RESTful规范**
   - GET：查询操作
   - POST：创建操作
   - PUT：更新操作
   - DELETE：删除操作

2. **URL设计**
   - 使用名词，避免动词
   - 使用复数形式：/api/users, /api/products
   - 嵌套资源：/api/users/{id}/orders

3. **响应格式**
   - 统一使用ApiResponse包装
   - 成功：{"success": true, "data": {...}, "message": ""}
   - 失败：{"success": false, "data": null, "message": "错误信息"}

### AI集成规范

1. **Spring AI配置**
   - 配置OpenAI API密钥
   - 设置模型参数和温度
   - 配置向量数据库连接

2. **LangChain4j集成**
   - 定义AI服务接口
   - 配置聊天模型和嵌入模型
   - 实现RAG(检索增强生成)功能

3. **AI应用场景**
   - 商品推荐系统
   - 智能客服聊天机器人
   - 商品描述自动生成
   - 用户行为分析

## 开发流程

### 文件开发顺序

参考 `FILE_DEVELOPMENT_ORDER.md` 文件中定义的开发顺序：

1. **基础架构 (序号1-7)**
   - 项目配置文件
   - 统一响应格式
   - 异常处理机制
   - 基础配置类

2. **用户模块 (序号8-15)**
   - User实体类
   - 用户请求/响应DTO
   - UserRepository数据访问
   - UserService业务逻辑
   - UserController接口层

3. **商品模块 (序号16-23)**
   - Category分类实体
   - Product商品实体
   - 商品相关DTO类
   - 商品业务逻辑
   - 商品接口层

4. **订单模块 (序号24-30)**
   - Order订单实体
   - 订单相关DTO
   - 订单业务流程
   - 支付集成

5. **安全模块 (序号31-35)**
   - Spring Security配置
   - JWT认证机制
   - 权限控制

### Git提交规范

1. **提交格式**
   ```
   <type>(<scope>): <subject>
   
   <body>
   
   <footer>
   ```

2. **提交类型**
   - feat: 新功能
   - fix: 修复bug
   - docs: 文档更新
   - style: 代码格式调整
   - refactor: 代码重构
   - test: 测试相关
   - chore: 构建过程或辅助工具的变动

3. **阶段性提交**
   - 每完成一个功能模块进行提交
   - 重要里程碑创建Tag标记
   - 保持提交历史的清晰和可追溯

## 测试策略

### 单元测试

1. **测试覆盖率**
   - Service层：100%覆盖率
   - Repository层：主要方法覆盖
   - Controller层：关键接口覆盖

2. **测试工具**
   - JUnit 5：基础测试框架
   - Mockito：模拟对象
   - TestContainers：集成测试

### 集成测试

1. **API测试**
   - 使用@SpringBootTest
   - 测试完整的请求响应流程
   - 验证数据库操作结果

2. **数据库测试**
   - 使用@DataJpaTest
   - 测试Repository层方法
   - 验证数据约束和关联关系

## 性能优化

### 数据库优化

1. **索引优化**
   - 分析慢查询日志
   - 合理创建索引
   - 避免过度索引

2. **查询优化**
   - 使用分页查询
   - 避免N+1查询问题
   - 合理使用懒加载

### 缓存策略

1. **Redis缓存**
   - 热点数据缓存
   - 设置合理的过期时间
   - 缓存更新策略

2. **应用缓存**
   - 使用@Cacheable注解
   - 配置缓存管理器
   - 缓存失效策略

## 部署配置

### 环境配置

1. **开发环境**
   - 本地MySQL和Redis
   - 详细的调试日志
   - 热部署配置

2. **生产环境**
   - 集群数据库配置
   - 日志级别优化
   - 监控和告警

### Docker部署

1. **容器化**
   - 应用容器化
   - 数据库容器化
   - Nginx反向代理

2. **编排管理**
   - docker-compose配置
   - 健康检查
   - 数据持久化

## 监控和运维

### 日志管理

1. **日志规范**
   - 分级记录：ERROR, WARN, INFO, DEBUG
   - 结构化日志格式
   - 敏感信息脱敏

2. **日志收集**
   - 集中式日志收集
   - 日志检索和分析
   - 异常告警机制

### 性能监控

1. **应用监控**
   - Spring Boot Actuator
   - JVM性能指标
   - 自定义业务指标

2. **基础设施监控**
   - 服务器资源监控
   - 数据库性能监控
   - 网络和存储监控

## 学习资源

### 官方文档

1. **Spring生态**
   - [Spring Boot官方文档](https://spring.io/projects/spring-boot)
   - [Spring Security官方文档](https://spring.io/projects/spring-security)
   - [Spring Data JPA官方文档](https://spring.io/projects/spring-data-jpa)

2. **AI集成**
   - [Spring AI官方文档](https://spring.io/projects/spring-ai)
   - [LangChain4j官方文档](https://docs.langchain4j.dev/)

### 最佳实践

1. **代码质量**
   - Clean Code原则
   - SOLID设计原则
   - 设计模式应用

2. **架构设计**
   - DDD领域驱动设计
   - 微服务架构模式
   - 事件驱动架构

## 当前开发状态

### 已完成模块

1. **基础架构** ✅
   - Maven配置和依赖管理
   - Spring Boot主启动类
   - 统一响应格式ApiResponse
   - 全局异常处理机制
   - Web配置和AI配置

2. **用户模块** ✅
   - User实体类（完整的用户信息和Spring Security集成）
   - 用户注册/登录请求DTO
   - 用户信息响应DTO
   - UserRepository（30+查询方法）
   - UserService接口和实现
   - UserController（完整的RESTful API）

3. **商品基础模块** ✅
   - Category分类实体（树形结构设计）
   - Product商品实体（完整的电商商品属性）
   - ProductCreateRequest创建请求DTO
   - ProductDetailResponse详情响应DTO

### 待开发模块

1. **商品模块继续**
   - ProductRepository数据访问层
   - ProductService业务逻辑层
   - ProductController接口层
   - CategoryRepository和CategoryService

2. **订单模块**
   - Order订单实体
   - OrderItem订单明细实体
   - 订单相关DTO
   - 订单业务流程
   - 支付集成

3. **安全认证模块**
   - Spring Security配置
   - JWT认证实现
   - 权限控制机制

4. **AI功能模块**
   - 商品推荐系统
   - 智能客服功能
   - 商品描述生成

### 下一步开发计划

按照FILE_DEVELOPMENT_ORDER.md继续开发：
- 序号20：ProductRepository.java - 商品数据访问层
- 序号21：ProductService.java - 商品服务接口
- 序号22：ProductServiceImpl.java - 商品服务实现
- 序号23：ProductController.java - 商品控制器

## 项目亮点

1. **现代化技术栈**: 使用Java 21和Spring Boot 3.x最新特性
2. **AI深度集成**: Spring AI和LangChain4j的实际应用场景
3. **企业级架构**: 完整的分层架构和领域模型设计
4. **详细文档**: 每个文件都有详细的开发思路和业务逻辑说明
5. **学习友好**: 代码注释详细，适合学习和理解
6. **实用性强**: 涵盖电商系统的核心功能模块

这个项目不仅是一个技术学习平台，更是一个展示现代Java开发最佳实践的完整案例。通过这个项目，可以深入理解Spring生态、AI集成、电商业务逻辑等多个技术领域。