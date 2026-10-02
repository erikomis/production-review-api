package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product")
public class Product implements Serializable {

    private static final long serialVersionUID = 3L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String slug;

    @ManyToOne
    @JoinColumn(name = "sub_category_id", referencedColumnName = "id", insertable = false, updatable = false)
    private SubCategory subCategorie;

    @Column(name = "sub_category_id")
    private Long subCategorieId;

    @OneToMany(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<ProductImage> productImages;


    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private  Instant updatedAt;
}





