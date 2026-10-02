package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.CategoryMapper;
import com.client.productionreview.dtos.category.CategoryRequestDTO;
import com.client.productionreview.dtos.category.CategoryResponseDTO;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequestMapping(value = "/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categorieService;

    private final CategoryMapper categorieMapper;


    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'WRITE_PRIVILEGES')")
    public CategoryResponseDTO addCatogory( @Valid @RequestBody CategoryRequestDTO categorie){
        Category model = categorieMapper.toModel(categorie);
        var categorieModel = categorieService.addCategory(model);
        return  categorieMapper.toDTO(categorieModel);
    }


    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'UPDATE_PRIVILEGES')")
    public CategoryResponseDTO updateCatogory(@PathVariable("id") Long id, @RequestBody CategoryRequestDTO categorioDto) {
        Category model = categorieMapper.toModel(categorioDto);
        var categorieModel = categorieService.updateCategory(model, id);
        return  categorieMapper.toDTO(categorieModel);
    }

    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'DELETE_PRIVILEGES')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCatogory(@PathVariable("id") Long id) {
        categorieService.deleteCategory(id);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponseDTO getCatogory(@PathVariable("id") Long id) {
       Category categorieModel = categorieService.getCategory(id);
        return categorieMapper.toDTO(categorieModel);

    }

    @GetMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public List<Category> getAllCatogories() {
        return  categorieService.getAllCategories();

    }

}
