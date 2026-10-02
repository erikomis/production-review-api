package com.client.productionreview.controller;

import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.service.ReviewImageService;
import com.client.productionreview.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class FileControllerTest {

    private static final String KEY = "reviews/12/0b8f6c1e-1d2b-4b7a-9c3e-2f1a0d9e8c7b.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewImageService reviewImageService;

    @Test
    void getFile_streamsWithTypeAndCache() throws Exception {
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        when(reviewImageService.openFile(KEY)).thenReturn(
                new StorageService.StoredObject(new ByteArrayInputStream(content), content.length, "application/octet-stream"));
        when(reviewImageService.contentTypeOf(KEY)).thenReturn("image/jpeg");

        mockMvc.perform(get("/api/v1/files/" + KEY))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "max-age=86400, public"))
                .andExpect(header().longValue("Content-Length", content.length))
                .andExpect(content().bytes(content));
    }

    @Test
    void getFile_missing_returns404() throws Exception {
        when(reviewImageService.openFile(KEY)).thenThrow(new NotFoundException("Arquivo não encontrado"));

        mockMvc.perform(get("/api/v1/files/" + KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Arquivo não encontrado"));
    }

    @Test
    void getFile_invalidKey_returns400() throws Exception {
        when(reviewImageService.openFile(anyString())).thenThrow(new BadRequestException("Arquivo inválido"));

        mockMvc.perform(get("/api/v1/files/outro/arquivo.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Arquivo inválido"));
        verify(reviewImageService).openFile("outro/arquivo.txt");
    }
}
