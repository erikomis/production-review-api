package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.integration.AuditLogIntegration;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.service.AdminExportService;
import com.client.productionreview.service.AdminUserService;
import com.client.productionreview.service.ReviewModerationService;
import com.client.productionreview.utils.CsvWriter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminExportServiceImpl implements AdminExportService {

    static final int PAGE_SIZE = 500;

    /** O serviço de logs limita o tamanho da página. */
    static final int ACTIVITY_PAGE_SIZE = 100;

    private final ReviewModerationService reviewModerationService;
    private final AdminUserService adminUserService;
    private final AuditLogIntegration auditLogIntegration;

    public AdminExportServiceImpl(ReviewModerationService reviewModerationService, AdminUserService adminUserService,
                                  AuditLogIntegration auditLogIntegration) {
        this.reviewModerationService = reviewModerationService;
        this.adminUserService = adminUserService;
        this.auditLogIntegration = auditLogIntegration;
    }

    @Override
    public byte[] reviewsCsv(ReviewStatus status, Long note, Long productId, String search, Boolean reported) {
        List<List<?>> rows = new ArrayList<>();
        for (int page = 0; rows.size() < MAX_ROWS; page++) {
            Page<ReviewResponseDTO> result = reviewModerationService.listReviews(status, note, productId, search, reported,
                    PageRequest.of(page, PAGE_SIZE));
            for (ReviewResponseDTO review : result.getContent()) {
                if (rows.size() >= MAX_ROWS) {
                    break;
                }
                rows.add(Arrays.asList(review.getId(), review.getProductName(), review.getUserName(), review.getUserUsername(),
                        review.getNote(), review.getTitle(), review.getDescription(), review.getStatus(),
                        review.getModerationReason(), review.getModeratedByName(), review.getModeratedAt(),
                        review.getHelpfulCount(), review.getReportsCount(),
                        review.getReply() == null ? null : review.getReply().getText(), review.getCreatedAt()));
            }
            if (!result.hasNext()) {
                break;
            }
        }
        return CsvWriter.write(List.of("ID", "Produto", "Autor", "Usuário", "Nota", "Título", "Descrição", "Status",
                "Motivo da moderação", "Moderada por", "Moderada em", "Útil", "Denúncias", "Resposta oficial",
                "Criada em"), rows);
    }

    @Override
    public byte[] usersCsv(String search, String role, Boolean active) {
        List<List<?>> rows = new ArrayList<>();
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        for (int page = 0; rows.size() < MAX_ROWS; page++) {
            Page<AdminUserDTO> result = adminUserService.listUsers(search, role, active, PageRequest.of(page, PAGE_SIZE, sort));
            for (AdminUserDTO user : result.getContent()) {
                if (rows.size() >= MAX_ROWS) {
                    break;
                }
                rows.add(Arrays.asList(user.getId(), user.getName(), user.getUsername(), user.getEmail(),
                        Boolean.TRUE.equals(user.getActive()) ? "Sim" : "Não",
                        user.getRoles() == null ? null : String.join(", ", user.getRoles()),
                        user.getReviewsCount(), user.getCreatedAt()));
            }
            if (!result.hasNext()) {
                break;
            }
        }
        return CsvWriter.write(List.of("ID", "Nome", "Usuário", "E-mail", "Ativo", "Perfis", "Avaliações", "Cadastrado em"),
                rows);
    }

    @Override
    public byte[] activityCsv(MultiValueMap<String, String> params) {
        List<List<?>> rows = new ArrayList<>();
        for (int page = 0; rows.size() < MAX_ROWS; page++) {
            MultiValueMap<String, String> query = new LinkedMultiValueMap<>(params == null ? new LinkedMultiValueMap<>() : params);
            query.set("page", String.valueOf(page));
            query.set("size", String.valueOf(ACTIVITY_PAGE_SIZE));
            JsonNode body = auditLogIntegration.getLogsJson(query);
            JsonNode content = body.path("content");
            for (JsonNode log : content) {
                if (rows.size() >= MAX_ROWS) {
                    break;
                }
                rows.add(Arrays.asList(text(log, "occurredAt"), text(log, "type"), text(log, "action"), text(log, "message"),
                        text(log, "nameUser"), text(log, "userId"), text(log, "entityType"), text(log, "entityId")));
            }
            int totalPages = body.path("page").path("totalPages").asInt(0);
            if (content.isEmpty() || page + 1 >= totalPages) {
                break;
            }
        }
        return CsvWriter.write(List.of("Data", "Tipo", "Ação", "Mensagem", "Usuário", "ID do usuário", "Entidade",
                "ID da entidade"), rows);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
