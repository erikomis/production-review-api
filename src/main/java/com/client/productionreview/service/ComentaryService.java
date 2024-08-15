package com.client.productionreview.service;

import com.client.productionreview.dtos.ComentaryDto;
import com.client.productionreview.model.Comentary;

import java.util.List;

public interface ComentaryService {

    public Comentary saveComentary(ComentaryDto comentary);

    public Comentary updateComentary(ComentaryDto comentary, Long id);

    public void deleteComentary(Long id);

    public Comentary getComentary(Long id);

    public List<Comentary> getComentaries();



}
