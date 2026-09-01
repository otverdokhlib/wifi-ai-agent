package dev.tverdokhlib.wifi_ai_agent.wifi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;

/**
 * Maps RouterOS REST JSON (wireless/wifi + /interface stats + registration-table +
 * optional monitor) onto normalized application DTOs. RouterOS-specific field names
 * stay in this class.
 * <p>
 * Legacy {@code /interface/wireless} exposes retransmission information indirectly:
 * {@code frames} is the number of frames that should be sent over the wireless link,
 * while {@code hw-frames} is the number actually sent by the driver. Their positive
 * difference is exposed as the normalized retry count. New {@code /interface/wifi}
 * does not expose an equivalent retry counter in its registration table, so retry
 * values remain {@code null} there.
 */
final class RouterOsWifiMapper {

	private static final Pattern FIRST_INTEGER_PATTERN = Pattern.compile("(-?\\d+)");

	private RouterOsWifiMapper() {
	}

	static String interfaceName(JsonNode wirelessOrWifi) {
		return text(wirelessOrWifi, "name");
	}

	static WifiInterfaceTelemetry mapInterface(
			JsonNode wirelessOrWifi,
			JsonNode interfaceCounters,
			JsonNode monitor,
			int clientCount
	) {
		Boolean disabled = bool(wirelessOrWifi, "disabled");
		Boolean enabled = disabled == null ? null : !disabled;
		String currentChannel = text(monitor, "channel");
		Integer currentFrequency = firstInteger(currentChannel);
		if (currentFrequency == null) {
			currentFrequency = firstInteger(firstNonBlank(
					text(wirelessOrWifi, "frequency"),
					text(wirelessOrWifi, "channel.frequency"),
					nestedText(wirelessOrWifi, "channel", "frequency")));
		}

		return new WifiInterfaceTelemetry(
				text(wirelessOrWifi, "name"),
				enabled,
				bool(wirelessOrWifi, "running"),
				firstNonBlank(
						text(monitor, "ssid"),
						text(wirelessOrWifi, "ssid"),
						text(wirelessOrWifi, "configuration.ssid"),
						nestedText(wirelessOrWifi, "configuration", "ssid")),
				firstNonBlank(
						text(monitor, "band"),
						text(wirelessOrWifi, "band"),
						text(wirelessOrWifi, "channel.band"),
						nestedText(wirelessOrWifi, "channel", "band")),
				currentFrequency,
				currentChannel,
				firstNonBlank(
						text(wirelessOrWifi, "channel-width"),
						text(wirelessOrWifi, "channel.width"),
						nestedText(wirelessOrWifi, "channel", "width")),
				longValue(interfaceCounters, "tx-byte"),
				longValue(interfaceCounters, "rx-byte"),
				longValue(interfaceCounters, "tx-packet"),
				longValue(interfaceCounters, "rx-packet"),
				longValue(interfaceCounters, "tx-error"),
				longValue(interfaceCounters, "rx-error"),
				longValue(interfaceCounters, "tx-drop"),
				longValue(interfaceCounters, "rx-drop"),
				longValue(interfaceCounters, "tx-queue-drop"),
				integer(monitor, "noise-floor"),
				clientCount
		);
	}

	static WifiClientTelemetry mapClient(JsonNode registration) {
		TxRx bytes = txRx(registration, "bytes");
		TxRx packets = txRx(registration, "packets");
		TxRx frames = txRx(registration, "frames");
		TxRx hwFrames = txRx(registration, "hw-frames");

		return new WifiClientTelemetry(
				text(registration, "mac-address"),
				text(registration, "interface"),
				rssiDbm(registration),
				text(registration, "tx-rate"),
				text(registration, "rx-rate"),
				bytes.tx(),
				bytes.rx(),
				packets.tx(),
				packets.rx(),
				text(registration, "uptime"),
				retries(frames.tx(), hwFrames.tx()),
				retries(frames.rx(), hwFrames.rx()),
				longValue(registration, "tx-frames-timed-out")
		);
	}

	static Integer rssiDbm(JsonNode registration) {
		Integer fromWifiSignal = integer(registration, "signal");
		if (fromWifiSignal != null) {
			return fromWifiSignal;
		}
		return firstInteger(text(registration, "signal-strength"));
	}

	static String text(JsonNode parent, String field) {
		if (parent == null) {
			return null;
		}
		JsonNode node = parent.get(field);
		if (node == null || node.isNull() || node.isMissingNode() || node.isArray() || node.isObject()) {
			return null;
		}
		String value = node.asString();
		return value == null || value.isBlank() ? null : value;
	}

	private static String nestedText(JsonNode parent, String objectField, String field) {
		if (parent == null) {
			return null;
		}
		JsonNode nested = parent.get(objectField);
		return text(nested, field);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static Boolean bool(JsonNode parent, String field) {
		String value = text(parent, field);
		if (value == null) {
			return null;
		}
		if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private static Integer integer(JsonNode parent, String field) {
		String value = text(parent, field);
		if (value == null) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Integer firstInteger(String value) {
		if (value == null) {
			return null;
		}
		Matcher matcher = FIRST_INTEGER_PATTERN.matcher(value);
		if (!matcher.find()) {
			return null;
		}
		try {
			return Integer.valueOf(matcher.group(1));
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Long longValue(JsonNode parent, String field) {
		String value = text(parent, field);
		return parseLong(value);
	}

	private static TxRx txRx(JsonNode parent, String field) {
		if (parent == null) {
			return TxRx.EMPTY;
		}
		JsonNode node = parent.get(field);
		if (node == null || node.isNull() || node.isMissingNode()) {
			return TxRx.EMPTY;
		}
		if (node.isArray() && node.size() >= 2) {
			return new TxRx(asLong(node.get(0)), asLong(node.get(1)));
		}
		String raw = node.isArray() || node.isObject() ? null : node.asString();
		if (raw == null) {
			return TxRx.EMPTY;
		}
		int comma = raw.indexOf(',');
		if (comma < 0) {
			return TxRx.EMPTY;
		}
		return new TxRx(parseLong(raw.substring(0, comma)), parseLong(raw.substring(comma + 1)));
	}

	private static Long retries(Long frames, Long hwFrames) {
		if (frames == null || hwFrames == null) {
			return null;
		}
		return Math.max(0L, hwFrames - frames);
	}

	private static Long asLong(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode() || node.isArray() || node.isObject()) {
			return null;
		}
		return parseLong(node.asString());
	}

	private static Long parseLong(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(value.trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private record TxRx(Long tx, Long rx) {
		private static final TxRx EMPTY = new TxRx(null, null);
	}
}
