package dev.tverdokhlib.wifi_ai_agent.ai;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class RequestClassifier {

    public RequestType classify(String question) {
        String q = question.toLowerCase(Locale.ROOT);

        if (isTroubleshooting(q)) {
            return RequestType.TROUBLESHOOTING;
        }

        if (isDocumentation(q)) {
            return RequestType.DOCUMENTATION;
        }

        if (isCurrentState(q)) {
            return RequestType.CURRENT_STATE;
        }

        return RequestType.GENERAL;
    }

    private boolean isDocumentation(String q) {
        return q.contains("manual")
                || q.contains("documentation")
                || q.contains("routeros reference")
                || q.contains("according to")
                || q.contains("what does routeros");
    }

    private boolean isTroubleshooting(String q) {
        return q.contains("why")
                || q.contains("problem")
                || q.contains("issue")
                || q.contains("slow")
                || q.contains("unstable")
                || q.contains("disconnect")
                || q.contains("poor connection")
                || q.contains("troubleshoot");
    }

    private boolean isCurrentState(String q) {
        return q.contains("connected")
                || q.contains("client")
                || q.contains("signal")
                || q.contains("rssi")
                || q.contains("rx rate")
                || q.contains("tx rate")
                || q.contains("interface")
                || q.contains("telemetry")
                || q.contains("current wifi");
    }
}
