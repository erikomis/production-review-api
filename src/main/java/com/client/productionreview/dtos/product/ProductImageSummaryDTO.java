package com.client.productionreview.dtos.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String urlImage;
}
