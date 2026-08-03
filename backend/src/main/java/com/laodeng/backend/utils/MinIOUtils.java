package com.laodeng.backend.utils;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.config.properties.MinIOProperties;
import com.laodeng.backend.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/8/3 23:30
 * @description MinIO 工具类，封装了常用的上传、删除、获取 URL 等操作
 */

@Slf4j
@RequiredArgsConstructor
public class MinIOUtils {
    private final MinIOProperties minIOProperties;
    /** MinIO 官方 SDK 客户端，线程安全，整个应用复用一个实例 */
    @Getter
    private MinioClient client;

    /** 对象名按日期分目录，避免单目录文件爆量 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 流式上传分片大小（5 MB），仅在文件大小未知时启用 */
    private static final long PART_SIZE = 5L * 1024 * 1024;

    // ============================================================
    //  初始化
    // ============================================================

    @PostConstruct
    public void init() {
        this.client = MinioClient.builder()
                .endpoint(minIOProperties.getEndpoint())
                .credentials(minIOProperties.getAccessKey(), minIOProperties.getSecretKey())
                .build();
        ensureBucket(minIOProperties.getBucketName());
        log.info("[MinIO] 初始化完成, defaultBucket={}", minIOProperties.getBucketName());
    }

    // ============================================================
    //  桶相关
    // ============================================================
    public void ensureBucket(String bucket) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("[MinIO] 已创建桶: {}", bucket);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"检查/创建桶失败: " + bucket);
        }
    }

    // ============================================================
    //  上传
    // ============================================================

    /**
     * 上传 {@link MultipartFile}（最常用入口）。
     * 自动按 {@code yyyy/MM/dd/UUID.ext} 规则生成对象名。
     *
     * @return 对象名（桶内相对路径），用于后续拼接 URL 或落库
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        String objectName = generateObjectName(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            String fileContentType = resolveContentType(file);
            log.info("当前传输的文件类型>>>>>>>>>{}",fileContentType);
            return upload(in, objectName, fileContentType, file.getSize());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR ,"读取上传文件流失败");
        }
    }

    /**
     * 流式上传（底层方法）
     */
    public String upload(InputStream in, String objectName, String contentType, long size) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(minIOProperties.getBucketName())
                    .object(objectName)
                    .stream(in, size, size > 0 ? -1 : PART_SIZE)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"上传失败: " + objectName);
        }
    }

    // ============================================================
    //  删除
    // ============================================================

    /** 删除单个对象 */
    public void delete(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(minIOProperties.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"删除失败: " + objectName);
        }
    }

    /**
     * 批量删除。
     * <p><b>实现要点：</b>{@code removeObjects} 返回的是<b>惰性</b> {@link Iterable}，
     * 不遍历就不会真正发请求，因此必须在循环中调用 {@code result.get()} 才会触发，
     * 并顺带把单项失败收集出来打日志。
     */
    public void deleteBatch(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }
        List<DeleteObject> targets = objectNames.stream().map(DeleteObject::new).toList();
        Iterable<Result<DeleteError>> results = client.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(minIOProperties.getBucketName())
                        .objects(targets)
                        .build());
        try {
            for (Result<DeleteError> r : results) {
                DeleteError err = r.get();
                log.warn("[MinIO] 批量删除项失败: object={}, message={}",
                        err.objectName(), err.message());
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"批量删除异常");
        }
    }

    // ============================================================
    //  访问 URL
    // ============================================================

    /**
     * 获取当前文件在minio的url
     */
    public String getFileUrl(String objectName) {
        return minIOProperties.getFileHost() + "/" + minIOProperties.getBucketName() + "/" + objectName;
    }

    /**
     * 生成临时预签名 GET URL。
     */
    public String getPresignedUrl(String objectName, int expireMinutes) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minIOProperties.getBucketName())
                    .object(objectName)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"临时预签名生成失败");
        }
    }

    // ============================================================
    //  获取的是当前存储的信息
    // ============================================================
    public List<String> listObjects(String prefix) {
        List<String> names = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder()
                    .bucket(minIOProperties.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> r : results) {
                names.add(r.get().objectName());
            }
            return names;
        } catch (Exception e) {
            throw new  BusinessException(ErrorCode.OPERATION_ERROR, "列出对象异常");
        }
    }

    // ============================================================
    //  生成不会重复的文件名
    // ============================================================

    /**
     * 通过原始文件名获取文件后缀之后根据当前时间和UUID结合防止文件名重复，返回文件名
     */
    public String generateObjectName(String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalFilename.substring(dot);
            }
        }
        return DATE_FMT.format(LocalDate.now())
                + "/" + UUID.randomUUID().toString().replace("-", "")
                + ext;
    }


    /**
     * 根据文件后缀
     * @param file
     * @return
     */
    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType(); //先去获取一下文件上传请求的时候文件携带的文件类型字段
        if (ct != null && !ct.equals("application/octet-stream")) {
            //如果当前类型字段不是默认字段的话就按照传输的请求头携带的文件类型
            return ct;
        }
        String name = file.getOriginalFilename(); //获取文件的原始文件名
        if (name == null) return "application/octet-stream"; //如果没有文件名就直接返回默认的文件类型
        int dot = name.lastIndexOf('.'); // 获取这个文件的后缀名
        if (dot < 0) return "application/octet-stream"; //如果后缀名是空的那么就直接返回默认文件类型
        return switch (name.substring(dot).toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".ico" -> "image/x-icon";
            case ".pdf" -> "application/pdf";
            case ".mp4" -> "video/mp4";
            case ".mp3" -> "audio/mpeg";
            case ".svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
