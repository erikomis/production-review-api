package com.client.productionreview.dtos.category;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CategoryRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must have at most 255 characters")
    private String name;
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must have at most 255 characters")
    private String description;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must have at most 255 characters")
    private String slug;



}
