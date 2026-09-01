package dev.tverdokhlib.wifi_ai_agent.ai.rag;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Educational endpoint for inspecting retrieval independently from the LLM. */
@RestController
@RequiredArgsConstructor
public class RagSearchController {

    private final RouterOsRag routerOsRag;

    @GetMapping("/api/rag/search")
    public List<Document> search(@RequestParam String query) {
        return routerOsRag.search(query);
    }
}
