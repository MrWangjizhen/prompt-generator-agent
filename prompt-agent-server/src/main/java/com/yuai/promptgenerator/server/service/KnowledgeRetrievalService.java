package com.yuai.promptgenerator.server.service;

import java.util.List;

import com.yuai.promptgenerator.server.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalService.class);
    private final VectorStore vectorStore;
    private final AppProperties.Rag properties;

    public KnowledgeRetrievalService(@Qualifier("knowledgeVectorStore") VectorStore vectorStore,
            AppProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties.getRag();
    }

    public Retrieval retrieve(String query) {
        if (query == null || query.isBlank()) return Retrieval.empty();
        try {
            var filter = new FilterExpressionBuilder().eq("kind", "knowledge").build();
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(query)
                    .topK(properties.getTopK()).similarityThreshold(properties.getSimilarityThreshold())
                    .filterExpression(filter).build());
            String context = documents.stream().map(Document::getText).reduce((a, b) -> a + "\n\n" + b).orElse("");
            List<Source> sources = documents.stream().map(document -> new Source(document.getId(),
                    String.valueOf(document.getMetadata().getOrDefault("filename", "unknown")))).toList();
            return new Retrieval(context, sources);
        }
        catch (RuntimeException exception) {
            log.warn("Knowledge retrieval skipped: {}", exception.getMessage());
            return Retrieval.empty();
        }
    }

    public record Retrieval(String context, List<Source> sources) {
        static Retrieval empty() { return new Retrieval("", List.of()); }
    }
    public record Source(String id, String filename) { }
}
