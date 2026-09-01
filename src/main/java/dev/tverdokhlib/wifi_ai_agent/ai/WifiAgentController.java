package dev.tverdokhlib.wifi_ai_agent.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WifiAgentController {

    private final WifiAgentService wifiAgentService;

    @PostMapping(
            value = "/api/agent/ask",
            consumes = "text/plain",
            produces = "text/plain"
    )
    public String ask(
            @RequestParam String conversationId,
            @RequestBody String question) {

        return wifiAgentService.ask(conversationId, question);
    }

}
