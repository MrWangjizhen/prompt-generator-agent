package com.yuai.promptgenerator.server.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.yuai.promptgenerator.server.config.AppProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WebSearchTool {
    private final AppProperties.Search properties;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public WebSearchTool(AppProperties properties) { this.properties = properties.getSearch(); }

    @Tool(description = "Search the public web using the configured search provider")
    public String webSearch(@ToolParam(description = "Search query") String query) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) return "Web search is not configured";
        try {
            URI uri = URI.create(properties.getBaseUrl() + (properties.getBaseUrl().contains("?") ? "&" : "?")
                    + "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET();
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                request.header("Authorization", "Bearer " + properties.getApiKey());
            }
            String body = client.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
            return body.substring(0, Math.min(body.length(), 50_000));
        }
        catch (Exception exception) {
            return "Search failed: " + exception.getClass().getSimpleName();
        }
    }
}
