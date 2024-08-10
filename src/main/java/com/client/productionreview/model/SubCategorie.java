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
@Table(name = "sub_categorie")
@Entity
public class SubCategorie {

    @Id
    private Long id;
    private String name;
    private String description;
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", referencedColumnName="id",nullable=false,unique=true)
    private Categorie categorieId;

//    @OneToMany(mappedBy = "subCategorie")
//    private List<Product> productions = new ArrayList<>();

}
