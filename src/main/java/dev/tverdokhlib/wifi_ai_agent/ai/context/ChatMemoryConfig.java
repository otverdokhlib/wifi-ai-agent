package dev.tverdokhlib.wifi_ai_agent.ai.context;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Concept 5: bounds the conversation-history part of the LLM context. */
@Configuration
public class ChatMemoryConfig {

    static final int MAX_MESSAGES = 8;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(MAX_MESSAGES).build();
    }
}
