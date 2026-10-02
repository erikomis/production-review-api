package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ProductMapper;
import com.client.productionreview.dtos.product.FollowResponseDTO;
import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductSuggestionDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductRequestDTO;
import com.client.productionreview.dtos.product.ProductResponseDTO;
import com.client.productionreview.dtos.product.ProductSortProperty;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ProductFollowService;
import com.client.productionreview.service.ProductService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/v1/production")
public class ProductController {


    private final ProductService productService;

    private final ProductMapper productMapper;

    private final ProductFollowService productFollowService;


    public ProductController(ProductService productService, ProductMapper productMapper,
                             ProductFollowService productFollowService) {
        this.productService = productService;
        this.productMapper = productMapper;
        this.productFollowService = productFollowService;
    }

    @PostMapping(
            value = "/add"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'WRITE_PRIVILEGES')")
    public ProductResponseDTO addProduct(
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        Product model = productMapper.toModel(productRequestDTO);

        var productMapp = productService.addProduct(model);

        return productMapper.toDTO(productMapp);
    }

    @GetMapping( "/list")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductSummaryDTO> listProductions(@RequestParam(value = "page", required = false) Integer page,
                                                   @RequestParam(value = "size", required = false) Integer size,
                                                   @RequestParam(value = "sort", required = false) Sort.Direction sort,
                                                   @RequestParam(value = "property", required = false) String property,
                                                   @RequestParam(value = "search", required = false) String search,
                                                   @RequestParam(value = "categoryId", required = false) Long categoryId,
                                                   @RequestParam(value = "subCategorieId", required = false) Long subCategorieId,
                                                   @RequestParam(value = "onlyRated", required = false, defaultValue = "false") boolean onlyRated) {
        String sortProperty = blankToNull(property);
        if (sortProperty != null) {
            // name | createdAt | averageNote | totalReviews; outro valor -> 400
            ProductSortProperty.from(sortProperty);
        }
        Pageable pageable = PaginationUtils.createPageable(page, size, sortProperty, sort != null ? sort.name() : null);
        return productService.listProducts(new ProductFilter(blankToNull(search), categoryId, subCategorieId, onlyRated), pageable);

    }


    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductDetailDTO getProduction(@PathVariable("id") Long id) {
        return productFollowService.withFollowInfo(productService.getProductDetail(id));
    }

    /** Autocompletar da busca: sem acento/maiúsculas, nomes que começam com o termo primeiro. */
    @GetMapping("/suggest")
    public List<ProductSuggestionDTO> suggest(@RequestParam(value = "q", required = false) String q,
                                              @RequestParam(value = "limit", required = false, defaultValue = "8") int limit) {
        return productService.suggest(q, limit);
    }

    @PostMapping("/{id}/follow")
    @SecurityRequirement(name = "jwt_auth")
    public FollowResponseDTO follow(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        return productFollowService.follow(id, user);
    }

    @DeleteMapping("/{id}/follow")
    @SecurityRequirement(name = "jwt_auth")
    public FollowResponseDTO unfollow(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        return productFollowService.unfollow(id, user);
    }


    @GetMapping("/slug/{slug}")
    @ResponseStatus(HttpStatus.OK)
    public ProductDetailDTO getProductionBySlug(@PathVariable("slug") String slug) {
        return productFollowService.withFollowInfo(productService.getProductDetailBySlug(slug));
    }


    @PutMapping( value = "/update/{id}")
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'UPDATE_PRIVILEGES')")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDTO updateProduction(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequestDTO productRequestDTO
    ) {

        Product model = productMapper.toModel(productRequestDTO);
        return productMapper.toDTO(productService.updateProduct( model, id));
    }

    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'DELETE_PRIVILEGES')")
    @DeleteMapping( "/delete/{id}")
    public ResponseEntity<?> deleteProduction(@PathVariable() Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

    }



    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
