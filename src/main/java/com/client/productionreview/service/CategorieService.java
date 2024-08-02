package com.client.productionreview.service;

import com.client.productionreview.dtos.CategorioDto;
import com.client.productionreview.model.Categorie;
import java.util.List;

public interface CategorieService {


    public Categorie addCatogorie(CategorioDto categorie);

    public Categorie updateCatogorie(CategorioDto categorioDto, Long id);

    public void deleteCatogorie(Long id);

    public  Categorie getCatogorie(Long id);

    public List<Categorie> getAllCatogorie();
}
