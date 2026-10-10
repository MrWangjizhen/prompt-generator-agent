package com.yuai.promptgenerator.server;

import com.yuai.promptgenerator.server.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.yuai.promptgenerator", "io.github.agentmemory"})
@EnableConfigurationProperties(AppProperties.class)
@EnableMongoRepositories(basePackages = "com.yuai.promptgenerator.core.repository")
public class PromptAgentServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromptAgentServerApplication.class, args);
    }
}
