package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.dtos.review.ReviewReplyDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.integration.AuditLogIntegration;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.service.impl.AdminExportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminExportServiceTest {

    @Mock
    private ReviewModerationService reviewModerationService;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private AuditLogIntegration auditLogIntegration;

    @InjectMocks
    private AdminExportServiceImpl service;

    private static String text(byte[] csv) {
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    void reviewsCsv_usesFiltersAndWritesRows() {
        ReviewResponseDTO review = ReviewResponseDTO.builder().id(12L).productName("Café").userName("Maria")
                .userUsername("maria").note(5L).title("Ótimo; recomendo").description("Bom").status(ReviewStatus.VISIBLE)
                .helpfulCount(2).reportsCount(1).reply(new ReviewReplyDTO("Obrigado", "Admin", Instant.now()))
                .createdAt(Instant.parse("2026-10-02T02:14:49Z")).build();
        when(reviewModerationService.listReviews(eq(ReviewStatus.VISIBLE), eq(5L), eq(3L), eq("cafe"), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 500), 1));

        String csv = text(service.reviewsCsv(ReviewStatus.VISIBLE, 5L, 3L, "cafe", true));

        String[] lines = csv.split("\r\n");
        assertEquals("ID;Produto;Autor;Usuário;Nota;Título;Descrição;Status;Motivo da moderação;Moderada por;"
                + "Moderada em;Útil;Denúncias;Resposta oficial;Criada em", lines[0]);
        assertEquals("12;Café;Maria;maria;5;\"Ótimo; recomendo\";Bom;VISIBLE;;;;2;1;Obrigado;2026-10-02T02:14:49Z", lines[1]);
    }

    @Test
    void reviewsCsv_stopsAt10000Rows() {
        List<ReviewResponseDTO> page = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            page.add(ReviewResponseDTO.builder().id((long) i).build());
        }
        when(reviewModerationService.listReviews(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new PageImpl<>(page, inv.getArgument(5, Pageable.class), 50_000));

        String csv = text(service.reviewsCsv(null, null, null, null, null));

        assertEquals(AdminExportService.MAX_ROWS + 1, csv.split("\r\n").length);
        verify(reviewModerationService, times(20)).listReviews(any(), any(), any(), any(), any(), any());
    }

    @Test
    void usersCsv_writesRows() {
        when(adminUserService.listUsers(eq("mar"), eq("USER"), eq(true), any())).thenReturn(new PageImpl<>(List.of(
                AdminUserDTO.builder().id(2L).name("Maria").username("maria").email("maria@mail.com").active(true)
                        .roles(List.of("USER")).reviewsCount(3).createdAt(Instant.parse("2026-10-01T10:00:00Z")).build())));

        String csv = text(service.usersCsv("mar", "USER", true));

        assertEquals("ID;Nome;Usuário;E-mail;Ativo;Perfis;Avaliações;Cadastrado em\r\n"
                + "2;Maria;maria;maria@mail.com;Sim;USER;3;2026-10-01T10:00:00Z\r\n", csv);
    }

    @Test
    void activityCsv_pagesThroughTheLogsService() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        when(auditLogIntegration.getLogsJson(any())).thenReturn(
                mapper.readTree("{\"content\":[{\"occurredAt\":\"2026-10-02T03:10:00Z\",\"type\":\"REVIEW_CREATED\","
                        + "\"action\":\"Avaliação criada\",\"message\":\"=cmd\",\"nameUser\":\"Maria\",\"userId\":2,"
                        + "\"entityType\":\"REVIEW\",\"entityId\":\"12\"}],\"page\":{\"totalPages\":2}}"),
                mapper.readTree("{\"content\":[{\"type\":\"USER_LOGGED_IN\"}],\"page\":{\"totalPages\":2}}"));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("type", "REVIEW_CREATED");

        String csv = text(service.activityCsv(params));

        String[] lines = csv.split("\r\n");
        assertEquals(3, lines.length);
        assertEquals("2026-10-02T03:10:00Z;REVIEW_CREATED;Avaliação criada;'=cmd;Maria;2;REVIEW;12", lines[1]);
        ArgumentCaptor<MultiValueMap<String, String>> query = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(auditLogIntegration, times(2)).getLogsJson(query.capture());
        assertEquals("REVIEW_CREATED", query.getAllValues().get(0).getFirst("type"));
        assertEquals("100", query.getAllValues().get(0).getFirst("size"));
        assertEquals("1", query.getAllValues().get(1).getFirst("page"));
    }
}
