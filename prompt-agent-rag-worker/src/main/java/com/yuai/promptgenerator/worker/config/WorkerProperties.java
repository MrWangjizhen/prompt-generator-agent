package com.yuai.promptgenerator.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class WorkerProperties {
    private final Qdrant qdrant = new Qdrant();
    private final Oss oss = new Oss();
    private String workerToken = "dev-worker-token";
    public Qdrant getQdrant() { return qdrant; }
    public Oss getOss() { return oss; }
    public String getWorkerToken() { return workerToken; }
    public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }

    public static class Qdrant {
        private String host = "localhost";
        private int port = 6334;
        private boolean tls;
        private String apiKey = "";
        private String knowledgeCollection = "prompt_knowledge_local_v1";
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isTls() { return tls; }
        public void setTls(boolean tls) { this.tls = tls; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
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
}
