package com.yuai.promptgenerator.server.config;

import com.yuai.promptgenerator.core.service.PromptLintService;
import com.yuai.promptgenerator.server.tools.CalculatorTool;
import com.yuai.promptgenerator.server.tools.DateTimeTool;
import com.yuai.promptgenerator.server.tools.PromptInspectionTool;
import com.yuai.promptgenerator.server.tools.SafeWebReaderTool;
import com.yuai.promptgenerator.server.tools.WebSearchTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {
    @Bean
    PromptLintService promptLintService() { return new PromptLintService(); }

    @Bean("safeToolCallbacks")
    ToolCallback[] safeToolCallbacks(PromptLintService lintService, AppProperties properties) {
        return ToolCallbacks.from(new DateTimeTool(), new CalculatorTool(), new PromptInspectionTool(lintService),
                new WebSearchTool(properties), new SafeWebReaderTool());
    }
}
