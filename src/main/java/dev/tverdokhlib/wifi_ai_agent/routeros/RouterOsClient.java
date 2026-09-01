package dev.tverdokhlib.wifi_ai_agent.routeros;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RouterOsClient {

	private final RestClient restClient;
	private final JsonMapper jsonMapper;

	public RouterOsClient(@Qualifier("routerOsRestClient") RestClient restClient, JsonMapper jsonMapper) {
		this.restClient = restClient;
		this.jsonMapper = jsonMapper;
	}

	public SystemResource getSystemResource() {
		JsonNode body = getJson("/rest/system/resource");
		return parseSystemResource(body);
	}

	public JsonNode getJson(String path) {
		return execute(() -> restClient.get()
				.uri(path)
				.retrieve()
				.body(JsonNode.class));
	}

	/**
	 * GET a RouterOS REST list resource. Missing menus (HTTP 400/404 "no such command")
	 * are treated as absent rather than a transport failure so callers can detect
	 * {@code /interface/wifi} vs {@code /interface/wireless}.
	 */
	public Optional<List<JsonNode>> getArrayIfPresent(String path) {
		try {
			return Optional.of(asArray(getJson(path), path));
		}
		catch (RouterOsException ex) {
			if (ex.getCause() instanceof RestClientResponseException responseEx && isMissingMenu(responseEx)) {
				return Optional.empty();
			}
			throw ex;
		}
	}

	public JsonNode postJson(String path, Object body) {
		return execute(() -> restClient.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(JsonNode.class));
	}

	public List<JsonNode> postArray(String path, Object body) {
		return asArray(postJson(path, body), path);
	}

	private JsonNode execute(Supplier<JsonNode> request) {
		try {
			return request.get();
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))
					|| ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(403))) {
				throw RouterOsException.authenticationFailed("RouterOS authentication failed");
			}
			throw RouterOsException.requestFailed(
					"RouterOS request failed with HTTP " + ex.getStatusCode().value(), ex);
		}
		catch (ResourceAccessException ex) {
			throw RouterOsException.unavailable("Unable to reach RouterOS", ex);
		}
	}

	private static boolean isMissingMenu(RestClientResponseException ex) {
		if (!(ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(400))
				|| ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404)))) {
			return false;
		}
		String body = ex.getResponseBodyAsString();
		return body != null && body.contains("no such command");
	}

	private static List<JsonNode> asArray(JsonNode body, String path) {
		if (body == null || body.isNull() || body.isMissingNode()) {
			return List.of();
		}
		if (!body.isArray()) {
			throw RouterOsException.requestFailed("Expected a JSON array from RouterOS " + path, null);
		}
		List<JsonNode> items = new ArrayList<>(body.size());
		body.forEach(items::add);
		return List.copyOf(items);
	}

	private SystemResource parseSystemResource(JsonNode body) {
		if (body == null || body.isNull() || body.isMissingNode()) {
			throw RouterOsException.requestFailed("Empty system resource response from RouterOS", null);
		}
		JsonNode resourceNode = body.isArray() ? body.get(0) : body;
		if (resourceNode == null || resourceNode.isNull()) {
			throw RouterOsException.requestFailed("Empty system resource response from RouterOS", null);
		}
		return jsonMapper.convertValue(resourceNode, SystemResource.class);
	}
}
