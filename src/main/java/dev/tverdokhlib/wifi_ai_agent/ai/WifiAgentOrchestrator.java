package dev.tverdokhlib.wifi_ai_agent.ai;

import dev.tverdokhlib.wifi_ai_agent.ai.prompt.WifiAgentPrompt;
import dev.tverdokhlib.wifi_ai_agent.ai.rag.RouterOsRag;
import dev.tverdokhlib.wifi_ai_agent.ai.loop.WifiAgentLoop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
/** Concept 6: selects LLM-only, tools, RAG, or tools-plus-RAG workflow. */
public class WifiAgentOrchestrator {

    private final ChatClient.Builder chatClientBuilder;
    private final WifiAgentLoop wifiAgentLoop;
    private final WifiAgentPrompt wifiAgentPrompt;
    private final RouterOsRag routerOsRag;
    private final ChatMemory chatMemory;
    private final RequestClassifier requestClassifier;

    public String ask(String conversationId, String question) {

        RequestType requestType = requestClassifier.classify(question);

        log.info("LLM orchestration: requestType={}", requestType);

        String response = switch (requestType) {
            case CURRENT_STATE -> executeWithTools(conversationId, question);

            case DOCUMENTATION -> executeWithRag(conversationId, question);

            case TROUBLESHOOTING -> executeWithToolsAndRag(conversationId, question);

            case GENERAL -> executeGeneral(conversationId, question);
        };

        return withAgentPrefix(response);
    }

    private String executeWithTools(
            String conversationId,
            String question) {

        ChatClient chatClient = createAgentChatClient();

        return chatClient
                .prompt()
                .system(wifiAgentPrompt.resource())
                .user(question)
                .toolCallbacks(wifiAgentLoop.toolsFor(question))
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .content();
    }

    private String executeWithRag(
            String conversationId,
            String question) {

        ChatClient chatClient = createChatClient();

        return chatClient
                .prompt()
                .system(wifiAgentPrompt.resource())
                .user(question)
                .advisors(advisor -> advisor
                        .advisors(
                                routerOsRag.advisor()
                        )
                        .param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .content();
    }

    private String executeWithToolsAndRag(
            String conversationId,
            String question) {

        ChatClient chatClient = createAgentChatClient();

        return chatClient
                .prompt()
                .system(wifiAgentPrompt.resource())
                .user(question)
                .toolCallbacks(wifiAgentLoop.toolsFor(question))
                .advisors(advisor -> advisor
                        .advisors(
                                routerOsRag.advisor()
                        )
                        .param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .content();
    }

    private String executeGeneral(
            String conversationId,
            String question) {

        ChatClient chatClient = createChatClient();

        return chatClient
                .prompt()
                .system(wifiAgentPrompt.resource())
                .user(question)
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .content();
    }

    private ChatClient createChatClient() {
        return chatClientBuilder.clone()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build()
                )
                .build();
    }

    private ChatClient createAgentChatClient() {
        return chatClientBuilder.clone()
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build(),
                        wifiAgentLoop.advisor()
                )
                .build();
    }

    private String withAgentPrefix(String content) {
        String normalized = content == null ? "" : content.strip();

        if (normalized.startsWith("[WIFI-AGENT]")) {
            return normalized;
        }

        return "[WIFI-AGENT]\n\n" + normalized;
    }
}
