package com.client.productionreview.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Perfil público (sem e-mail). Contagens e média consideram só reviews visíveis. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDTO {

    private String username;
    private String name;
    private Instant memberSince;
    private long reviewsCount;
    private long helpfulReceived;
    /** Média das notas dadas (1 casa); null sem reviews. */
    private Double averageNoteGiven;
}
