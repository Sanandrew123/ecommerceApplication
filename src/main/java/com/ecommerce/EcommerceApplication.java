/*
文件级分析：
- 职责：Spring Boot应用程序入口点，负责启动整个应用程序
- 包结构考虑：位于根包com.ecommerce下，这是Spring Boot扫描组件的起点
- 命名原因：遵循Spring Boot命名约定，以Application结尾表示主启动类
- 调用关系：被JVM调用启动，自动扫描并加载所有子包下的Spring组件

设计思路：
1. 使用@SpringBootApplication注解启用自动配置
2. 开启JPA审计功能，自动填充创建时间、修改时间等字段
3. 预留扩展空间，后续可能需要排除某些自动配置或添加其他注解
*/
package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 电商系统主启动类
 * 
 * 作为整个Spring Boot应用的入口点，该类承担以下职责：
 * 1. 启动Spring应用上下文
 * 2. 启用自动配置机制
 * 3. 启用JPA审计功能，自动管理实体的创建时间、修改时间等
 * 4. 启用事务管理，确保数据一致性
 * 
 * @author Claude
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableJpaAuditing  // 启用JPA审计功能，自动填充@CreatedDate, @LastModifiedDate等字段
@EnableTransactionManagement  // 启用事务管理，支持@Transactional注解
public class EcommerceApplication {

    /**
     * 应用程序主入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用
        SpringApplication.run(EcommerceApplication.class, args);
        
        // 打印启动成功信息
        System.out.println("""
            
            ========================================
            🚀 电商系统启动成功！
            📖 API文档地址: http://localhost:8080/swagger-ui.html
            🔧 监控地址: http://localhost:8080/actuator
            ========================================
            """);
    }
}