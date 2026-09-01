package dev.tverdokhlib.wifi_ai_agent.routeros;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RouterOsProperties.class)
public class RouterOsClientConfig {

	@Bean
	RestClient routerOsRestClient(RestClient.Builder builder, RouterOsProperties properties) {
		RestClient.Builder clientBuilder = builder.baseUrl(properties.baseUrl());

		if (StringUtils.hasText(properties.username())) {
			clientBuilder.defaultHeaders(headers -> headers.setBasicAuth(
					properties.username(),
					properties.password() == null ? "" : properties.password()));
		}

		if (properties.sslInsecure()) {
			clientBuilder.requestFactory(DevelopmentOnlyInsecureTls.requestFactory());
		}

		return clientBuilder.build();
	}
}
