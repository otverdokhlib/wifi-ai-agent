package dev.tverdokhlib.wifi_ai_agent.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WifiAgentService {

    private final WifiAgentOrchestrator orchestrator;

    public String ask(String conversationId, String question) {
        return orchestrator.ask(conversationId, question);
    }
}
