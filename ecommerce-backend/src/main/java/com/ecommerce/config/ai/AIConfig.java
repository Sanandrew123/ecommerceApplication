package com.ecommerce.config.ai;

/*
 * 文件职责: AI相关配置类，整合Spring AI、LangChain4j等AI技术栈
 * 
 * 学习扩展价值：
 * 1. Spring AI集成 - 官方AI框架，与Spring生态完美结合
 * 2. LangChain4j - Java版LangChain，丰富的AI工具链
 * 3. 向量数据库 - Redis向量存储，支持RAG应用
 * 4. OpenAI集成 - 主流AI模型API接入
 * 
 * 应用场景预设：
 * 1. 智能客服 - 基于商品知识库的问答系统
 * 2. 商品推荐 - 基于用户画像和商品特征的AI推荐
 * 3. 内容生成 - 商品描述、营销文案自动生成
 * 4. 图像识别 - 商品图片分类和标签提取
 * 5. 数据分析 - 销售数据智能分析和预测
 * 
 * 包结构设计思路:
 * - config.ai包专门处理AI相关配置
 * - 与其他配置模块解耦，便于独立开发和测试
 * - 支持条件化配置，可选择性启用AI功能
 * 
 * 命名原因:
 * - AIConfig明确表达这是AI相关配置
 * - 简洁命名，便于理解和维护
 * 
 * 依赖关系:
 * - 依赖Spring AI和LangChain4j框架
 * - 与Redis配置协同，支持向量存储
 * - 为AI服务层提供基础配置支持
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * AI技术栈配置类
 * 
 * 技术亮点：
 * 1. Spring AI官方框架集成
 * 2. 多模型支持(OpenAI、本地模型等)
 * 3. 向量数据库集成(Redis Vector Store)
 * 4. RAG(检索增强生成)支持
 * 5. 条件化配置，支持开发/生产环境切换
 * 
 * 学习重点：
 * 1. AI模型配置和管理
 * 2. 向量存储和检索
 * 3. Prompt工程和模板管理
 * 4. AI服务的Spring Bean管理
 * 
 * 扩展方向：
 * 1. 多模型路由 - 根据任务类型选择不同模型
 * 2. 模型微调 - 基于电商数据的模型优化
 * 3. 成本控制 - API调用频率限制和成本监控
 * 4. 性能优化 - 模型响应缓存和并发控制
 * 
 * @author ecommerce-team
 * @version 1.0.0
 * @since 2024-08-04
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AIConfig {

    /**
     * OpenAI API Key
     * 从配置文件或环境变量读取
     */
    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    /**
     * OpenAI API Base URL
     * 支持自定义API端点，便于使用代理或本地部署
     */
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    /**
     * 默认使用的AI模型
     */
    @Value("${app.ai.default.model:gpt-3.5-turbo}")
    private String defaultModel;

    /**
     * AI功能是否启用
     */
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * OpenAI Chat模型配置
     * 
     * 功能说明：
     * 1. 配置OpenAI API连接
     * 2. 设置默认模型参数
     * 3. 支持流式响应
     * 4. 错误处理和重试机制
     * 
     * 学习要点：
     * - OpenAI API的Java集成方式
     * - 模型参数调优(temperature、max_tokens等)
     * - API密钥安全管理
     * 
     * @return OpenAI聊天模型实例
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.openai.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiChatModel openAiChatModel() {
        log.info("初始化OpenAI Chat模型，模型: {}, API地址: {}", defaultModel, openaiBaseUrl);
        
        // 检查API Key配置
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            log.warn("OpenAI API Key未配置，AI功能将受限");
        }
        
        // 创建OpenAI API配置
        var openAiApi = new OpenAiApi(openaiBaseUrl, openaiApiKey);
        
        // 创建Chat模型实例
        var chatModel = new OpenAiChatModel(openAiApi);
        
        log.info("OpenAI Chat模型初始化完成");
        return chatModel;
    }

    /**
     * Spring AI ChatClient配置
     * 
     * 功能说明：
     * 1. 基于OpenAI模型的高级客户端
     * 2. 支持Prompt模板和参数化
     * 3. 内置对话历史管理
     * 4. 支持函数调用(Function Calling)
     * 
     * 学习要点：
     * - ChatClient API的使用方式
     * - Prompt工程最佳实践
     * - 对话上下文管理
     * - 函数调用集成
     * 
     * @param chatModel OpenAI聊天模型
     * @return ChatClient实例
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        log.info("初始化Spring AI ChatClient");
        
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一个专业的电商平台AI助手，专门帮助用户解决购物相关问题。
                    
                    你的职责包括：
                    1. 商品推荐和比较
                    2. 订单状态查询和处理
                    3. 购物建议和指导
                    4. 客户服务支持
                    
                    请始终以友好、专业的语气回答用户问题，并尽可能提供有用的建议。
                    如果遇到无法处理的问题，请引导用户联系人工客服。
                    """)
                .build();
    }

    /**
     * Redis向量存储配置
     * 
     * 功能说明：
     * 1. 基于Redis的向量数据库
     * 2. 支持语义搜索和相似度匹配
     * 3. 为RAG应用提供知识库存储
     * 4. 高性能向量检索
     * 
     * 应用场景：
     * 1. 商品语义搜索 - 基于描述找到相似商品
     * 2. 知识库问答 - 客服知识库的智能检索
     * 3. 用户画像匹配 - 基于兴趣向量的个性化推荐
     * 4. 内容去重 - 检测重复或相似的商品/内容
     * 
     * @param redisConnectionFactory Redis连接工厂
     * @return 向量存储实例
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.vector-store.enabled", havingValue = "true", matchIfMissing = true)
    public VectorStore vectorStore(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化Redis向量存储");
        
        // 创建Redis向量存储配置
        return RedisVectorStore.builder(redisConnectionFactory)
                .indexName("ecommerce-knowledge-base")  // 索引名称
                .prefix("ecommerce:")                    // 键前缀
                .build();
    }

    /**
     * AI服务配置信息Bean
     * 
     * 功能说明：
     * 1. 封装AI配置信息
     * 2. 便于其他组件获取AI配置
     * 3. 支持配置热更新
     * 
     * @return AI配置信息
     */
    @Bean
    public AIConfigProperties aiConfigProperties() {
        var properties = new AIConfigProperties();
        properties.setEnabled(aiEnabled);
        properties.setDefaultModel(defaultModel);
        properties.setOpenaiApiKey(openaiApiKey != null ? openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())) + "..." : "未配置");
        properties.setOpenaiBaseUrl(openaiBaseUrl);
        
        log.info("AI配置信息: {}", properties);
        return properties;
    }

    /**
     * AI配置信息类
     * 封装AI相关配置参数
     */
    public static class AIConfigProperties {
        private boolean enabled;
        private String defaultModel;
        private String openaiApiKey;
        private String openaiBaseUrl;

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        
        public String getOpenaiApiKey() { return openaiApiKey; }
        public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
        
        public String getOpenaiBaseUrl() { return openaiBaseUrl; }
        public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }

        @Override
        public String toString() {
            return String.format("AIConfig{enabled=%s, model='%s', baseUrl='%s'}", 
                enabled, defaultModel, openaiBaseUrl);
        }
    }
}