package com.client.productionreview.dtos.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Resposta oficial da equipe ReviewStore a uma review. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String text;

    private String authorName;

    private Instant repliedAt;
}
