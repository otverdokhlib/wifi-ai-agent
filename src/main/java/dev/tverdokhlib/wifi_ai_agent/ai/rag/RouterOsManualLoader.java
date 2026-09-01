package dev.tverdokhlib.wifi_ai_agent.ai.rag;

import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RouterOsManualLoader implements CommandLineRunner {

    private final SimpleVectorStore vectorStore;
    private final int chunkSize;
    private final File vectorStoreFile;

    public RouterOsManualLoader(
            SimpleVectorStore vectorStore,
            @org.springframework.beans.factory.annotation.Value("${wifi.agent.rag.chunk-size:500}")
            int chunkSize) {
        this.vectorStore = vectorStore;
        this.chunkSize = chunkSize;
        this.vectorStoreFile = chunkSize == 1000
                ? new File("data/routeros-vector-store.json")
                : new File("data/routeros-vector-store-chunk-" + chunkSize + ".json");
    }

    @Override
    public void run(String... args) {
        if (vectorStoreFile.exists()) {
            vectorStore.load(vectorStoreFile);
            log.info("RAG vector store loaded from {}", vectorStoreFile.getPath());
            return;
        }

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                "classpath:/rag/MikroTik_RouterOS_v28_Reference_Manual.pdf");
        List<Document> documents = pdfReader.read();
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .build()
                .apply(documents);

        vectorStore.write(chunks);
        vectorStoreFile.getParentFile().mkdirs();
        vectorStore.save(vectorStoreFile);
        log.info("RAG created {} chunks and saved vector store to {}",
                chunks.size(), vectorStoreFile.getPath());
    }
}
