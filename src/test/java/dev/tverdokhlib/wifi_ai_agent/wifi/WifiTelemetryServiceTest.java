package dev.tverdokhlib.wifi_ai_agent.wifi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.tverdokhlib.wifi_ai_agent.routeros.RouterOsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Verifies point 1: RouterOS Wi-Fi data is aggregated into an application snapshot. */
@ExtendWith(MockitoExtension.class)
class WifiTelemetryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-27T16:20:00Z");

    @Mock
    private RouterOsClient routerOsClient;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private WifiTelemetryService service;

    @BeforeEach
    void setUp() {
        service = new WifiTelemetryService(routerOsClient, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aggregatesInterfacesClientsAndAuthoritativeCounts() {
        when(routerOsClient.getArrayIfPresent(WifiTelemetryService.WIRELESS_INTERFACES))
                .thenReturn(Optional.of(List.of(json("""
                        {"name":"wlan1","disabled":"false","running":"true","ssid":"MT5"}
                        """))));
        when(routerOsClient.getArrayIfPresent(WifiTelemetryService.WIRELESS_REGISTRATION))
                .thenReturn(Optional.of(List.of(
                        json("""
                                {"mac-address":"AA:AA:AA:AA:AA:01","interface":"wlan1","signal-strength":"-37@54Mbps"}
                                """),
                        json("""
                                {"mac-address":"AA:AA:AA:AA:AA:02","interface":"wlan1","signal-strength":"-53@24Mbps"}
                                """))));
        when(routerOsClient.postArray(eq(WifiTelemetryService.INTERFACE_PRINT), any()))
                .thenReturn(List.of(json("""
                        {"name":"wlan1","tx-byte":"100","rx-byte":"200"}
                        """)));
        when(routerOsClient.postArray(eq(WifiTelemetryService.WIRELESS_MONITOR), any()))
                .thenReturn(List.of(json("""
                        {"channel":"5180/20/a/P(17dBm)","noise-floor":"-109"}
                        """)));

        WifiTelemetrySnapshot snapshot = service.collectSnapshot();

        assertThat(snapshot.collectedAt()).isEqualTo(NOW);
        assertThat(snapshot.totalClientCount()).isEqualTo(2);
        assertThat(snapshot.clientCountByInterface()).containsExactly(Map.entry("wlan1", 2));
        assertThat(snapshot.interfaces()).hasSize(1);
        assertThat(snapshot.interfaces().getFirst().clientCount()).isEqualTo(2);
        assertThat(snapshot.clients()).extracting(WifiClientTelemetry::macAddress)
                .containsExactly("AA:AA:AA:AA:AA:01", "AA:AA:AA:AA:AA:02");
    }

    @Test
    void fallsBackFromLegacyWirelessToWifiPackage() {
        when(routerOsClient.getArrayIfPresent(WifiTelemetryService.WIRELESS_INTERFACES))
                .thenReturn(Optional.of(List.of()));
        when(routerOsClient.getArrayIfPresent(WifiTelemetryService.WIFI_INTERFACES))
                .thenReturn(Optional.of(List.of(json("""
                        {"name":"wifi1","disabled":"false","running":"true","configuration.ssid":"Office"}
                        """))));
        when(routerOsClient.getArrayIfPresent(WifiTelemetryService.WIFI_REGISTRATION))
                .thenReturn(Optional.of(List.of(json("""
                        {"mac-address":"11:22:33:44:55:66","interface":"wifi1","signal":"-50"}
                        """))));
        when(routerOsClient.postArray(eq(WifiTelemetryService.INTERFACE_PRINT), any()))
                .thenReturn(List.of());
        when(routerOsClient.postArray(eq(WifiTelemetryService.WIFI_MONITOR), any()))
                .thenReturn(List.of());

        WifiTelemetrySnapshot snapshot = service.collectSnapshot();

        assertThat(snapshot.interfaces().getFirst().ssid()).isEqualTo("Office");
        assertThat(snapshot.clients().getFirst().rssiDbm()).isEqualTo(-50);
        verify(routerOsClient, never())
                .getArrayIfPresent(WifiTelemetryService.WIRELESS_REGISTRATION);
    }

    private JsonNode json(String value) {
        return jsonMapper.readTree(value);
    }
}
