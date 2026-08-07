package com.laodeng.backend.config;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/7 23:40
 * @description LocalDateTime 全局 Jackson 格式测试
 */
class JacksonConfigTest {

    private final JsonMapper mapper;

    JacksonConfigTest() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().localDateTimeCustomizer().customize(builder);
        this.mapper = builder.build();
    }

    @Test
    void shouldDeserializeZeroPaddedSpaceFormat() throws Exception {
        String json = "\"2026-08-08 00:00:00\"";
        assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0, 0), mapper.readValue(json, LocalDateTime.class));
    }

    @Test
    void shouldDeserializeNonZeroPaddedSpaceFormat() throws Exception {
        String json = "\"2026-8-8 0:0:0\"";
        assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0, 0), mapper.readValue(json, LocalDateTime.class));
    }

    @Test
    void shouldDeserializeIsoFormat() throws Exception {
        String json = "\"2026-08-08T00:00:00\"";
        assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0, 0), mapper.readValue(json, LocalDateTime.class));
    }

    @Test
    void shouldSerializeToSpaceFormat() throws Exception {
        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 8, 8, 0, 0, 0));
        assertEquals("\"2026-08-08 00:00:00\"", json);
    }
}
