package com.client.productionreview.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "categories")
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Categorie {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String slug;

    @ManyToOne
    @JoinColumn(name = "sub_categoria_id", referencedColumnName = "id")
    private SubCategorie subCategoria;



}
