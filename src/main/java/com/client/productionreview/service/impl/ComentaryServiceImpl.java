package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.ComentaryDto;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Comentary;
import com.client.productionreview.model.Product;
import com.client.productionreview.repositories.jpa.ComentaryRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.ComentaryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ComentaryServiceImpl  implements ComentaryService {

    private  final ComentaryRepository comentaryRepository;
    private final ProductRepository productRepository;

    ComentaryServiceImpl(ComentaryRepository comentaryRepository, ProductRepository productRepository) {
        this.comentaryRepository = comentaryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Comentary saveComentary(ComentaryDto comentary) {
        Optional<Product> product = productRepository.findById(comentary.getProductId());

        if(product.isEmpty()){
            throw new RuntimeException("Product not found");
        }


        return comentaryRepository.save(Comentary.builder()
                .title(comentary.getTitle())
                .description(comentary.getDescription())
                        .note(comentary.getNote())
                .productId(product.get())
                .build());
    }

    @Override
    public Comentary updateComentary(ComentaryDto comentary, Long id) {

        Optional<Product> product = productRepository.findById(comentary.getProductId());

        if(product.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        Optional<Comentary> comentaryEntity = comentaryRepository.findById(id);

        if(comentaryEntity.isEmpty()){
            throw new NotFoundException("Comentary not found");
        }

        return comentaryRepository.save(Comentary.builder()
                .id(id)
                .title(comentary.getTitle())
                .description(comentary.getDescription())
                .note(comentary.getNote())
                .productId(product.get())
                .build());

    }

    @Override
    public void deleteComentary(Long id) {

        Optional<Comentary> comentary = comentaryRepository.findById(id);

        if(comentary.isEmpty()){
            throw new NotFoundException("Comentary not found");
        }

        comentaryRepository.deleteById(id);

    }

    @Override
    public Comentary getComentary(Long id) {

            Optional<Comentary> comentary = comentaryRepository.findById(id);

            if(comentary.isEmpty()){
                throw new NotFoundException("Comentary not found");
            }

            return comentary.get();
    }

    @Override
    public List<Comentary> getComentaries() {
        return comentaryRepository.findAll();
    }
}
