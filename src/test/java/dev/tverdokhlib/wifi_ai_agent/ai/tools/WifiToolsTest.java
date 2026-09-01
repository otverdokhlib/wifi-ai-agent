package dev.tverdokhlib.wifi_ai_agent.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import dev.tverdokhlib.wifi_ai_agent.wifi.WifiTelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

/** Verifies only concept 2: Java function exposure and invocation. */
class WifiToolsTest {

    @Test
    void exposesTheThreeJavaFunctionsAsNativeLlmTools() {
        WifiTools tools = new WifiTools(mock(WifiTelemetryService.class));

        assertThat(ToolCallbacks.from(tools))
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder(
                        "getWifiTelemetry", "getWifiInterfaces", "getWifiClients");
    }

    @Test
    void clientToolInvokesTelemetryServiceAndReturnsAuthoritativeCount() {
        WifiTelemetryService telemetry = mock(WifiTelemetryService.class);
        when(telemetry.getClients()).thenReturn(List.of());

        WifiTools.WifiClientsResult result = new WifiTools(telemetry).getWifiClients();

        assertThat(result.clientCount()).isZero();
        assertThat(result.clients()).isEmpty();
        verify(telemetry).getClients();
    }
}
