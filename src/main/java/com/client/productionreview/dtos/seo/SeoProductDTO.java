package com.client.productionreview.dtos.seo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Dados prontos para o JSON-LD (schema.org Product + AggregateRating + Review) da página do produto. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String image;
    /** Só aparece quando a marca é conhecida. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String brand;
    private String url;
    /** null quando o produto não tem avaliações visíveis. */
    private AggregateRating aggregateRating;
    @Builder.Default
    private List<SeoReview> reviews = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregateRating implements Serializable {
        private static final long serialVersionUID = 1L;
        private Double ratingValue;
        private long reviewCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoReview implements Serializable {
        private static final long serialVersionUID = 1L;
        private String author;
        private Instant datePublished;
        private String reviewBody;
        private String name;
        private Long ratingValue;
    }
}
