package dev.tverdokhlib.wifi_ai_agent.ai.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import dev.tverdokhlib.wifi_ai_agent.ai.tools.WifiTools;

/** Verifies only concept 7: bounded-loop tool selection. */
class WifiAgentLoopTest {

    private final WifiAgentLoop agentLoop = new WifiAgentLoop(
            mock(WifiTools.class),
            2,
            4);

    @Test
    void exposesOnlyClientToolForClientQuestions() {
        assertThat(agentLoop.allowedToolNames("Which connected client has the worst signal?"))
                .containsExactly("getWifiClients");
    }

    @Test
    void exposesOnlyInterfaceToolForInterfaceQuestions() {
        assertThat(agentLoop.allowedToolNames("Which interface uses channel 11?"))
                .containsExactly("getWifiInterfaces");
    }

    @Test
    void exposesCombinedTelemetryForFullDiagnosis() {
        assertThat(agentLoop.allowedToolNames("Why is the Wi-Fi slow and unstable?"))
                .containsExactly("getWifiTelemetry");
    }

    @Test
    void exposesCombinedTelemetryWhenClientsAndInterfacesAreBothNeeded() {
        assertThat(agentLoop.allowedToolNames(
                "Compare connected clients with their interface channels"))
                .containsExactly("getWifiTelemetry");
    }

    @Test
    void exposesExplicitStopConditionsForTheLoop() {
        assertThat(agentLoop.limits())
                .isEqualTo(new WifiAgentLoop.Limits(2, 4));
    }
}
