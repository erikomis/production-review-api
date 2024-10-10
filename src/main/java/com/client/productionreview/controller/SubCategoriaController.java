package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.SubCategoryMapper;
import com.client.productionreview.dtos.subCategory.SubCategoryResponseDTO;
import com.client.productionreview.dtos.subCategory.SubCategoryRequestDTO;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.service.SubCategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping( "/api/v1/sub-categorie")
public class SubCategoriaController {


    private final SubCategoryService subCategoryService;

    private final SubCategoryMapper subCategoryMapper;

    SubCategoriaController(SubCategoryService subCategoryService, SubCategoryMapper subCategoryMapper){
        this.subCategoryService = subCategoryService;
        this.subCategoryMapper = subCategoryMapper;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'WRITE_PRIVILEGES')")
    public SubCategoryResponseDTO add(@Valid  @RequestBody SubCategoryRequestDTO subCategoriaDto){
        SubCategory model = subCategoryMapper.toModel(subCategoriaDto);
        var subCategoriaModel = subCategoryService.addSubCategory(model);
        return subCategoryMapper.toDTO(subCategoriaModel);

    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'UPDATE_PRIVILEGES')")
    public SubCategoryResponseDTO update(@Valid  @RequestBody SubCategoryRequestDTO subCategoriaDto, @PathVariable Long id){
        SubCategory model = subCategoryMapper.toModel(subCategoriaDto);
        var subCategoriaModel = subCategoryService.updateSubCategory(model, id);
        return subCategoryMapper.toDTO(subCategoriaModel);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt_auth")
    @PreAuthorize("@permissionChecker.hasRoleWithPermission(authentication, 'ADMIN', 'DELETE_PRIVILEGES')")
    public void delete(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SubCategoryResponseDTO get(@PathVariable Long id) {
        SubCategory subCategoriaModel = subCategoryService.getSubCategory(id);
        return subCategoryMapper.toDTO(subCategoriaModel);

    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SubCategory> getAll() {
       return subCategoryService.getAllSubCategorie();

    }

}
