package com.yuai.promptgenerator.worker.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.yuai.promptgenerator.core.domain.KnowledgeDocument;
import org.springframework.ai.document.Document;

public class KnowledgeChunker {
    private static final int CHUNK_CHARS = 3200;
    private static final int OVERLAP_CHARS = 400;

    public List<Document> split(KnowledgeDocument source, String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        List<Document> result = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_CHARS);
            if (end < normalized.length()) {
                int boundary = normalized.lastIndexOf('\n', end);
                if (boundary > start + CHUNK_CHARS / 2) end = boundary;
            }
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("kind", "knowledge");
                metadata.put("documentId", source.getId());
                metadata.put("filename", source.getFilename());
                metadata.put("checksum", source.getChecksum());
                metadata.put("version", source.getVersion());
                metadata.put("chunkIndex", index);
                String seed = source.getId() + ":" + source.getVersion() + ":" + index;
                result.add(new Document(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString(), chunk, metadata));
                index++;
            }
            if (end == normalized.length()) break;
            start = Math.max(start + 1, end - OVERLAP_CHARS);
        }
        return result;
    }
}
