package dev.tverdokhlib.wifi_ai_agent.wifi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/router/wifi")
public class WifiTelemetryController {

	private final WifiTelemetryService wifiTelemetryService;

	public WifiTelemetryController(WifiTelemetryService wifiTelemetryService) {
		this.wifiTelemetryService = wifiTelemetryService;
	}

	@GetMapping("/telemetry")
	public WifiTelemetrySnapshot telemetry() {
		return wifiTelemetryService.collectSnapshot();
	}

	@GetMapping("/interfaces")
	public List<WifiInterfaceTelemetry> interfaces() {
		return wifiTelemetryService.getInterfaces();
	}

	@GetMapping("/clients")
	public List<WifiClientTelemetry> clients() {
		return wifiTelemetryService.getClients();
	}
}
