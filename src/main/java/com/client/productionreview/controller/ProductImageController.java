package com.client.productionreview.controller;

import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.service.ProductImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RequestMapping(value = "/api/v1/production/file")
@RestController
public class ProductImageController {


    private final ProductImageService storageService;

    public ProductImageController(ProductImageService storageService) {
        this.storageService = storageService;
    }


    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'WRITE_PRIVILEGES')")
    public ProductImage uploadImagem(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idProduct") Long idProduct
    ) {
            return storageService.createProductImage(file, idProduct);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'DELETE_PRIVILEGES')")
    public void deleteFile(@PathVariable("id") Long idProductImage) {
        storageService.deleteFile(idProductImage);
    }
}
