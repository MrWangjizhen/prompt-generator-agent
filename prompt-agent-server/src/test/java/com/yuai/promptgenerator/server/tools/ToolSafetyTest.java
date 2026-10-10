package com.yuai.promptgenerator.server.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ToolSafetyTest {
    @Test
    void calculatorDoesNotEvaluateCode() {
        CalculatorTool tool = new CalculatorTool();
        assertThat(tool.calculate("(2 + 3) * 4")).isEqualTo("20");
        assertThat(tool.calculate("Runtime.getRuntime()" )).startsWith("Invalid expression");
    }

    @Test
    void webReaderBlocksLoopback() {
        assertThat(new SafeWebReaderTool().readWebPage("http://127.0.0.1:8080/secret"))
                .contains("Private or local network targets are blocked");
    }
}
