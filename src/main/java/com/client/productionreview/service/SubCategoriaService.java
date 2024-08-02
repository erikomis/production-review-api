package com.client.productionreview.service;


import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.model.SubCategorie;

import java.util.List;

public interface SubCategoriaService {

    public SubCategorie addSubCategorie(SubCategorieDto categorie);

    public SubCategorie updateSubCategorie(SubCategorieDto categorioDto, Long id);

    public void deleteSubCategorie(Long id);

    public  SubCategorie getSubCategorie(Long id);

    public List<SubCategorie> getAllSubCategorie();
}
