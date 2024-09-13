package com.client.productionreview.controller;

import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.service.ProductImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RequestMapping(value = "/api/v1/production/file")
@RestController
public class ProductImageController {


    private ProductImageService storageService;

    public ProductImageController(ProductImageService storageService) {
        this.storageService = storageService;
    }


    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt_auth")
    public ProductImage uploadImagem(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idProduct") Long idProduct
    ) {
            return storageService.createProductImage(file, idProduct);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt_auth")
    public void deleteFile(@PathVariable("id") Long idProductImage) {
        storageService.deleteFile(idProductImage);
    }
}
