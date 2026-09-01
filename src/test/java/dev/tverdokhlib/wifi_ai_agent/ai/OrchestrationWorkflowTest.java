package dev.tverdokhlib.wifi_ai_agent.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Verifies only concept 6: routing to tools, RAG, both, or LLM only. */
class OrchestrationWorkflowTest {

    private final RequestClassifier classifier = new RequestClassifier();

    @ParameterizedTest
    @CsvSource({
            "'How many clients are connected?', CURRENT_STATE",
            "'Show current WiFi telemetry', CURRENT_STATE",
            "'According to the RouterOS manual, what is Registration Table?', DOCUMENTATION",
            "'Where can I find the documentation?', DOCUMENTATION",
            "'Why is my WiFi connection unstable?', TROUBLESHOOTING",
            "'Troubleshoot poor connection signal', TROUBLESHOOTING",
            "'Hello, what can you do?', GENERAL"
    })
    void classifiesRequest(String question, RequestType expectedType) {
        assertThat(classifier.classify(question)).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @CsvSource({
            "'Why does RouterOS show poor connection?', TROUBLESHOOTING",
            "'According to the manual, which clients are connected?', DOCUMENTATION"
    })
    void appliesClassificationPrecedence(String question, RequestType expectedType) {
        assertThat(classifier.classify(question)).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @CsvSource({
            "CURRENT_STATE, true, false",
            "DOCUMENTATION, false, true",
            "TROUBLESHOOTING, true, true",
            "GENERAL, false, false"
    })
    void mapsEveryRequestTypeToAnExplicitWorkflow(
            RequestType requestType, boolean tools, boolean rag) {
        assertThat(requestType.usesTools()).isEqualTo(tools);
        assertThat(requestType.usesRag()).isEqualTo(rag);
    }
}
