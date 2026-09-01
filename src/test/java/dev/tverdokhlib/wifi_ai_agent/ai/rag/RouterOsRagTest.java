package dev.tverdokhlib.wifi_ai_agent.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.mockito.ArgumentCaptor;

/** Verifies only concept 4: document retrieval for RAG. */
class RouterOsRagTest {

    @Test
    void retrievesRelevantRouterOsChunksFromVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        List<Document> expected = List.of(new Document("Registration Table clients"));
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(expected);

        RouterOsRag rag = new RouterOsRag(vectorStore, 2, 0.4);

        assertThat(rag.search("registration table")).isEqualTo(expected);
        assertThat(rag.advisor()).isNotNull();
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getQuery()).isEqualTo("registration table");
        assertThat(request.getValue().getTopK()).isEqualTo(2);
        assertThat(request.getValue().getSimilarityThreshold()).isEqualTo(0.4);
    }
}
