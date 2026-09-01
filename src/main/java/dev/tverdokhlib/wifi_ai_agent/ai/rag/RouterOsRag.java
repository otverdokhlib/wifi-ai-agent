package dev.tverdokhlib.wifi_ai_agent.ai.rag;

import java.util.List;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Concept 4: retrieves RouterOS documentation and creates the advisor that adds
 * retrieved chunks to the LLM context.
 */
@Component
public class RouterOsRag {

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public RouterOsRag(
            VectorStore vectorStore,
            @Value("${wifi.agent.rag.top-k:2}") int topK,
            @Value("${wifi.agent.rag.similarity-threshold:0.4}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<Document> search(String query) {
        return vectorStore.similaritySearch(searchRequest(query));
    }

    public QuestionAnswerAdvisor advisor() {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest(""))
                .build();
    }

    private SearchRequest searchRequest(String query) {
        return SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
    }
}
