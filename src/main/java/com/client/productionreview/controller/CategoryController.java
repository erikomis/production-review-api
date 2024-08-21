package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.CategoryMapper;
import com.client.productionreview.dtos.category.CategoryRequestDTO;
import com.client.productionreview.dtos.category.CategoryResponseDTO;
import com.client.productionreview.model.Category;
import com.client.productionreview.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequestMapping(value = "/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categorieService;

    private final CategoryMapper categorieMapper;


    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDTO addCatogory(@RequestBody CategoryRequestDTO categorie){
        Category model = categorieMapper.toModel(categorie);
        var categorieModel = categorieService.addCatogory(model);
        return  categorieMapper.toDTO(categorieModel);
    }


    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponseDTO updateCatogory(@PathVariable("id") Long id, @RequestBody CategoryRequestDTO categorioDto) {
        Category model = categorieMapper.toModel(categorioDto);
        var categorieModel = categorieService.updateCatogory(model, id);
        return  categorieMapper.toDTO(categorieModel);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCatogory(@PathVariable("id") Long id) {
        categorieService.deleteCatogory(id);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponseDTO getCatogory(@PathVariable("id") Long id) {
       Category categorieModel = categorieService.getCatogory(id);
        return categorieMapper.toDTO(categorieModel);

    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Category> getAllCatogories() {
        return  categorieService.getAllCatogories();

    }

}
