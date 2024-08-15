package com.client.productionreview.controller;

import com.client.productionreview.dtos.ComentaryDto;
import com.client.productionreview.model.Comentary;
import com.client.productionreview.service.ComentaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comentary")
public class ComentaryController {


    private final ComentaryService comentaryService;

    ComentaryController( ComentaryService comentaryService){
        this.comentaryService = comentaryService;
    }


    @PostMapping("/")
    public ResponseEntity <Comentary> addComentary(@Valid @RequestBody ComentaryDto comentaryDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(comentaryService.saveComentary(comentaryDto));
    }

    @PutMapping({"/{id}"})
    public ResponseEntity<Comentary> updateComentary(@Valid @RequestBody ComentaryDto comentaryDto, @PathVariable("id") Long id){
        return ResponseEntity.status(HttpStatus.OK).body(comentaryService.updateComentary(comentaryDto, id));
    }

    @DeleteMapping({"/{id}"})
    public ResponseEntity<Void> deleteComentary(@PathVariable("id") Long id){
        comentaryService.deleteComentary(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

    }

    @GetMapping({"/{id}"})
    public ResponseEntity<Comentary> getComentary(@PathVariable("id") Long id){
        return ResponseEntity.status(HttpStatus.OK).body(comentaryService.getComentary(id));
    }

    @GetMapping("/")
    public ResponseEntity<List<Comentary>> getComentaries(){
        return ResponseEntity.status(HttpStatus.OK).body(comentaryService.getComentaries());

    }



}
