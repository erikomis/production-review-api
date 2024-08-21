package com.client.productionreview.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "categories")
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Category {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String slug;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_categoria_id", referencedColumnName = "id", insertable = false, updatable = false)
    private SubCategory subCategoria;

    @Column(name = "sub_categoria_id")
    private Long subCategoriaId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private  LocalDateTime updatedAt;



}
