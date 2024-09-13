package com.client.productionreview.service.impl;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.service.StorageService;
import io.minio.*;
import io.minio.errors.MinioException;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class StorageServiceImpl implements StorageService {

    private final MinioClient minioClient;

    public StorageServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                                    inputStream, inputStream.available(), -1)
                            .contentType(contentType)
                            .build());

        } catch (MinioException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }


    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new NotFoundException("Error occurred: " + e.getMessage());
        }


    }


}