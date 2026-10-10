package io.github.agentmemory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrivacyFilterTest {

    @Test
    void shouldMaskCommonSensitiveValues() {
        BasicMemoryPrivacyFilter filter = new BasicMemoryPrivacyFilter(true);

        String result = filter.filter("邮箱 a@test.com 手机 13800138000");

        assertEquals("邮箱 [EMAIL] 手机 [PHONE]", result);
    }
}

