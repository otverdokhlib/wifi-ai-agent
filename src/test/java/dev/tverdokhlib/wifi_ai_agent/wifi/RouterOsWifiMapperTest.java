package dev.tverdokhlib.wifi_ai_agent.wifi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Verifies point 1: conversion of RouterOS JSON fields into Wi-Fi telemetry. */
class RouterOsWifiMapperTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsInterfaceStateCountersAndMonitorData() {
        JsonNode routerInterface = json("""
                {"name":"wlan1","disabled":"false","running":"true","ssid":"MT5",
                 "band":"5ghz-a","frequency":"5180","channel-width":"20mhz"}
                """);
        JsonNode counters = json("""
                {"name":"wlan1","tx-byte":"100","rx-byte":"200","tx-error":"5","rx-drop":"8"}
                """);
        JsonNode monitor = json("""
                {"channel":"5180/20/a/P(17dBm)","noise-floor":"-109"}
                """);

        WifiInterfaceTelemetry telemetry =
                RouterOsWifiMapper.mapInterface(routerInterface, counters, monitor, 2);

        assertThat(telemetry.name()).isEqualTo("wlan1");
        assertThat(telemetry.enabled()).isTrue();
        assertThat(telemetry.running()).isTrue();
        assertThat(telemetry.frequencyMhz()).isEqualTo(5180);
        assertThat(telemetry.channel()).isEqualTo("5180/20/a/P(17dBm)");
        assertThat(telemetry.txBytes()).isEqualTo(100L);
        assertThat(telemetry.rxBytes()).isEqualTo(200L);
        assertThat(telemetry.noiseFloorDbm()).isEqualTo(-109);
        assertThat(telemetry.clientCount()).isEqualTo(2);
    }

    @Test
    void mapsRegistrationTableClientMetrics() {
        WifiClientTelemetry client = RouterOsWifiMapper.mapClient(json("""
                {"mac-address":"AA:BB:CC:DD:EE:FF","interface":"wlan2",
                 "signal-strength":"-39@1Mbps","tx-rate":"54Mbps","rx-rate":"48Mbps",
                 "bytes":"32293,293301","packets":"619,976","uptime":"2h36m7s"}
                """));

        assertThat(client.macAddress()).isEqualTo("AA:BB:CC:DD:EE:FF");
        assertThat(client.interfaceName()).isEqualTo("wlan2");
        assertThat(client.rssiDbm()).isEqualTo(-39);
        assertThat(client.txRate()).isEqualTo("54Mbps");
        assertThat(client.rxRate()).isEqualTo("48Mbps");
        assertThat(client.txBytes()).isEqualTo(32293L);
        assertThat(client.rxBytes()).isEqualTo(293301L);
    }

    private JsonNode json(String value) {
        return jsonMapper.readTree(value);
    }
}
