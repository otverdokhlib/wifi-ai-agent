package dev.tverdokhlib.wifi_ai_agent.ai.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

/** Verifies only concept 5: bounded conversation history in context. */
class ChatMemoryConfigTest {

    @Test
    void keepsOnlyConfiguredConversationWindow() {
        ChatMemory memory = new ChatMemoryConfig().chatMemory();
        IntStream.range(0, 25)
                .forEach(index -> memory.add("conversation", new UserMessage("message-" + index)));

        assertThat(memory.get("conversation")).hasSize(ChatMemoryConfig.MAX_MESSAGES);
        assertThat(memory.get("conversation").getFirst().getText())
                .isEqualTo("message-" + (25 - ChatMemoryConfig.MAX_MESSAGES));
    }
}
