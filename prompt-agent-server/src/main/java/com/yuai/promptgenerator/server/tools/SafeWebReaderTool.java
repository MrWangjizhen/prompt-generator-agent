package com.yuai.promptgenerator.server.tools;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jsoup.Jsoup;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class SafeWebReaderTool {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    @Tool(description = "Read text from a public HTTP or HTTPS page with private-network and size protections")
    public String readWebPage(@ToolParam(description = "Public HTTP or HTTPS URL") String url) {
        try {
            URI uri = URI.create(url);
            validate(uri);
            HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "PromptGeneratorAgent/1.0").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return "HTTP status " + response.statusCode();
            if (response.body().length > 1_048_576) return "Page exceeds the 1 MB safety limit";
            String html = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
            String text = Jsoup.parse(html).text();
            return text.substring(0, Math.min(text.length(), 40_000));
        }
        catch (Exception exception) {
            return "Read failed: " + exception.getMessage();
        }
    }

    private void validate(URI uri) throws Exception {
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Only HTTP and HTTPS are allowed");
        }
        if (uri.getHost() == null) throw new IllegalArgumentException("URL host is required");
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Private or local network targets are blocked");
            }
        }
    }
}
