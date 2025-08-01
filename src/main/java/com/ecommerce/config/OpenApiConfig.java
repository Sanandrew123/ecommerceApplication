/*
文件级分析：
- 职责：OpenAPI（Swagger）文档配置类，自动生成API文档
- 包结构考虑：位于config包下，与其他配置类统一管理
- 命名原因：OpenApiConfig明确表示这是OpenAPI配置类
- 调用关系：被Spring Boot自动扫描加载，配置API文档生成器

设计思路：
1. 配置API文档的基本信息（标题、描述、版本等）
2. 配置安全认证方案（JWT Token）
3. 配置API分组，便于文档组织
4. 配置全局参数和响应示例
5. 为开发和测试提供便利的接口调试工具
*/
package com.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI文档配置类
 * 
 * 配置Swagger/OpenAPI 3.0规范的API文档，提供：
 * 1. 自动生成的API文档界面
 * 2. 在线接口测试功能
 * 3. 请求响应格式说明
 * 4. 安全认证配置
 * 
 * 访问地址：
 * - API文档UI: http://localhost:8080/api/swagger-ui.html
 * - API文档JSON: http://localhost:8080/api/v3/api-docs
 * 
 * 开发价值：
 * - 减少前后端沟通成本
 * - 提供实时的接口测试工具
 * - 自动生成客户端代码
 * - 保持文档与代码同步
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:/api}")
    private String contextPath;
    
    /**
     * OpenAPI配置
     * 
     * 配置API文档的基本信息、服务器地址、安全方案等
     * 
     * @return OpenAPI配置实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 基本信息配置
                .info(buildApiInfo())
                // 服务器配置
                .servers(buildServers())
                // 安全方案配置
                .components(buildComponents())
                // 全局安全要求
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    
    /**
     * 构建API基本信息
     * 
     * @return API信息对象
     */
    private Info buildApiInfo() {
        return new Info()
                .title("电商系统API文档")
                .description("""
                        ## 电商系统后端API接口文档
                        
                        ### 项目简介
                        基于Spring Boot 3.x + Java 21构建的现代化电商系统后端，提供完整的电商业务功能。
                        
                        ### 技术栈
                        - **框架**: Spring Boot 3.2.1
                        - **Java版本**: Java 21 LTS
                        - **数据库**: MySQL 8.0 + Redis 7.0
                        - **安全**: Spring Security + JWT
                        - **文档**: OpenAPI 3.0
                        
                        ### 主要功能模块
                        - 👤 用户管理：注册、登录、个人信息管理
                        - 🛍️ 商品管理：商品CRUD、分类管理、库存管理
                        - 🛒 购物车：添加、删除、修改商品
                        - 📦 订单管理：下单、支付、订单状态管理
                        - 💳 支付系统：支付宝、微信支付集成
                        - 📁 文件管理：图片上传、文件存储
                        - 🔐 权限管理：基于角色的访问控制
                        
                        ### 认证说明
                        本系统使用JWT Token进行身份认证，请在请求头中添加：
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ### 响应格式
                        所有API都遵循统一的响应格式：
                        ```json
                        {
                            "code": 200,
                            "message": "操作成功",
                            "data": {...},
                            "timestamp": "2024-01-01 12:00:00"
                        }
                        ```
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("开发团队")
                        .email("dev@ecommerce.com")
                        .url("https://github.com/ecommerce-team"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
    
    /**
     * 构建服务器配置
     * 
     * @return 服务器列表
     */
    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort + contextPath)
                        .description("本地开发环境"),
                new Server()
                        .url("https://api-test.ecommerce.com" + contextPath)
                        .description("测试环境"),
                new Server()
                        .url("https://api.ecommerce.com" + contextPath)
                        .description("生产环境")
        );
    }
    
    /**
     * 构建组件配置（主要是安全方案）
     * 
     * @return 组件配置
     */
    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", 
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .name("Authorization")
                                .description("JWT认证Token，格式：Bearer <token>"));
    }
}