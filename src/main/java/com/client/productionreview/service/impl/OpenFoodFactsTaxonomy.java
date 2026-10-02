package com.client.productionreview.service.impl;

import java.util.List;

/** Taxonomia fixa usada na importação do Open Food Facts (nomes em PT, tags da OFF em inglês). */
final class OpenFoodFactsTaxonomy {

    record Sub(String name, String slug, String tag) {
    }

    record Cat(String name, String slug, List<Sub> subCategories) {
    }

    static final List<Cat> CATEGORIES = List.of(
            new Cat("Bebidas", "bebidas", List.of(
                    new Sub("Refrigerantes", "refrigerantes", "sodas"),
                    new Sub("Sucos e néctares", "sucos-e-nectares", "fruit-juices"),
                    new Sub("Cafés", "cafes", "coffees"))),
            new Cat("Laticínios", "laticinios", List.of(
                    new Sub("Leites", "leites", "milks"),
                    new Sub("Iogurtes", "iogurtes", "yogurts"),
                    new Sub("Queijos", "queijos", "cheeses"))),
            new Cat("Café da manhã", "cafe-da-manha", List.of(
                    new Sub("Cereais matinais", "cereais-matinais", "breakfast-cereals"),
                    new Sub("Biscoitos", "biscoitos", "biscuits"),
                    new Sub("Pães", "paes", "breads"))),
            new Cat("Doces e snacks", "doces-e-snacks", List.of(
                    new Sub("Chocolates", "chocolates", "chocolates"),
                    new Sub("Salgadinhos", "salgadinhos", "crisps"),
                    new Sub("Sorvetes", "sorvetes", "ice-creams"))));

    static int totalSteps() {
        return CATEGORIES.stream().mapToInt(cat -> cat.subCategories().size()).sum();
    }

    private OpenFoodFactsTaxonomy() {
    }
}
