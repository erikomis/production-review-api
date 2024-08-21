package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.ProductMapper;
import com.client.productionreview.dtos.product.ProductRequestDTO;
import com.client.productionreview.dtos.product.ProductResponseDTO;
import com.client.productionreview.model.Product;
import com.client.productionreview.service.ProductService;
import com.client.productionreview.utils.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping(value = "/production")
public class ProductController {


    private final ProductService productService;

    private final ProductMapper productMapper;


    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PostMapping(
            value = "/add"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO addProduct(
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        Product model = productMapper.toModel(productRequestDTO);

        var productMapp = productService.addProduct(model);

        return productMapper.toDTO(productMapp);
    }

    @GetMapping( "/list")
    @ResponseStatus(HttpStatus.OK)
    public Page<Product> listProductions(@RequestParam("page") Integer page, @RequestParam("size")
            Integer size, @RequestParam(value = "sort",
            required = false) Sort.Direction sort, @RequestParam(value = "property", required = false)
            String property, @RequestParam(value = "search", required = false) String search) {
        Pageable pageable = PaginationUtils.createPageable(page, size, property, Objects.requireNonNull(sort).name());
        return productService.getAllProduct(pageable, search);

    }


    @PutMapping( value = "/update/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDTO updateProduction(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequestDTO productRequestDTO
    ) {

        Product model = productMapper.toModel(productRequestDTO);
        return productMapper.toDTO(productService.updateProduct( model, id));
    }

    @DeleteMapping( "/delete/{id}")
    public ResponseEntity<?> deleteProduction(@PathVariable() Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

    }


}
