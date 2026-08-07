package com.laodeng.backend.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/7 23:30
 * @description Jackson 全局配置：统一 LocalDateTime 序列化格式，并兼容多种反序列化输入格式
 */
@Log4j2
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    @Bean
    public JsonMapperBuilderCustomizer localDateTimeCustomizer() {
        return (JsonMapper.Builder builder) -> builder.addModule(new SimpleModule()
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(OUTPUT_FORMATTER))
                .addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer()));
    }

    /**
     * 宽松的 LocalDateTime 反序列化器：依次尝试多种日期格式，
     * 兼容前端传入的 "yyyy-MM-dd HH:mm:ss"（含不补零的月/日）以及 ISO 格式。
     */
    static class LenientLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        @Override
        protected LocalDateTime _fromString(JsonParser p, DeserializationContext ctxt, String string0)
                throws JacksonException {
            String string = string0.trim();
            if (string.isEmpty()) {
                return _fromEmptyString(p, ctxt, string);
            }
            for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
                try {
                    return LocalDateTime.parse(string, formatter);
                } catch (DateTimeParseException ignored) {
                    // 继续尝试下一种格式
                }
            }
            DateTimeParseException e = new DateTimeParseException(
                    "Text '" + string + "' could not be parsed with any of the configured formats", string, 0);
            return _handleDateTimeFormatException(ctxt, e, OUTPUT_FORMATTER, string);
        }
    }
}
