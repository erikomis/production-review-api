package com.client.productionreview.controller;

import com.client.productionreview.dtos.admin.DeduplicationResultDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.CatalogDeduplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminCatalogController {

    private final CatalogDeduplicationService deduplicationService;

    public AdminCatalogController(CatalogDeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @PostMapping("/deduplicate")
    public DeduplicationResultDTO deduplicate(@AuthenticationPrincipal User admin) {
        return deduplicationService.deduplicate(admin);
    }
}
