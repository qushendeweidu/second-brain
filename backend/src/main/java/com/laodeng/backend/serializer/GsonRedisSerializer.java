package com.laodeng.backend.serializer;

import cn.hutool.core.util.ObjUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/31 16:12
 * @description Gson序列化器
 */

@Log4j2
public class GsonRedisSerializer<T> implements RedisSerializer<T> {
    private final Class<T> clazz;
    private final Gson gson;

    public GsonRedisSerializer(Class<T> clazz) {
        this.clazz = clazz;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    @Override
    public byte[] serialize(@Nullable T value) throws SerializationException {
        if (value == null){
            return new byte[0];
        }
        return this.gson.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public @Nullable T deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (ObjUtil.isEmpty(bytes)){
            return null;
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        return this.gson.fromJson(json, this.clazz);
    }

    public boolean canSerialize() {
        return RedisSerializer.super.canSerialize(this.clazz);
    }

    @Override
    public Class<?> getTargetType() {
        return this.clazz;
    }
}
