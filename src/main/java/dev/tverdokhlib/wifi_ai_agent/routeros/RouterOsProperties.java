package dev.tverdokhlib.wifi_ai_agent.routeros;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeros")
public record RouterOsProperties(
		String baseUrl,
		String username,
		String password,
		Ssl ssl
) {

	public record Ssl(boolean insecure) {
	}

	public boolean sslInsecure() {
		return ssl != null && ssl.insecure();
	}
}
