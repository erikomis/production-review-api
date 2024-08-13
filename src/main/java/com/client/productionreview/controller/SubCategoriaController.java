package com.client.productionreview.controller;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.service.SubCategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    @PutMapping("/{id}")
    public ResponseEntity<SubCategorie> update(@PathVariable Long id, @RequestBody SubCategorieDto subCategoriaDto) {
        return ResponseEntity.status(HttpStatus.OK).body(subCategoriaService.updateSubCategorie( subCategoriaDto, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subCategoriaService.deleteSubCategorie(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubCategorie> get(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(subCategoriaService.getSubCategorie(id));
    }

    @GetMapping
    public ResponseEntity<List<SubCategorie>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(subCategoriaService.getAllSubCategorie());
    }

}
