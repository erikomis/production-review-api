package com.client.productionreview.controller;


import com.client.productionreview.dtos.CategorioDto;
import com.client.productionreview.model.Categorie;
import com.client.productionreview.service.CategorieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/categorie")
public class CategorieController {

    private final CategorieService categorieService;

    public  CategorieController(CategorieService categorieService){
        this.categorieService = categorieService;
    }

    @PostMapping("/")
    public ResponseEntity<Categorie> addCatogorie(@RequestBody CategorioDto categorie){
        return  ResponseEntity.status(HttpStatus.CREATED).body(categorieService.addCatogorie(categorie));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Categorie> updateCatogorie(@PathVariable("id") Long id, @RequestBody CategorioDto categorioDto) {
        categorieService.updateCatogorie(categorioDto, id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Categorie> deleteCatogorie(@PathVariable("id") Long id) {
        categorieService.deleteCatogorie(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getCatogorie(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(categorieService.getCatogorie(id));
    }

    @GetMapping("/")
    public ResponseEntity<List<Categorie>> getAllCatogorie() {
        return ResponseEntity.status(HttpStatus.OK).body(categorieService.getAllCatogorie());
    }

}
