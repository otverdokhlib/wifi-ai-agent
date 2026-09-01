package dev.tverdokhlib.wifi_ai_agent.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Verifies only concept 3: the system prompt contract. */
class WifiAgentPromptTest {

    @Test
    void definesRoleRulesConstraintsAndResponseBehavior() {
        String prompt = new WifiAgentPrompt(
                new ClassPathResource("prompts/wifi-agent-system.txt")).content();

        assertThat(prompt)
                .contains("You are an AI Wi-Fi Troubleshooting Agent")
                .contains("Use available RouterOS tools")
                .contains("use the retrieved reference context")
                .contains("Never invent, assume, estimate, or fabricate telemetry")
                .contains("Observed facts:")
                .contains("Conclusion:")
                .contains("Recommendation:")
                .contains("under 150 words")
                .contains("at most three telemetry facts")
                .contains("Never omit this section")
                .contains("[WIFI-AGENT]");
    }
}
