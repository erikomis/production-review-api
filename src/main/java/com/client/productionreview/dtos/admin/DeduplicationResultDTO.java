package com.client.productionreview.dtos.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code POST /admin/catalog/deduplicate}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeduplicationResultDTO {

    /** Grupos de produtos com o mesmo nome normalizado na mesma subcategoria. */
    private int groups;

    private int removed;

    /** Produto mantido em cada grupo (o mais antigo). */
    @Builder.Default
    private List<Long> keptIds = new ArrayList<>();

    @Builder.Default
    private List<Long> removedIds = new ArrayList<>();
}
