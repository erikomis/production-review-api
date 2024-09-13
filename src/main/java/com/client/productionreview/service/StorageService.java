package com.client.productionreview.service;

import io.minio.ObjectWriteResponse;

import java.io.InputStream;

public interface StorageService {

    ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType);

    void deleteFile(String bucketName, String objectName);
}
