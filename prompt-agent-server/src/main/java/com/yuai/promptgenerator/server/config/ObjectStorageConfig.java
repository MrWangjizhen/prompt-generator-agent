package com.yuai.promptgenerator.server.config;

import java.io.InputStream;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.ObjectMetadata;
import com.yuai.promptgenerator.server.service.ObjectStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectStorageConfig {
    @Bean(destroyMethod = "close")
    ObjectStorage objectStorage(AppProperties properties) {
        if (properties.getOss().getEndpoint().isBlank() || properties.getOss().getBucket().isBlank()) {
            return new ObjectStorage() {
                public void put(String key, InputStream data, long length, String contentType) { throw unavailable(); }
                public InputStream get(String key) { throw unavailable(); }
                public void delete(String key) { throw unavailable(); }
                public boolean available() { return false; }
                private IllegalStateException unavailable() { return new IllegalStateException("OSS is not configured"); }
            };
        }
        OSS client = new OSSClientBuilder().build(properties.getOss().getEndpoint(),
                new EnvironmentVariableCredentialsProvider());
        return new ObjectStorage() {
            public void put(String key, InputStream data, long length, String contentType) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(length);
                metadata.setContentType(contentType);
                client.putObject(properties.getOss().getBucket(), key, data, metadata);
            }
            public InputStream get(String key) { return client.getObject(properties.getOss().getBucket(), key).getObjectContent(); }
            public void delete(String key) { client.deleteObject(properties.getOss().getBucket(), key); }
            public boolean available() { return true; }
            public void close() { client.shutdown(); }
        };
    }
}
