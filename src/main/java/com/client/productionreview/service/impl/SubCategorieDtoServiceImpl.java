package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.repositories.jpa.SubCategorieRepository;
import com.client.productionreview.service.SubCategoriaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubCategorieDtoServiceImpl  implements SubCategoriaService {


    private final SubCategorieRepository subCategorieRepository;

    SubCategorieDtoServiceImpl(SubCategorieRepository subCategorieRepository) {
        this.subCategorieRepository = subCategorieRepository;
    }

    @Override
    public SubCategorie addSubCategorie(SubCategorieDto categorie) {

        var exists = getExists(categorie);

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

    private Optional<SubCategorie> getExists(SubCategorieDto categorie) {
        var exists = subCategorieRepository.findByName(categorie.getName());
        return exists;
    }

    @Override
    public SubCategorie updateSubCategorie(SubCategorieDto subCategorieDto, Long id) {
        var existsName = getExists(subCategorieDto);

        if (existsName.isEmpty()) {
            throw new RuntimeException("SubCategorie  exists already");
        }

        var existsId = subCategorieRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new RuntimeException("SubCategorie not found");
        }


        return subCategorieRepository.save(
                SubCategorie.builder()
                        .id(id)
                        .name(subCategorieDto.getName())
                        .description(subCategorieDto.getDescription())
                        .slug(subCategorieDto.getSlug())
                        .build()
        );
    }

    @Override
    public void deleteSubCategorie(Long id) {

        var existsId = subCategorieRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new RuntimeException("SubCategorie not found");
        }

        subCategorieRepository.deleteById(id);

    }

    @Override
    public SubCategorie getSubCategorie(Long id) {
        var existsId = subCategorieRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new RuntimeException("SubCategorie not found");
        }

        return existsId.get();
    }

    @Override
    public List<SubCategorie> getAllSubCategorie() {
        return subCategorieRepository.findAll();
    }
}
