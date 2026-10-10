package com.yuai.promptgenerator.worker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuai.promptgenerator.core.domain.KnowledgeDocument;
import org.junit.jupiter.api.Test;

class KnowledgeChunkerTest {
    @Test
    void createsOverlappingDeterministicChunksWithSourceMetadata() {
        KnowledgeDocument source = new KnowledgeDocument();
        source.setId("document-1");
        source.setFilename("guide.md");
        source.setChecksum("sha256");
        source.setVersion(2);
        String text = "A".repeat(3000) + "\n" + "B".repeat(1200);

        var first = new KnowledgeChunker().split(source, text);
        var second = new KnowledgeChunker().split(source, text);

        assertThat(first).hasSize(2);
        assertThat(first).extracting(document -> document.getId())
                .containsExactlyElementsOf(second.stream().map(document -> document.getId()).toList());
        assertThat(first.get(1).getText()).startsWith("A".repeat(399));
        assertThat(first.get(0).getMetadata()).containsEntry("documentId", "document-1")
                .containsEntry("version", 2).containsEntry("chunkIndex", 0);
    }
}
