package com.client.productionreview.controller;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.service.SubCategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/sub-categorie")
public class SubCategoriaController {


    private SubCategoriaService subCategoriaService;


    public SubCategoriaController(SubCategoriaService subCategoriaService) {
        this.subCategoriaService = subCategoriaService;
    }



    @PostMapping
    public ResponseEntity<SubCategorie> create(@RequestBody SubCategorieDto subCategoriaDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subCategoriaService.addSubCategorie(subCategoriaDto));
    }

}
