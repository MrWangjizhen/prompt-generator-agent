package com.yuai.promptgenerator.worker.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.yuai.promptgenerator.worker.service.WorkerObjectStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerStorageConfig {
    @Bean(destroyMethod = "close")
    WorkerObjectStorage storage(WorkerProperties properties) {
        if (properties.getOss().getEndpoint().isBlank() || properties.getOss().getBucket().isBlank()) {
            return key -> { throw new IllegalStateException("OSS is not configured"); };
        }
        OSS client = new OSSClientBuilder().build(properties.getOss().getEndpoint(),
                new EnvironmentVariableCredentialsProvider());
        return new WorkerObjectStorage() {
            public java.io.InputStream get(String key) {
                return client.getObject(properties.getOss().getBucket(), key).getObjectContent();
            }
            public void close() { client.shutdown(); }
        };
    }
}
