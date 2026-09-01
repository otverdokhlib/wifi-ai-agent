package dev.tverdokhlib.wifi_ai_agent.ai.evaluation;

import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.doesNotLeakToolProtocol;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.evaluate;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.hasAgentPrefix;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.nonBlank;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies only concept 8: scenario output against explicit acceptance criteria. */
class AgentEvaluatorTest {

    @Test
    void scoresAResponseAgainstPredefinedAcceptanceCriteria() {
        var result = evaluate(
                "current-clients",
                "[WIFI-AGENT]\n\nThere are 3 connected clients.",
                List.of(nonBlank(), hasAgentPrefix(), doesNotLeakToolProtocol()));

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void reportsFailedAcceptanceCriteria() {
        var result = evaluate(
                "tool-protocol-leak",
                "{\"name\":\"getWifiClients\",\"parameters\":{}}",
                List.of(hasAgentPrefix(), doesNotLeakToolProtocol()));

        assertThat(result.passed()).isFalse();
        assertThat(result.failures()).containsExactly(
                "response starts with [WIFI-AGENT]",
                "response does not expose tool-call protocol");
    }
}
