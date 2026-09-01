package dev.tverdokhlib.wifi_ai_agent.ai.evaluation;

import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.containsAny;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.doesNotLeakToolProtocol;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.evaluate;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.hasAgentPrefix;
import static dev.tverdokhlib.wifi_ai_agent.ai.evaluation.AgentEvaluator.nonBlank;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import dev.tverdokhlib.wifi_ai_agent.ai.WifiAgentService;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end behavioral evaluation against Ollama, RouterOS and the local RAG store.
 * Enable explicitly with {@code RUN_AI_EVALUATIONS=true}.
 */
@Tag("evaluation")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_AI_EVALUATIONS", matches = "(?i)true")
class WifiAgentBehaviorEvaluationTest {

    private static final List<AgentEvaluator.Criterion> COMMON_CRITERIA = List.of(
            nonBlank(),
            hasAgentPrefix(),
            doesNotLeakToolProtocol());

    @Autowired
    private WifiAgentService wifiAgentService;

    @TestFactory
    Stream<DynamicTest> predefinedScenariosMeetAcceptanceCriteria() {
        return scenarios().stream()
                .map(scenario -> DynamicTest.dynamicTest(
                        scenario.id(),
                        () -> evaluateScenario(scenario)));
    }

    private void evaluateScenario(Scenario scenario) {
        String conversationId = "evaluation-" + scenario.id() + "-" + UUID.randomUUID();

        for (Turn turn : scenario.turns()) {
            String response = wifiAgentService.ask(conversationId, turn.question());
            var result = evaluate(
                    scenario.id() + "/" + turn.id(),
                    response,
                    concat(COMMON_CRITERIA, turn.criteria()));

            System.out.printf("%nEvaluation: %s%nRequest: %s%nResponse:%n%s%n%n",
                    result.summary(), turn.question(), result.response());

            assertThat(result.passed())
                    .as("%s%nResponse:%n%s", result.summary(), result.response())
                    .isTrue();
        }
    }

    private List<Scenario> scenarios() {
        return List.of(
                new Scenario("current-state", List.of(
                        new Turn(
                                "connected-clients",
                                "How many Wi-Fi clients are currently connected?",
                                List.of(containsAny(
                                        "answer refers to connected clients",
                                        "client", "connected"))))),

                new Scenario("documentation", List.of(
                        new Turn(
                                "registration-table",
                                "According to the RouterOS manual, what is Registration Table?",
                                List.of(containsAny(
                                        "answer addresses Registration Table",
                                        "registration"))))),

                new Scenario("troubleshooting", List.of(
                        new Turn(
                                "diagnosis",
                                "Why is the current Wi-Fi slow or unstable? Diagnose it from available evidence.",
                                List.of(
                                        containsAny(
                                                "answer refers to observed telemetry or insufficient evidence",
                                                "signal", "rate", "traffic", "telemetry", "data",
                                                "evidence", "insufficient", "unknown"),
                                        containsAny(
                                                "answer gives a recommendation or explicitly reports no problem",
                                                "recommend", "check", "consider", "no clear problem",
                                                "cannot", "insufficient"))))),

                new Scenario("general", List.of(
                        new Turn(
                                "capabilities",
                                "What kinds of Wi-Fi questions can you help me with?",
                                List.of(containsAny(
                                        "answer describes Wi-Fi assistance",
                                        "wi-fi", "wifi", "network", "router"))))),

                new Scenario("conversation-memory", List.of(
                        new Turn(
                                "clients",
                                "List the currently connected Wi-Fi clients.",
                                List.of(containsAny(
                                        "answer refers to clients",
                                        "client", "connected"))),
                        new Turn(
                                "follow-up",
                                "Which of them has the worst signal?",
                                List.of(containsAny(
                                        "follow-up resolves client signal context",
                                        "signal", "rssi", "dbm", "client"))))));
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private record Scenario(String id, List<Turn> turns) {
    }

    private record Turn(
            String id,
            String question,
            List<AgentEvaluator.Criterion> criteria) {
    }
}
