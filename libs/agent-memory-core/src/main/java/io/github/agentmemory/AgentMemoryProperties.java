package io.github.agentmemory;

/**
 * Agent 记忆 starter 的统一配置模型。
 */
public class AgentMemoryProperties {

    private boolean enabled = true;

    private final ShortTerm shortTerm = new ShortTerm();

    private final Profile profile = new Profile();

    private final LongTerm longTerm = new LongTerm();

    private final Model router = new Model();

    private final Model extractor = new Model();

    private final Privacy privacy = new Privacy();

    private final Observation observation = new Observation();

    private final Async async = new Async();

    private CommitMode commitMode = CommitMode.ASYNC;

    private final Schema schema = new Schema();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ShortTerm getShortTerm() {
        return shortTerm;
    }

    public Profile getProfile() {
        return profile;
    }

    public LongTerm getLongTerm() {
        return longTerm;
    }

    public Model getRouter() {
        return router;
    }

    public Model getExtractor() {
        return extractor;
    }

    public Privacy getPrivacy() {
        return privacy;
    }

    public Observation getObservation() {
        return observation;
    }

    public Async getAsync() {
        return async;
    }

    public CommitMode getCommitMode() {
        return commitMode;
    }

    public void setCommitMode(CommitMode commitMode) {
        this.commitMode = commitMode == null ? CommitMode.ASYNC : commitMode;
    }

    public Schema getSchema() {
        return schema;
    }

    public static class ShortTerm {

        private int maxMessages = 10;

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }
    }

    public static class Profile {

        private StoreType store = StoreType.IN_MEMORY;

        public StoreType getStore() {
            return store;
        }

        public void setStore(StoreType store) {
            this.store = store;
        }
    }

    public static class LongTerm {

        private StoreType store = StoreType.IN_MEMORY;

        private IndexStoreType indexStore = IndexStoreType.IN_MEMORY;

        private int topK = 5;

        private double similarityThreshold = 0.65;

        private String vectorStoreBeanName = "";

        public StoreType getStore() {
            return store;
        }

        public void setStore(StoreType store) {
            this.store = store;
        }

        public IndexStoreType getIndexStore() {
            return indexStore;
        }

        public void setIndexStore(IndexStoreType indexStore) {
            this.indexStore = indexStore;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public String getVectorStoreBeanName() {
            return vectorStoreBeanName;
        }

        public void setVectorStoreBeanName(String vectorStoreBeanName) {
            this.vectorStoreBeanName = vectorStoreBeanName == null ? "" : vectorStoreBeanName;
        }
    }

    public static class Model {

        private String modelBeanName = "";

        public String getModelBeanName() {
            return modelBeanName;
        }

        public void setModelBeanName(String modelBeanName) {
            this.modelBeanName = modelBeanName == null ? "" : modelBeanName;
        }
    }

    public static class Privacy {

        private boolean requireConsent = true;

        private boolean sensitiveFilterEnabled = true;

        public boolean isRequireConsent() {
            return requireConsent;
        }

        public void setRequireConsent(boolean requireConsent) {
            this.requireConsent = requireConsent;
        }

        public boolean isSensitiveFilterEnabled() {
            return sensitiveFilterEnabled;
        }

        public void setSensitiveFilterEnabled(boolean sensitiveFilterEnabled) {
            this.sensitiveFilterEnabled = sensitiveFilterEnabled;
        }
    }

    public static class Observation {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Async {

        private int corePoolSize = 2;

        private int maxPoolSize = 4;

        private int queueCapacity = 100;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Schema {

        private int version = 1;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    public enum StoreType {
        IN_MEMORY,
        MONGODB,
        QDRANT
    }

    public enum IndexStoreType {
        IN_MEMORY,
        MONGODB,
        NONE
    }

    public enum CommitMode {
        SYNC,
        ASYNC
    }
}

