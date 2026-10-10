package com.yuai.promptgenerator.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.yuai.promptgenerator.server.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RagWorkerInvokerTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void acceptsOnlyFunctionComputeAsyncAcceptedResponse() throws IOException {
        AppProperties properties = propertiesWithServer(202, true);
        assertThat(new RagWorkerInvoker(properties).invoke("job-1")).isTrue();

        server.stop(0);
        properties = propertiesWithServer(200, true);
        assertThat(new RagWorkerInvoker(properties).invoke("job-1")).isFalse();
    }

    @Test
    void acceptsSynchronousSuccessDuringLocalDevelopment() throws IOException {
        AppProperties properties = propertiesWithServer(200, false);
        assertThat(new RagWorkerInvoker(properties).invoke("job-1")).isTrue();
    }

    private AppProperties propertiesWithServer(int status, boolean async) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/rag/jobs/job-1", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("X-Fc-Invocation-Type"))
                    .isEqualTo(async ? "Async" : null);
            assertThat(exchange.getRequestHeaders().getFirst("X-Worker-Token")).isEqualTo("worker-secret");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        AppProperties properties = new AppProperties();
        properties.getRag().setWorkerUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getRag().setWorkerToken("worker-secret");
        properties.getRag().setAsyncInvocation(async);
        return properties;
    }
}
