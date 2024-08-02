package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.repositories.jpa.SubCategorieRepository;
import com.client.productionreview.service.SubCategoriaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubCategorieDtoServiceImpl  implements SubCategoriaService {


    private final SubCategorieRepository subCategorieRepository;

    SubCategorieDtoServiceImpl(SubCategorieRepository subCategorieRepository) {
        this.subCategorieRepository = subCategorieRepository;
    }

    @Override
    public SubCategorie addSubCategorie(SubCategorieDto categorie) {

        var exists = subCategorieRepository.findByName(categorie.getName());

        if (exists.isPresent()) {
            throw new RuntimeException("Categorie already exists");
        }

        return subCategorieRepository.save(
                SubCategorie.builder()
                        .name(categorie.getName())
                        .description(categorie.getDescription())
                        .slug(categorie.getSlug())
                        .build()
        );




    }

    @Override
    public SubCategorie updateSubCategorie(SubCategorieDto categorioDto, Long id) {
        return null;
    }

    @Override
    public void deleteSubCategorie(Long id) {

    }

    @Override
    public SubCategorie getSubCategorie(Long id) {
        return null;
    }

    @Override
    public List<SubCategorie> getAllSubCategorie() {
        return List.of();
    }
}
