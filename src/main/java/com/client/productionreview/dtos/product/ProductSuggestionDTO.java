package com.client.productionreview.dtos.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Item do autocompletar da busca. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private String categoryName;

    /** Usado pela projeção JPQL (a imagem é preenchida depois). */
    public ProductSuggestionDTO(Long id, String name, String slug, String categoryName) {
        this(id, name, slug, null, categoryName);
    }
}
