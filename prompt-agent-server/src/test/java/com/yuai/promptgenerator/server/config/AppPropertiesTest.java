package com.yuai.promptgenerator.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsModelToolCapabilitySwitch() {
        runner.withPropertyValues("app.model.tool-callbacks-enabled=true").run(context ->
                assertThat(context.getBean(AppProperties.class).getModel().isToolCallbacksEnabled()).isTrue());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class PropertiesConfiguration {
    }
}
