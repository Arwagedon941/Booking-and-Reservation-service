package com.example.platform.fileservice.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public FileStorageService(MinioClient minioClient,
                              @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    io.minio.MakeBucketArgs.builder().bucket(bucket).build()
            );
        }
    }

    public String upload(String objectName, MultipartFile file, Long resourceId) throws Exception {
        ensureBucket();
        String storedName = buildObjectName(objectName, resourceId);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(storedName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return storedName;
    }

    public byte[] download(String objectName) throws Exception {
        ensureBucket();
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build())) {
            return stream.readAllBytes();
        }
    }

    public byte[] download(String objectName, Long resourceId) throws Exception {
        String key = buildObjectName(objectName, resourceId);
        return download(key);
    }

    public List<String> list(Long resourceId) throws Exception {
        ensureBucket();
        String prefix = resourceId != null ? "resource-" + resourceId + "/" : null;
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build());
        return StreamSupport.stream(results.spliterator(), false)
                .map(r -> {
                    try {
                        return r.get().objectName();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public void delete(String objectName) throws Exception {
        ensureBucket();
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    public String buildObjectName(String original, Long resourceId) {
        if (resourceId == null) {
            return original;
        }
        return "resource-" + resourceId + "/" + original;
    }
}



