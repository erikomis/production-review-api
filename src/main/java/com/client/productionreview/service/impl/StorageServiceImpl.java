package com.client.productionreview.service.impl;

import com.client.productionreview.exception.IoFileException;
import com.client.productionreview.service.StorageService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private final MinioClient minioClient;

    public StorageServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());

        } catch (Exception e) {
            log.error("Erro ao enviar arquivo {} para o storage", objectName, e);
            throw new IoFileException("Erro ao enviar arquivo para o storage");
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            log.error("Erro ao remover arquivo {} do storage", objectName, e);
            throw new IoFileException("Erro ao remover arquivo do storage");
        }
    }

}
