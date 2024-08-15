package com.client.productionreview.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentaryDto {

    private Long id;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
//    @NotBlank(message = "Image is required")
//    private String image;
    @NotBlank(message = "Note is required")
    private  Long  note;

    @NotBlank(message = "Product is required")
    private Long productId;
}
