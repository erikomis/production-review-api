package com.client.productionreview.dtos.importer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Produto retornado pela busca da API v2 do Open Food Facts (só os campos pedidos em {@code fields}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProduct {

    private String code;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_name_pt")
    private String productNamePt;

    private String brands;

    @JsonProperty("image_front_url")
    private String imageFrontUrl;

    @JsonProperty("generic_name_pt")
    private String genericNamePt;

    private String quantity;

    @JsonProperty("nutriscore_grade")
    private String nutriscoreGrade;
}
