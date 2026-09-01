package dev.tverdokhlib.wifi_ai_agent.routeros;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/router")
public class RouterOsController {

	private final RouterOsClient routerOsClient;

	public RouterOsController(RouterOsClient routerOsClient) {
		this.routerOsClient = routerOsClient;
	}

	@GetMapping("/system-resource")
	public SystemResource systemResource() {
		return routerOsClient.getSystemResource();
	}
}
