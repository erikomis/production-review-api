package com.client.productionreview.service.impl;


import com.client.productionreview.dtos.CategorioDto;

import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Categorie;
import com.client.productionreview.repositories.jpa.CategorieRepository;
import com.client.productionreview.service.CategorieService;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CategorieServiceImpl implements CategorieService {


    private final CategorieRepository categorieRepository;

    CategorieServiceImpl(CategorieRepository categorieRepository){
        this.categorieRepository = categorieRepository;
    }


    @Override
    public Categorie addCatogorie(CategorioDto categorie) {

        var exists = categorieRepository.findByName(categorie.getName());

        if(exists.isPresent()){
            throw new BusinessExcepion("Categorie already exists");
        }

        return categorieRepository.save(
                Categorie.builder()
                          .name(categorie.getName())
                          .description(categorie.getDescription())
                            .slug(categorie.getSlug())
                          .build()
       );
    }

    @Override
    public Categorie updateCatogorie(CategorioDto categorioDto, Long id) {

        var categorie = categorieRepository.findById(id);

        if(categorie.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

       return  categorieRepository.save(
                Categorie.builder()
                        .id(id)
                        .name(categorioDto.getName())
                        .description(categorioDto.getDescription())
                        .slug(categorioDto.getSlug())
                        .build());
    }

    @Override
    public void deleteCatogorie(Long id) {

        var categorie = categorieRepository.findById(id);

        if(categorie.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        categorieRepository.deleteById(id);

    }

    @Override
    public Categorie getCatogorie(Long id) {
        var categorie = categorieRepository.findById(id);

        if(categorie.isEmpty()){
            throw new NotFoundException("Categorie not found");
        }

        return categorie.get();

    }

    @Override
    public List<Categorie> getAllCatogorie() {
        return categorieRepository.findAll();
    }


}
