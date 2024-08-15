package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Categorie;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.repositories.jpa.CategorieRepository;
import com.client.productionreview.repositories.jpa.SubCategorieRepository;
import com.client.productionreview.service.SubCategoriaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubCategorieServiceImpl implements SubCategoriaService {


    private final SubCategorieRepository subCategorieRepository;
    private final CategorieRepository categorieRepository;

    SubCategorieServiceImpl(SubCategorieRepository subCategorieRepository, CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
        this.subCategorieRepository = subCategorieRepository;
    }

    @Override
    public SubCategorie addSubCategorie(SubCategorieDto categorie) {


        var existsCategorie = getExistsCategorie(categorie.getCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new BusinessExcepion("Categorie not exists");
        }

        var exists = getExistsSubCategoria(categorie);


        if (exists.isPresent()) {
            throw new NotFoundException("SubCategorie already exists");
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
    public SubCategorie updateSubCategorie(SubCategorieDto subCategorieDto, Long id) {
        var existsName = getExistsSubCategoria(subCategorieDto);
        var existsCategorie = getExistsCategorie(subCategorieDto.getCategorieId());

        if (existsCategorie.isEmpty()) {
            throw new NotFoundException("Categorie not exists");
        }

        if (existsName.isEmpty()) {
            throw new BusinessExcepion("SubCategorie  exists already");
        }

        var existsId = subCategorieRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new NotFoundException("SubCategorie not found");
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
            throw new NotFoundException("SubCategorie not found");
        }

        subCategorieRepository.deleteById(id);

    }

    @Override
    public SubCategorie getSubCategorie(Long id) {
        var existsId = subCategorieRepository.findById(id);

        if (existsId.isEmpty()) {
            throw new NotFoundException("SubCategorie not found");
        }

        return existsId.get();
    }

    @Override
    public List<SubCategorie> getAllSubCategorie() {
        return subCategorieRepository.findAll();
    }


    private Optional<SubCategorie> getExistsSubCategoria(SubCategorieDto categorie) {
        return subCategorieRepository.findByName(categorie.getName());

    }

    private Optional<Categorie> getExistsCategorie(Long id) {
        return categorieRepository.findById(id);

    }
}
