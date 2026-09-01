package dev.tverdokhlib.wifi_ai_agent.routeros;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * DEVELOPMENT ONLY.
 * <p>
 * Trusts any TLS certificate and skips hostname verification so a local RouterOS
 * device with a self-signed certificate can be reached. Do not use this in
 * production; install a trusted certificate (or pin the router CA) instead.
 */
final class DevelopmentOnlyInsecureTls {

	private DevelopmentOnlyInsecureTls() {
	}

	static JdkClientHttpRequestFactory requestFactory() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { InsecureTrustManager.INSTANCE }, null);

			SSLParameters sslParameters = new SSLParameters();
			// Empty algorithm disables HTTPS endpoint identification (hostname check).
			sslParameters.setEndpointIdentificationAlgorithm("");

			HttpClient httpClient = HttpClient.newBuilder()
					.sslContext(sslContext)
					.sslParameters(sslParameters)
					.build();
			return new JdkClientHttpRequestFactory(httpClient);
		}
		catch (NoSuchAlgorithmException | KeyManagementException ex) {
			throw new IllegalStateException("Failed to initialize development-only insecure TLS", ex);
		}
	}

	private static final class InsecureTrustManager extends X509ExtendedTrustManager {

		private static final InsecureTrustManager INSTANCE = new InsecureTrustManager();

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
		}
	}
}
