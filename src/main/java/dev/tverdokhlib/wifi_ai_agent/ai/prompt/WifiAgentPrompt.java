package dev.tverdokhlib.wifi_ai_agent.ai.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Concept 3: one explicit home for the agent role, rules and response contract.
 */
@Component
public class WifiAgentPrompt {

    private final Resource systemPrompt;

    public WifiAgentPrompt(@Value("classpath:prompts/wifi-agent-system.txt") Resource systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Resource resource() {
        return systemPrompt;
    }

    public String content() {
        try {
            return systemPrompt.getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Cannot read Wi-Fi agent system prompt", exception);
        }
    }
}
