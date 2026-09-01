package dev.tverdokhlib.wifi_ai_agent.ai.tools;

import java.util.List;

import dev.tverdokhlib.wifi_ai_agent.wifi.WifiClientTelemetry;
import dev.tverdokhlib.wifi_ai_agent.wifi.WifiInterfaceTelemetry;
import dev.tverdokhlib.wifi_ai_agent.wifi.WifiTelemetryService;
import dev.tverdokhlib.wifi_ai_agent.wifi.WifiTelemetrySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Concept 2: Java functions exposed to the LLM as native Spring AI tools.
 */
@Component
@RequiredArgsConstructor
public class WifiTools {

    private final WifiTelemetryService wifiTelemetryService;

    @Tool(description = """
            Get a combined snapshot of current Wi-Fi clients and interfaces from MikroTik RouterOS.
            Use only for a complete network snapshot or diagnosis, or when a question explicitly
            requires both client and interface data. Do not use for client-only or interface-only questions.
            """)
    public WifiTelemetrySnapshot getWifiTelemetry() {
        return wifiTelemetryService.collectSnapshot();
    }

    @Tool(description = "Get current Wi-Fi interfaces from MikroTik RouterOS.")
    public List<WifiInterfaceTelemetry> getWifiInterfaces() {
        return wifiTelemetryService.getInterfaces();
    }

    @Tool(description = """
            Get currently connected Wi-Fi clients from MikroTik RouterOS.
            The clientCount field is the authoritative number of currently connected clients.
            Always use clientCount when answering questions about the number of connected clients.
            """)
    public WifiClientsResult getWifiClients() {
        List<WifiClientTelemetry> clients = wifiTelemetryService.getClients();
        return new WifiClientsResult(clients.size(), clients);
    }

    public record WifiClientsResult(int clientCount, List<WifiClientTelemetry> clients) {
    }
}
