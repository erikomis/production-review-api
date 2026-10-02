package com.client.productionreview.controller;

import com.client.productionreview.integration.AuditLogIntegration;
import com.client.productionreview.service.AdminExportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Proxy para o serviço de logs: o painel nunca fala direto com ele. */
@RestController
@RequestMapping("/api/v1/admin/activity")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminActivityController {

    private final AuditLogIntegration auditLogIntegration;

    private final AdminExportService adminExportService;

    public AdminActivityController(AuditLogIntegration auditLogIntegration, AdminExportService adminExportService) {
        this.auditLogIntegration = auditLogIntegration;
        this.adminExportService = adminExportService;
    }

    /** Mesmos filtros da atividade (type, entityType, userId, search, from, to); até 10.000 linhas. */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam MultiValueMap<String, String> params) {
        return CsvResponses.attachment("atividade", adminExportService.activityCsv(params));
    }

    @GetMapping
    public ResponseEntity<String> listActivity(@RequestParam MultiValueMap<String, String> params) {
        return auditLogIntegration.getLogs(params);
    }

    @GetMapping("/summary")
    public ResponseEntity<String> summary(@RequestParam MultiValueMap<String, String> params) {
        return auditLogIntegration.getSummary(params);
    }
}
