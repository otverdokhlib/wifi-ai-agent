package dev.tverdokhlib.wifi_ai_agent.ai.loop;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import dev.tverdokhlib.wifi_ai_agent.ai.tools.WifiTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Concept 7: bounded observe-decide-act loop driven by tool results. */
@Component
@Slf4j
public class WifiAgentLoop {

    private final WifiTools wifiTools;
    private final ToolCallingAdvisor advisor;
    private final Limits limits;

    public WifiAgentLoop(
            WifiTools wifiTools,
            @Value("${wifi.agent.loop.max-calls-per-tool:2}") int maxCallsPerTool,
            @Value("${wifi.agent.loop.max-total-tool-calls:4}") int maxTotalToolCalls) {
        this.wifiTools = wifiTools;
        this.limits = new Limits(maxCallsPerTool, maxTotalToolCalls);
        ToolCallingManager manager = ToolCallingManager.builder()
                .maxCallsPerTool(maxCallsPerTool)
                .maxTotalToolCalls(maxTotalToolCalls)
                .onLimitExceeded(ToolCallLimitBehavior.RETURN_ERROR_RESPONSE)
                .build();
        this.advisor = ToolCallingAdvisor.builder().toolCallingManager(manager).build();
    }

    public ToolCallingAdvisor advisor() {
        return advisor;
    }

    public Limits limits() {
        return limits;
    }

    public ToolCallback[] toolsFor(String question) {
        Set<String> allowedTools = allowedToolNames(question);
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(wifiTools))
                .filter(callback -> allowedTools.contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        log.info("Agent loop enabled: tools={}", allowedTools);
        return callbacks;
    }

    public Set<String> allowedToolNames(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        boolean clients = containsAny(q, "client", "connected", "device", "station", "signal",
                "rssi", "rx rate", "tx rate", "traffic", "uptime", "which of them",
                "one of them", "they");
        boolean interfaces = containsAny(q, "interface", "radio", "channel", "frequency", "ssid");

        if (clients && !interfaces) return Set.of("getWifiClients");
        if (interfaces && !clients) return Set.of("getWifiInterfaces");
        return Set.of("getWifiTelemetry");
    }

    private boolean containsAny(String text, String... terms) {
        return Arrays.stream(terms).anyMatch(text::contains);
    }

    /** Explicit stop conditions for the observe-decide-act cycle. */
    public record Limits(int maxCallsPerTool, int maxTotalToolCalls) {
    }
}
