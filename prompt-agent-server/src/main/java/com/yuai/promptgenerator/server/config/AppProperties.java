package com.yuai.promptgenerator.server.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppProperties {
    private final Jwt jwt = new Jwt();
    private final Qdrant qdrant = new Qdrant();
    private final Oss oss = new Oss();
    private final Rag rag = new Rag();
    private final Search search = new Search();
    private final Security security = new Security();
    private final Model model = new Model();

    public Jwt getJwt() { return jwt; }
    public Qdrant getQdrant() { return qdrant; }
    public Oss getOss() { return oss; }
    public Rag getRag() { return rag; }
    public Search getSearch() { return search; }
    public Security getSecurity() { return security; }
    public Model getModel() { return model; }

    public static class Jwt {
        private String secret;
        private String issuer = "prompt-generator-agent";
        private Duration accessTtl = Duration.ofMinutes(15);
        private Duration refreshTtl = Duration.ofDays(7);
        private boolean cookieSecure;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Duration getAccessTtl() { return accessTtl; }
        public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }
        public Duration getRefreshTtl() { return refreshTtl; }
        public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
        public boolean isCookieSecure() { return cookieSecure; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    }

    public static class Qdrant {
        private String host = "localhost";
        private int port = 6334;
        private boolean tls;
        private String apiKey = "";
        private String memoryCollection = "agent_memory_local_v1";
        private String knowledgeCollection = "prompt_knowledge_local_v1";
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isTls() { return tls; }
        public void setTls(boolean tls) { this.tls = tls; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getMemoryCollection() { return memoryCollection; }
        public void setMemoryCollection(String memoryCollection) { this.memoryCollection = memoryCollection; }
        public String getKnowledgeCollection() { return knowledgeCollection; }
        public void setKnowledgeCollection(String knowledgeCollection) { this.knowledgeCollection = knowledgeCollection; }
    }

    public static class Oss {
        private String endpoint = "";
        private String bucket = "";
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
    }

    public static class Rag {
        private String workerUrl = "http://localhost:8132";
        private String workerToken = "dev-worker-token";
        private int topK = 5;
        private double similarityThreshold = 0.65;
        private long maxUploadBytes = 20L * 1024 * 1024;
        private boolean asyncInvocation;
        public String getWorkerUrl() { return workerUrl; }
        public void setWorkerUrl(String workerUrl) { this.workerUrl = workerUrl; }
        public String getWorkerToken() { return workerToken; }
        public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        public long getMaxUploadBytes() { return maxUploadBytes; }
        public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
        public boolean isAsyncInvocation() { return asyncInvocation; }
        public void setAsyncInvocation(boolean asyncInvocation) { this.asyncInvocation = asyncInvocation; }
    }

    public static class Search {
        private String baseUrl = "";
        private String apiKey = "";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Security {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Model {
        private boolean toolCallbacksEnabled;
        public boolean isToolCallbacksEnabled() { return toolCallbacksEnabled; }
        public void setToolCallbacksEnabled(boolean toolCallbacksEnabled) {
            this.toolCallbacksEnabled = toolCallbacksEnabled;
        }
    }
}
