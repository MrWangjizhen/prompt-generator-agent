package com.yuai.promptgenerator.mcp;

import com.yuai.promptgenerator.mcp.catalog.PromptFrameworkCatalog;
import com.yuai.promptgenerator.mcp.tools.PromptEngineeringTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PromptEngineeringMcpApplication {
    public static void main(String[] args) { SpringApplication.run(PromptEngineeringMcpApplication.class, args); }

    @Bean
    PromptFrameworkCatalog promptFrameworkCatalog() { return new PromptFrameworkCatalog(); }

    @Bean
    ToolCallbackProvider promptToolProvider(PromptFrameworkCatalog catalog) {
        return ToolCallbackProvider.from(ToolCallbacks.from(new PromptEngineeringTools(catalog)));
    }
}
