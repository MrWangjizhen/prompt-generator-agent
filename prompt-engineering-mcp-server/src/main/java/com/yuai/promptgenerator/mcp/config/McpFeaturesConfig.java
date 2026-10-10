package com.yuai.promptgenerator.mcp.config;

import java.util.List;

import com.yuai.promptgenerator.mcp.catalog.PromptFrameworkCatalog;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpFeaturesConfig {
    @Bean
    List<McpServerFeatures.SyncResourceSpecification> promptResources(PromptFrameworkCatalog catalog) {
        McpSchema.Resource resource = new McpSchema.Resource("prompt://frameworks", "Prompt Framework Catalog",
                "Curated prompt-engineering framework reference", "text/markdown", null);
        return List.of(new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) ->
                new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(
                        "prompt://frameworks", "text/markdown", catalog.asMarkdown())))));
    }

    @Bean
    List<McpServerFeatures.SyncPromptSpecification> promptTemplates(PromptFrameworkCatalog catalog) {
        McpSchema.Prompt prompt = new McpSchema.Prompt("guided_prompt_generator",
                "Guide a user through requirement discovery and produce a final prompt",
                List.of(new McpSchema.PromptArgument("taskType", "Task type", true),
                        new McpSchema.PromptArgument("goal", "Initial goal", false)));
        return List.of(new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Object taskType = request.arguments() == null ? "TEXT" : request.arguments().getOrDefault("taskType", "TEXT");
            Object goal = request.arguments() == null ? null : request.arguments().get("goal");
            String text = catalog.generatorTemplate(taskType.toString(), goal == null ? null : goal.toString());
            return new McpSchema.GetPromptResult("Guided prompt generator",
                    List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text))));
        }));
    }
}
