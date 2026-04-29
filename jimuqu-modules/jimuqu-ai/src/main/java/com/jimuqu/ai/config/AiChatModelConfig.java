package com.jimuqu.ai.config;

import org.noear.solon.ai.chat.ChatConfig;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * 模型配置
 *
 * @author chengliang
 * @date 2025/12/27
 */
@Configuration
@Inject("${classpath:jimuqu-ai.yml}")
public class AiChatModelConfig {

    /**
     * 构建模型
     * @param config 配置
     * @return 模型
     */
    @Bean
    public ChatModel build(@Inject("${solon.ai.chat.default}") ChatConfig config) {
        return ChatModel.of(config).build();
    }

}
