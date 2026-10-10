package com.yuai.promptgenerator.worker;

import com.yuai.promptgenerator.worker.config.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.yuai.promptgenerator")
@EnableConfigurationProperties(WorkerProperties.class)
@EnableMongoRepositories(basePackages = "com.yuai.promptgenerator.core.repository")
public class RagWorkerApplication {
    public static void main(String[] args) { SpringApplication.run(RagWorkerApplication.class, args); }
}
