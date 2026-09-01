package dev.tverdokhlib.wifi_ai_agent.wifi;

/**
 * Normalized Wi-Fi access-point / interface telemetry. Values are taken from
 * RouterOS when the device exposes them; otherwise they are {@code null}.
 */
public record WifiInterfaceTelemetry(
		String name,
		Boolean enabled,
		Boolean running,
		String ssid,
		String band,
		Integer frequencyMhz,
		String channel,
		String channelWidth,
		Long txBytes,
		Long rxBytes,
		Long txPackets,
		Long rxPackets,
		Long txErrors,
		Long rxErrors,
		Long txDrops,
		Long rxDrops,
		Long txQueueDrops,
		Integer noiseFloorDbm,
		Integer clientCount
) {
}
