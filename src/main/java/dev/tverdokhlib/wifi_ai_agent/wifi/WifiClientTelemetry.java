package dev.tverdokhlib.wifi_ai_agent.wifi;

/**
 * Normalized per-client telemetry as observed by RouterOS (registration table).
 * Connected stations are not queried directly.
 */
public record WifiClientTelemetry(
		String macAddress,
		String interfaceName,
		Integer rssiDbm,
		String txRate,
		String rxRate,
		Long txBytes,
		Long rxBytes,
		Long txPackets,
		Long rxPackets,
		String uptime,
		Long txRetries,
		Long rxRetries,
		Long txFramesTimedOut
) {
}
