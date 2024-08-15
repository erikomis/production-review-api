package com.client.productionreview.controller;

import com.client.productionreview.dtos.ProductDto;
import com.client.productionreview.model.Product;
import com.client.productionreview.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping(value = "/production")
public class ProductController {


    private final ProductService productService;


    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> addProduction(MultipartFile file, ProductDto product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product, file));
    }

    @GetMapping(value = "/list")
    public ResponseEntity<Page<Product>> listProductions(@RequestParam("page") Integer page, @RequestParam("size")
            Integer size, @RequestParam(value = "sort",
            required = false) Sort.Direction sort, @RequestParam(value = "property", required = false)
            String property, @RequestParam(value = "search", required = false) String search) {
        Pageable pageable = Objects.nonNull(sort) ? PageRequest.of(page, size, Sort.by(sort, property)) : PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProduct(pageable, search));

    }


    @PutMapping(value = "/update/{id}")
    public ResponseEntity<Product> updateProduction(@PathVariable Long id, @RequestBody ProductDto product, MultipartFile file) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(product, id, file));
    }

    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<?> deleteProduction(@PathVariable() Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

    }


}
