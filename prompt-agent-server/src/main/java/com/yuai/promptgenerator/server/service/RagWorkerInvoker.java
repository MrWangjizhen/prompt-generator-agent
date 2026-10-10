package com.yuai.promptgenerator.server.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.yuai.promptgenerator.server.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RagWorkerInvoker {
    private static final Logger log = LoggerFactory.getLogger(RagWorkerInvoker.class);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final AppProperties.Rag properties;

    public RagWorkerInvoker(AppProperties properties) { this.properties = properties.getRag(); }

    public boolean invoke(String jobId) {
        try {
            String url = properties.getWorkerUrl().replaceAll("/$", "") + "/internal/rag/jobs/" + jobId;
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("X-Worker-Token", properties.getWorkerToken());
            if (properties.isAsyncInvocation()) builder.header("X-Fc-Invocation-Type", "Async");
            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.noBody()).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return properties.isAsyncInvocation() ? status == 202 : status >= 200 && status < 300;
        }
        catch (Exception exception) {
            log.warn("RAG worker invocation deferred for job {}: {}", jobId, exception.getMessage());
            return false;
        }
    }
}
