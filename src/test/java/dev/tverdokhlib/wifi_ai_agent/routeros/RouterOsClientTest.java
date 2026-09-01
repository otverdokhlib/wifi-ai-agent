package dev.tverdokhlib.wifi_ai_agent.routeros;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Verifies point 1: real RouterOS REST request and response handling. */
class RouterOsClientTest {

    private MockRestServiceServer server;
    private RouterOsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://192.168.88.1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RouterOsClient(builder.build(), JsonMapper.builder().build());
    }

    @Test
    void mapsRouterOsSystemResourceResponse() {
        server.expect(requestTo("https://192.168.88.1/rest/system/resource"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"version":"7.24.1","board-name":"RB4011","platform":"MikroTik"}
                        """, MediaType.APPLICATION_JSON));

        SystemResource resource = client.getSystemResource();

        assertThat(resource.version()).isEqualTo("7.24.1");
        assertThat(resource.boardName()).isEqualTo("RB4011");
        assertThat(resource.platform()).isEqualTo("MikroTik");
        server.verify();
    }

    @Test
    void treatsMissingRouterOsMenuAsAbsent() {
        server.expect(requestTo("https://192.168.88.1/rest/interface/wifiwave2"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"detail":"no such command or directory (wifiwave2)","error":400}
                                """));

        assertThat(client.getArrayIfPresent("/rest/interface/wifiwave2")).isEmpty();
        server.verify();
    }

    @Test
    void mapsUnauthorizedResponseToDomainException() {
        server.expect(requestTo("https://192.168.88.1/rest/system/resource"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(client::getSystemResource)
                .isInstanceOf(RouterOsException.class)
                .hasMessage("RouterOS authentication failed")
                .extracting(error -> ((RouterOsException) error).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
