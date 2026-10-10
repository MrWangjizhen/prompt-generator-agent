package com.yuai.promptgenerator.server.config;

import java.net.http.HttpClient;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "true")
public class McpTransportConfig {

    @Bean
    List<NamedClientMcpTransport> authenticatedPromptEngineeringTransport(
            @Value("${MCP_SERVER_URL:http://localhost:8131}") String baseUrl,
            @Value("${MCP_SERVER_TOKEN:}") String token,
            ObjectProvider<ObjectMapper> objectMappers) {
        var builder = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint("/sse")
                .clientBuilder(HttpClient.newBuilder())
                .objectMapper(objectMappers.getIfAvailable(ObjectMapper::new));
        if (token != null && !token.isBlank()) {
            builder.customizeRequest(request -> request.header("X-MCP-Token", token));
        }
        return List.of(new NamedClientMcpTransport("prompt-engineering", builder.build()));
    }
}
