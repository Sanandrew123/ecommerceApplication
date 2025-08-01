# Spring Boot电商网站详细开发计划与文件结构

## 目录
1. [项目概述](#项目概述)
2. [详细开发计划](#详细开发计划)
3. [最终文件结构](#最终文件结构)
4. [数据库设计](#数据库设计)
5. [接口设计](#接口设计)
6. [技术选型说明](#技术选型说明)

---

## 项目概述

### 功能模块
- 用户管理（注册、登录、个人信息）
- 商品管理（商品CRUD、分类管理、库存管理）
- 购物车管理（添加、删除、修改数量）
- 订单管理（下单、支付、订单状态）
- 支付系统（支付宝、微信支付）
- 文件管理（图片上传、文件存储）
- 系统管理（日志、监控、配置）

### 技术栈
- **后端**: Spring Boot 3.x + Spring Security + Spring Data JPA
- **数据库**: MySQL 8.0 + Redis 7.0
- **搜索**: Elasticsearch 8.x
- **消息队列**: RabbitMQ
- **文件存储**: 本地存储/阿里云OSS
- **缓存**: Redis + Caffeine
- **监控**: Spring Boot Actuator + Prometheus

---

## 详细开发计划

### 第1阶段：项目基础搭建（第1-3天）

#### 第1天：环境准备与项目初始化
**上午（3小时）**
1. 开发环境检查（JDK 17、Maven、MySQL、Redis、IDE）
2. 使用Spring Initializr创建项目
   - 选择依赖：Web、JPA、MySQL、Security、DevTools、Validation
3. 配置application.yml基础配置
4. 测试项目启动是否正常

**下午（4小时）**
1. 创建基础项目结构目录
2. 配置Git仓库，提交初始代码
3. 配置数据库连接，测试连接
4. 添加常用工具类依赖（Lombok、Hutool等）
5. 创建全局异常处理框架

#### 第2天：基础架构设计
**上午（4小时）**
1. 设计项目分层架构
2. 创建基础实体类结构
3. 设计统一返回结果格式
4. 创建基础配置类（Web配置、跨域配置）

**下午（3小时）**
1. 设计数据库表结构（用户、商品、订单相关）
2. 创建数据库初始化脚本
3. 配置JPA相关设置
4. 测试数据库表创建

#### 第3天：安全框架搭建
**上午（4小时）**
1. 集成Spring Security
2. 设计JWT工具类
3. 创建用户认证相关配置
4. 设计权限管理结构

**下午（3小时）**
1. 创建登录接口基础框架
2. 测试JWT生成和验证
3. 配置安全过滤器链
4. 完善异常处理

### 第2阶段：用户管理模块（第4-6天）

#### 第4天：用户注册功能
**上午（4小时）**
1. 设计用户实体类（User）
2. 创建用户数据访问层（UserRepository）
3. 设计用户注册请求DTO
4. 实现用户注册服务层逻辑

**下午（3小时）**
1. 创建用户注册控制器
2. 添加参数验证注解
3. 测试用户注册功能
4. 处理重复注册等异常情况

#### 第5天：用户登录功能
**上午（4小时）**
1. 实现UserDetailsService接口
2. 创建登录请求和响应DTO
3. 实现登录服务层逻辑
4. 集成JWT令牌生成

**下午（3小时）**
1. 创建登录控制器
2. 实现JWT过滤器
3. 测试登录功能和令牌验证
4. 处理登录失败等异常

#### 第6天：用户信息管理
**上午（3小时）**
1. 实现获取用户信息接口
2. 实现修改用户信息接口
3. 实现修改密码功能

**下午（4小时）**
1. 添加用户头像上传功能
2. 实现用户状态管理（启用/禁用）
3. 完善用户相关的所有接口
4. 编写用户模块的单元测试

### 第3阶段：商品管理模块（第7-10天）

#### 第7天：商品分类管理
**上午（4小时）**
1. 设计商品分类实体（Category）
2. 实现分类树形结构设计
3. 创建分类数据访问层
4. 实现分类CRUD服务

**下午（3小时）**
1. 创建分类管理控制器
2. 实现分类树形结构返回
3. 测试分类管理功能
4. 添加分类排序功能

#### 第8天：商品基础管理
**上午（4小时）**
1. 设计商品实体（Product）
2. 设计商品与分类的关联关系
3. 创建商品数据访问层
4. 实现商品基础CRUD服务

**下午（3小时）**
1. 创建商品管理控制器
2. 实现商品列表分页查询
3. 实现商品详情查询
4. 添加商品状态管理

#### 第9天：商品高级功能
**上午（4小时）**
1. 实现商品搜索功能（基于数据库）
2. 实现商品筛选功能（价格、分类等）
3. 实现商品排序功能（价格、销量、时间）
4. 添加商品库存管理

**下午（3小时）**
1. 实现热门商品推荐
2. 实现相关商品推荐
3. 添加商品浏览历史记录
4. 优化商品查询性能

#### 第10天：商品图片管理
**上午（3小时）**
1. 设计文件上传配置
2. 实现商品图片上传接口
3. 实现图片存储和访问

**下午（4小时）**
1. 实现多图片上传和管理
2. 添加图片压缩和格式转换
3. 实现图片删除功能
4. 测试商品模块完整功能

### 第4阶段：购物车模块（第11-12天）

#### 第11天：购物车基础功能
**上午（4小时）**
1. 设计购物车数据结构（基于Redis）
2. 实现添加商品到购物车
3. 实现购物车商品列表查询
4. 实现购物车商品数量修改

**下午（3小时）**
1. 实现删除购物车商品
2. 实现清空购物车
3. 实现购物车商品选择状态
4. 添加购物车数据持久化

#### 第12天：购物车高级功能
**上午（3小时）**
1. 实现购物车商品价格计算
2. 添加购物车商品库存检查
3. 实现购物车商品失效处理

**下午（4小时）**
1. 优化购物车性能（缓存策略）
2. 实现购物车数据同步
3. 添加购物车操作日志
4. 测试购物车完整功能

### 第5阶段：订单管理模块（第13-16天）

#### 第13天：订单基础设计
**上午（4小时）**
1. 设计订单实体（Order、OrderItem）
2. 设计订单状态枚举
3. 创建订单数据访问层
4. 设计订单号生成规则

**下午（3小时）**
1. 实现订单创建前置检查
2. 设计订单创建流程
3. 实现库存扣减逻辑
4. 添加订单事务管理

#### 第14天：订单创建流程
**上午（4小时）**
1. 实现订单创建服务
2. 实现订单金额计算
3. 实现收货地址管理
4. 添加订单创建验证

**下午（3小时）**
1. 创建订单创建控制器
2. 实现从购物车创建订单
3. 实现立即购买创建订单
4. 测试订单创建功能

#### 第15天：订单查询管理
**上午（4小时）**
1. 实现订单列表查询（分页）
2. 实现订单详情查询
3. 实现订单状态筛选
4. 实现订单搜索功能

**下午（3小时）**
1. 实现订单取消功能
2. 实现订单确认收货
3. 实现订单评价功能
4. 添加订单操作权限控制

#### 第16天：订单状态管理
**上午（3小时）**
1. 实现订单状态流转
2. 添加订单状态变更日志
3. 实现订单超时处理

**下午（4小时）**
1. 实现订单退款功能
2. 实现订单售后处理
3. 添加订单统计功能
4. 测试订单模块完整功能

### 第6阶段：支付模块（第17-19天）

#### 第17天：支付基础框架
**上午（4小时）**
1. 设计支付记录实体
2. 集成支付宝SDK配置
3. 实现支付接口基础框架
4. 设计支付回调处理

**下午（3小时）**
1. 实现支付订单创建
2. 实现支付状态查询
3. 添加支付安全验证
4. 实现支付失败处理

#### 第18天：支付宝集成
**上午（4小时）**
1. 配置支付宝应用参数
2. 实现支付宝支付接口
3. 实现支付宝回调处理
4. 添加支付宝签名验证

**下午（3小时）**
1. 实现支付宝退款接口
2. 实现支付宝对账功能
3. 测试支付宝支付流程
4. 处理支付异常情况

#### 第19天：支付完善
**上午（3小时）**
1. 集成微信支付（可选）
2. 实现支付方式选择
3. 添加支付重试机制

**下午（4小时）**
1. 实现支付通知处理
2. 添加支付日志记录
3. 实现支付数据统计
4. 测试支付模块完整功能

### 第7阶段：系统优化（第20-23天）

#### 第20天：缓存集成
**上午（4小时）**
1. 配置Redis缓存
2. 实现商品信息缓存
3. 实现用户信息缓存
4. 实现热门数据缓存

**下午（3小时）**
1. 添加缓存更新策略
2. 实现缓存预热功能
3. 添加缓存监控
4. 优化缓存性能

#### 第21天：搜索优化
**上午（4小时）**
1. 集成Elasticsearch
2. 创建商品搜索索引
3. 实现全文搜索功能
4. 添加搜索结果高亮

**下午（3小时）**
1. 实现搜索建议功能
2. 添加搜索统计分析
3. 优化搜索性能
4. 实现搜索结果排序

#### 第22天：消息队列
**上午（4小时）**
1. 集成RabbitMQ
2. 实现订单异步处理
3. 实现邮件异步发送
4. 添加消息重试机制

**下午（3小时）**
1. 实现库存异步更新
2. 添加消息死信队列
3. 实现消息监控
4. 测试消息队列功能

#### 第23天：性能优化
**上午（3小时）**
1. 数据库查询优化
2. 添加数据库索引
3. 实现读写分离配置

**下午（4小时）**
1. 接口性能测试
2. 添加接口限流
3. 实现数据分页优化
4. 完善异常处理

### 第8阶段：系统完善（第24-25天）

#### 第24天：监控与日志
**上午（4小时）**
1. 配置Actuator监控
2. 集成Prometheus指标
3. 实现自定义监控指标
4. 配置健康检查

**下午（3小时）**
1. 配置日志框架
2. 实现操作日志记录
3. 添加错误日志收集
4. 配置日志文件管理

#### 第25天：部署准备
**上午（3小时）**
1. 编写Docker配置文件
2. 配置不同环境的配置文件
3. 实现数据库迁移脚本

**下午（4小时）**
1. 编写部署文档
2. 进行完整功能测试
3. 性能压力测试
4. 项目总结和优化建议

---

## 最终文件结构

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
└── mvnw                                                        # Maven Wrapper脚本
```

---

## 数据库设计

### 核心表结构

#### 用户相关表
1. **users** - 用户主表
2. **user_addresses** - 用户地址表
3. **user_login_logs** - 用户登录日志表

#### 商品相关表
1. **categories** - 商品分类表（支持多级分类）
2. **products** - 商品主表
3. **product_images** - 商品图片表
4. **product_specs** - 商品规格表
5. **product_stock_logs** - 库存变动日志表

#### 订单相关表
1. **orders** - 订单主表
2. **order_items** - 订单明细表
3. **order_status_logs** - 订单状态变更日志表

#### 支付相关表
1. **payments** - 支付记录表
2. **payment_logs** - 支付日志表
3. **refunds** - 退款记录表

#### 系统相关表
1. **operation_logs** - 操作日志表
2. **system_configs** - 系统配置表
3. **admin_users** - 管理员用户表

### 索引设计策略
1. **主键索引** - 所有表的主键
2. **唯一索引** - 用户名、邮箱、订单号等
3. **普通索引** - 外键、状态字段、时间字段
4. **组合索引** - 常用查询条件组合
5. **全文索引** - 商品名称、描述等

---

## 接口设计

### RESTful API设计规范

#### 认证相关接口
```
POST   /api/auth/register     # 用户注册
POST   /api/auth/login        # 用户登录
POST   /api/auth/logout       # 用户登出
POST   /api/auth/refresh      # 刷新Token
POST   /api/auth/forgot       # 忘记密码
POST   /api/auth/reset        # 重置密码
```

#### 用户相关接口
```
GET    /api/users/profile     # 获取用户信息
PUT    /api/users/profile     # 更新用户信息
POST   /api/users/avatar      # 上传头像
GET    /api/users/addresses   # 获取地址列表
POST   /api/users/addresses   # 添加地址
PUT    /api/users/addresses/{id}  # 更新地址
DELETE /api/users/addresses/{id}  # 删除地址
```

#### 商品相关接口
```
GET    /api/products          # 获取商品列表（分页、搜索、筛选）
GET    /api/products/{id}     # 获取商品详情
GET    /api/products/hot      # 获取热门商品
GET    /api/products/recommend # 获取推荐商品
GET    /api/categories        # 获取分类树
GET    /api/categories/{id}/products # 获取分类下的商品
```

#### 购物车相关接口
```
GET    /api/cart              # 获取购物车
POST   /api/cart/items        # 添加商品到购物车
PUT    /api/cart/items/{id}   # 更新购物车商品数量
DELETE /api/cart/items/{id}   # 删除购物车商品
DELETE /api/cart              # 清空购物车
```

#### 订单相关接口
```
GET    /api/orders            # 获取订单列表
POST   /api/orders            # 创建订单
GET    /api/orders/{id}       # 获取订单详情
PUT    /api/orders/{id}/cancel # 取消订单
PUT    /api/orders/{id}/confirm # 确认收货
POST   /api/orders/{id}/review # 订单评价
```

#### 支付相关接口
```
POST   /api/payments          # 创建支付
GET    /api/payments/{id}     # 查询支付状态
POST   /api/payments/{id}/notify # 支付回调
POST   /api/payments/{id}/refund  # 申请退款
```

#### 文件相关接口
```
POST   /api/files/upload      # 文件上传
GET    /api/files/{id}        # 文件下载
DELETE /api/files/{id}        # 删除文件
```

#### 管理后台接口
```
# 用户管理
GET    /api/admin/users       # 用户列表
PUT    /api/admin/users/{id}/status # 用户状态管理

# 商品管理
POST   /api/admin/products    # 创建商品
PUT    /api/admin/products/{id} # 更新商品
DELETE /api/admin/products/{id} # 删除商品

# 订单管理
GET    /api/admin/orders      # 订单列表
PUT    /api/admin/orders/{id}/status # 订单状态管理

# 分类管理
POST   /api/admin/categories  # 创建分类
PUT    /api/admin/categories/{id} # 更新分类
DELETE /api/admin/categories/{id} # 删除分类

# 统计分析
GET    /api/admin/statistics  # 获取统计数据
```

---

## 技术选型说明

### 后端技术栈选择理由

#### Spring Boot 3.x
- **优势**: 自动配置、起步依赖、内嵌服务器
- **适用场景**: 快速开发、微服务架构
- **学习重点**: 自动配置原理、起步依赖机制

#### Spring Security + JWT
- **优势**: 成熟的安全框架、无状态认证
- **适用场景**: 用户认证授权、API安全
- **学习重点**: 过滤器链、认证流程、权限控制

#### Spring Data JPA
- **优势**: 简化数据访问、自动生成查询
- **适用场景**: 关系型数据库操作
- **学习重点**: 实体关联、查询方法、事务管理

#### Redis
- **优势**: 高性能缓存、丰富数据结构
- **适用场景**: 缓存、会话存储、计数器
- **学习重点**: 数据类型、过期策略、持久化

#### Elasticsearch
- **优势**: 全文搜索、分析能力强
- **适用场景**: 商品搜索、日志分析
- **学习重点**: 索引设计、查询语法、聚合分析

#### RabbitMQ
- **优势**: 可靠性高、路由灵活
- **适用场景**: 异步处理、系统解耦
- **学习重点**: 交换机类型、消息确认、死信队列

### 开发工具推荐

#### 开发环境
- **IDE**: IntelliJ IDEA Ultimate
- **数据库客户端**: DataGrip 或 Navicat
- **Redis客户端**: Redis Desktop Manager
- **API测试**: Postman 或 Apifox
- **版本控制**: Git + GitKraken

#### 监控工具
- **应用监控**: Spring Boot Actuator + Micrometer
- **日志分析**: ELK Stack（Elasticsearch + Logstash + Kibana）
- **性能监控**: Prometheus + Grafana
- **链路追踪**: Zipkin 或 Jaeger

### 部署方案

#### 开发环境
- **方式**: Docker Compose 一键启动
- **包含服务**: MySQL、Redis、RabbitMQ、Elasticsearch
- **特点**: 快速搭建、环境一致

#### 测试环境
- **方式**: Docker 容器化部署
- **CI/CD**: GitLab CI 或 Jenkins
- **特点**: 自动化部署、环境隔离

#### 生产环境
- **方式**: Kubernetes 集群部署
- **服务治理**: Spring Cloud 或 Istio
- **特点**: 高可用、弹性伸缩、负载均衡

---

## 关键学习点

### 第一阶段重点（基础搭建）
1. **Spring Boot自动配置原理**
2. **项目分层架构设计**
3. **统一异常处理机制**
4. **配置文件管理策略**

### 第二阶段重点（用户管理）
1. **Spring Security配置**
2. **JWT令牌机制**
3. **密码加密策略**
4. **用户权限控制**

### 第三阶段重点（商品管理）
1. **JPA实体关联设计**
2. **分页查询实现**
3. **动态查询构建**
4. **文件上传处理**

### 第四阶段重点（购物车）
1. **Redis数据结构设计**
2. **缓存策略选择**
3. **数据一致性保证**

### 第五阶段重点（订单管理）
1. **复杂业务流程设计**
2. **分布式事务处理**
3. **库存扣减策略**
4. **订单状态流转**

### 第六阶段重点（支付集成）
1. **第三方支付集成**
2. **支付安全处理**
3. **回调处理机制**
4. **异常情况处理**

### 第七阶段重点（系统优化）
1. **缓存设计模式**
2. **搜索引擎使用**
3. **消息队列应用**
4. **性能监控实现**

### 第八阶段重点（系统完善）
1. **监控体系搭建**
2. **日志系统设计**
3. **部署流程优化**
4. **运维自动化**

通过这个详细的开发计划，你可以系统性地学习Spring Boot的各个方面，从项目搭建到最终部署，每个阶段都有明确的学习目标和实践内容。建议严格按照时间安排进行，确保每个阶段的功能都完整实现并充分测试。