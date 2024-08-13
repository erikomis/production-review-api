package com.client.productionreview.service;

import com.client.productionreview.dtos.SubCategorieDto;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.SubCategorie;
import com.client.productionreview.repositories.jpa.SubCategorieRepository;
import com.client.productionreview.service.impl.SubCategorieServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SubCategorieServiceTest {

    @InjectMocks
    private SubCategorieServiceImpl subCategorieService;


    @Mock
    private SubCategorieRepository subCategorieRepository;

    private SubCategorieDto subCategorieDto;

    @BeforeEach
    public void LoadSubCategorie() {
        subCategorieDto = new SubCategorieDto();
        subCategorieDto.setName("teste");
        subCategorieDto.setDescription("teste");
        subCategorieDto.setSlug("teste");
    }


    @Test
    void given_create_when_then_createSubCategoria() {



        SubCategorie subCategorie = new SubCategorie();

        subCategorie.setName(subCategorieDto.getName());
        subCategorie.setDescription(subCategorieDto.getDescription());
        subCategorie.setSlug( subCategorieDto.getSlug());

        when(subCategorieService.addSubCategorie(subCategorieDto)).thenReturn(subCategorie);

        when(subCategorieRepository.save(subCategorie)).thenReturn(subCategorie);


        Assertions.assertEquals(subCategorie, subCategorieService.addSubCategorie(subCategorieDto));


    }


    @Test
    void given_create_when_then_createSubCategoria_throw() {

        SubCategorie subCategorie = new SubCategorie();
        subCategorie.setId(1L);
        subCategorie.setName("teste");
        subCategorie.setDescription("teste");
        subCategorie.setSlug("teste");



        when(subCategorieRepository.findByName(subCategorieDto.getName())).thenReturn(Optional.of(subCategorie));

        Assertions.assertThrows(NotFoundException.class, () -> subCategorieService.addSubCategorie(subCategorieDto));

        // Verifica que o repositório não foi chamado para salvar ou buscar a subcategoria
        verify(subCategorieRepository, times(1)).findByName(subCategorieDto.getName());
        verify(subCategorieRepository, times(0)).save(any());


    }

    @Test
    void given_findById_when_then_findSubCategoria() {

        SubCategorie subCategorie = new SubCategorie();
        subCategorie.setId(1L);
        subCategorie.setName("teste");
        subCategorie.setDescription("teste");
        subCategorie.setSlug("teste");

        when(subCategorieRepository.findById(1L)).thenReturn(Optional.of(subCategorie));

        Assertions.assertEquals(subCategorie, subCategorieService.getSubCategorie(1L));

        verify(subCategorieRepository, times(1)).findById(1L);
    }

    @Test
    void given_findById_when_then_findSubCategoria_throw() {

        when(subCategorieRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> subCategorieService.getSubCategorie(1L));

        verify(subCategorieRepository, times(1)).findById(1L);
    }



}
