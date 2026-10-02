package com.client.productionreview.service;

import io.minio.ObjectWriteResponse;

import java.io.InputStream;

public interface StorageService {

    ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    void deleteFile(String bucketName, String objectName);

    /**
     * Abre o objeto para leitura em streaming (quem chama fecha o stream).
     * Lança {@link com.client.productionreview.exception.NotFoundException} se não existir.
     */
    StoredObject getFile(String bucketName, String objectName);

    record StoredObject(InputStream content, long size, String contentType) {
    }
}
