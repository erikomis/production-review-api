package com.client.productionreview.model;

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
public class Categorie {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;

    private String slug;


    @OneToMany(mappedBy = "categorieId" ,cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubCategorie> subCategories = new ArrayList<>();


}
