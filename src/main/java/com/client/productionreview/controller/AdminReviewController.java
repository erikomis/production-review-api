package com.client.productionreview.controller;

import com.client.productionreview.dtos.review.BulkModerationResponseDTO;
import com.client.productionreview.dtos.review.ReviewBulkModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewModerationRequestDTO;
import com.client.productionreview.dtos.review.ReviewReplyRequestDTO;
import com.client.productionreview.dtos.review.ReviewReportDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.AdminExportService;
import com.client.productionreview.service.ReviewModerationService;
import com.client.productionreview.service.ReviewReportService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminReviewController {

    private final ReviewModerationService reviewModerationService;

    private final ReviewReportService reviewReportService;

    private final AdminExportService adminExportService;

    public AdminReviewController(ReviewModerationService reviewModerationService, ReviewReportService reviewReportService,
                                 AdminExportService adminExportService) {
        this.reviewModerationService = reviewModerationService;
        this.reviewReportService = reviewReportService;
        this.adminExportService = adminExportService;
    }

    @GetMapping
    public Page<ReviewResponseDTO> listReviews(@RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size,
                                               @RequestParam(value = "status", required = false) ReviewStatus status,
                                               @RequestParam(value = "note", required = false) Long note,
                                               @RequestParam(value = "productId", required = false) Long productId,
                                               @RequestParam(value = "search", required = false) String search,
                                               @RequestParam(value = "reported", required = false) Boolean reported) {
        return reviewModerationService.listReviews(status, note, productId, search, reported,
                PaginationUtils.createPageable(page, size, null, null));
    }

    /** Mesmos filtros da listagem; até 10.000 linhas. */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(value = "status", required = false) ReviewStatus status,
                                            @RequestParam(value = "note", required = false) Long note,
                                            @RequestParam(value = "productId", required = false) Long productId,
                                            @RequestParam(value = "search", required = false) String search,
                                            @RequestParam(value = "reported", required = false) Boolean reported) {
        return CsvResponses.attachment("avaliacoes", adminExportService.reviewsCsv(status, note, productId, search, reported));
    }

    @PatchMapping("/moderation")
    public BulkModerationResponseDTO moderateBulk(@Valid @RequestBody ReviewBulkModerationRequestDTO request,
                                                  @AuthenticationPrincipal User admin) {
        return new BulkModerationResponseDTO(reviewModerationService.moderateBulk(request, admin));
    }

    @GetMapping("/{id}/reports")
    public List<ReviewReportDTO> listReports(@PathVariable("id") Long id) {
        return reviewReportService.listReports(id);
    }

    @DeleteMapping("/{id}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissReports(@PathVariable("id") Long id, @AuthenticationPrincipal User admin) {
        reviewReportService.dismissReports(id, admin);
    }

    @PutMapping("/{id}/reply")
    public ReviewResponseDTO reply(@PathVariable("id") Long id, @Valid @RequestBody ReviewReplyRequestDTO request,
                                   @AuthenticationPrincipal User admin) {
        return reviewModerationService.reply(id, request.getText(), admin);
    }

    @DeleteMapping("/{id}/reply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@PathVariable("id") Long id, @AuthenticationPrincipal User admin) {
        reviewModerationService.deleteReply(id, admin);
    }

    @PatchMapping("/{id}/moderation")
    public ReviewResponseDTO moderate(@PathVariable("id") Long id, @Valid @RequestBody ReviewModerationRequestDTO request,
                                      @AuthenticationPrincipal User admin) {
        return reviewModerationService.moderate(id, request, admin);
    }
}
