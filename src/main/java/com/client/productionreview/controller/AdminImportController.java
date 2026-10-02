package com.client.productionreview.controller;

import com.client.productionreview.dtos.importer.ImportJobDTO;
import com.client.productionreview.dtos.importer.ImportRequestDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.CatalogImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/import")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminImportController {

    private final CatalogImportService catalogImportService;

    public AdminImportController(CatalogImportService catalogImportService) {
        this.catalogImportService = catalogImportService;
    }

    @PostMapping("/open-food-facts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportJobDTO importOpenFoodFacts(@Valid @RequestBody(required = false) ImportRequestDTO request,
                                            @AuthenticationPrincipal User user) {
        int perSubcategory = request == null ? ImportRequestDTO.DEFAULT_PRODUCTS_PER_SUBCATEGORY
                : request.productsPerSubcategoryOrDefault();
        return catalogImportService.startOpenFoodFactsImport(perSubcategory, user);
    }

    @GetMapping("/jobs/latest")
    public ResponseEntity<ImportJobDTO> latestJob() {
        return catalogImportService.getLatestJob()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/jobs/{id}")
    public ImportJobDTO getJob(@PathVariable("id") String id) {
        return catalogImportService.getJob(id);
    }
}
