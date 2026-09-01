package dev.tverdokhlib.wifi_ai_agent.ai.evaluation;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Concept 8: deterministic acceptance criteria for predefined agent scenarios. */
public final class AgentEvaluator {

    private AgentEvaluator() {
    }

    public static Criterion nonBlank() {
        return new Criterion("response is not blank", response -> !response.isBlank());
    }

    public static Criterion hasAgentPrefix() {
        return new Criterion("response starts with [WIFI-AGENT]",
                response -> response.stripLeading().startsWith("[WIFI-AGENT]"));
    }

    public static Criterion doesNotLeakToolProtocol() {
        return new Criterion("response does not expose tool-call protocol", response -> {
            String value = response.toLowerCase(Locale.ROOT);
            return !value.contains("\"name\":\"getwifi")
                    && !value.contains("\"parameters\":")
                    && !value.contains("tool_call")
                    && !value.contains("toolcall");
        });
    }

    public static Criterion containsAny(String description, String... terms) {
        return new Criterion(description, response -> {
            String value = response.toLowerCase(Locale.ROOT);
            return List.of(terms).stream()
                    .map(term -> term.toLowerCase(Locale.ROOT))
                    .anyMatch(value::contains);
        });
    }

    public static EvaluationResult evaluate(
            String scenarioId, String response, List<Criterion> criteria) {
        String safeResponse = response == null ? "" : response;
        List<String> failures = criteria.stream()
                .filter(criterion -> !criterion.predicate().test(safeResponse))
                .map(Criterion::description)
                .toList();
        return new EvaluationResult(scenarioId, criteria.size() - failures.size(),
                criteria.size(), failures, safeResponse);
    }

    public record Criterion(String description, Predicate<String> predicate) {
    }

    public record EvaluationResult(
            String scenarioId,
            int passedCriteria,
            int totalCriteria,
            List<String> failures,
            String response) {
        public boolean passed() {
            return failures.isEmpty();
        }

        public double score() {
            return totalCriteria == 0 ? 1.0 : (double) passedCriteria / totalCriteria;
        }

        public String summary() {
            return "%s: %d/%d (%.0f%%), failures=%s".formatted(
                    scenarioId, passedCriteria, totalCriteria, score() * 100, failures);
        }
    }
}
