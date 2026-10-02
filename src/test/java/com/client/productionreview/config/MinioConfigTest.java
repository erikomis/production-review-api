package com.client.productionreview.config;

import com.sun.net.httpserver.HttpServer;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioConfigTest {

    private HttpServer server;
    private final AtomicReference<String> requestedPath = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void endpoint_respectsHttpSchemeAndPort() throws Exception {
        // antes: endpoint(url, 443, true) forçava https na porta 443
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        MinioClient client = MinioConfig.build(url, "", "");

        assertTrue(client.bucketExists(BucketExistsArgs.builder().bucket("production-review").build()));
        assertEquals("/production-review", requestedPath.get());
    }

    @Test
    void endpoint_withCredentials_signsRequests() throws Exception {
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        MinioClient client = MinioConfig.build(url, "dev", "dev-secret");

        assertTrue(client.bucketExists(BucketExistsArgs.builder().bucket("bucket").build()));
    }
}
