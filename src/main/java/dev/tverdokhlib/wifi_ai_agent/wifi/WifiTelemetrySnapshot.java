package dev.tverdokhlib.wifi_ai_agent.wifi;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WifiTelemetrySnapshot(
		Instant collectedAt,
		List<WifiInterfaceTelemetry> interfaces,
		List<WifiClientTelemetry> clients,
		int totalClientCount,
		Map<String, Integer> clientCountByInterface) {

	public WifiTelemetrySnapshot {
		interfaces = List.copyOf(interfaces == null ? List.of() : interfaces);
		clients = List.copyOf(clients == null ? List.of() : clients);
		clientCountByInterface = Collections.unmodifiableMap(new LinkedHashMap<>(
				clientCountByInterface == null ? Map.of() : clientCountByInterface));
	}
}
