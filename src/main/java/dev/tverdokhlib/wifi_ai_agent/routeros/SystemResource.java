package dev.tverdokhlib.wifi_ai_agent.routeros;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RouterOS {@code /system/resource} payload. Values are strings because RouterOS
 * encodes all JSON fields as strings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemResource(
		@JsonProperty("architecture-name") String architectureName,
		@JsonProperty("board-name") String boardName,
		@JsonProperty("build-time") String buildTime,
		@JsonProperty("cpu") String cpu,
		@JsonProperty("cpu-count") String cpuCount,
		@JsonProperty("cpu-frequency") String cpuFrequency,
		@JsonProperty("cpu-load") String cpuLoad,
		@JsonProperty("factory-software") String factorySoftware,
		@JsonProperty("free-hdd-space") String freeHddSpace,
		@JsonProperty("free-memory") String freeMemory,
		@JsonProperty("platform") String platform,
		@JsonProperty("total-hdd-space") String totalHddSpace,
		@JsonProperty("total-memory") String totalMemory,
		@JsonProperty("uptime") String uptime,
		@JsonProperty("version") String version,
		@JsonProperty("write-sect-since-reboot") String writeSectSinceReboot,
		@JsonProperty("write-sect-total") String writeSectTotal,
		@JsonProperty("bad-blocks") String badBlocks
) {
}
