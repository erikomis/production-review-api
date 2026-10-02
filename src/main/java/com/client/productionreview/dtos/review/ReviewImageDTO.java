package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Foto de uma review; {@code url} é relativa à API (ex.: {@code /api/v1/files/reviews/12/uuid.jpg}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FILES_PATH = "/api/v1/files/";

    private Long id;

    private String url;

    public static ReviewImageDTO of(Long id, String objectKey) {
        return new ReviewImageDTO(id, FILES_PATH + objectKey);
    }
}
