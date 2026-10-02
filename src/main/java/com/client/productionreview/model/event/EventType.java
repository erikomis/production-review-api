package com.client.productionreview.model.event;

import lombok.Getter;

/**
 * Tipos de evento de domínio publicados no Kafka. O {@code action} é o texto curto
 * mantido por compatibilidade com o consumidor antigo e com o SSE.
 */
@Getter
public enum EventType {
    USER_SIGNED_UP(EntityType.USER, "Usuário cadastrado"),
    USER_ACTIVATED(EntityType.USER, "Conta ativada"),
    USER_LOGGED_IN(EntityType.USER, "Login realizado"),
    USER_ROLE_CHANGED(EntityType.USER, "Permissão de usuário alterada"),
    USER_STATUS_CHANGED(EntityType.USER, "Status de usuário alterado"),

    CATEGORY_CREATED(EntityType.CATEGORY, "Categoria criada"),
    CATEGORY_UPDATED(EntityType.CATEGORY, "Categoria atualizada"),
    CATEGORY_DELETED(EntityType.CATEGORY, "Categoria excluída"),

    SUBCATEGORY_CREATED(EntityType.SUBCATEGORY, "Subcategoria criada"),
    SUBCATEGORY_UPDATED(EntityType.SUBCATEGORY, "Subcategoria atualizada"),
    SUBCATEGORY_DELETED(EntityType.SUBCATEGORY, "Subcategoria excluída"),

    PRODUCT_CREATED(EntityType.PRODUCT, "Produto criado"),
    PRODUCT_UPDATED(EntityType.PRODUCT, "Produto atualizado"),
    PRODUCT_DELETED(EntityType.PRODUCT, "Produto excluído"),

    PRODUCT_IMAGE_ADDED(EntityType.PRODUCT_IMAGE, "Imagem de produto adicionada"),
    PRODUCT_IMAGE_REMOVED(EntityType.PRODUCT_IMAGE, "Imagem de produto removida"),

    REVIEW_CREATED(EntityType.REVIEW, "Avaliação criada"),
    REVIEW_UPDATED(EntityType.REVIEW, "Avaliação editada"),
    REVIEW_DELETED(EntityType.REVIEW, "Avaliação excluída"),
    REVIEW_HIDDEN(EntityType.REVIEW, "Avaliação ocultada"),
    REVIEW_RESTORED(EntityType.REVIEW, "Avaliação restaurada"),

    CATALOG_IMPORT_STARTED(EntityType.IMPORT, "Importação de catálogo iniciada"),
    CATALOG_IMPORT_COMPLETED(EntityType.IMPORT, "Importação de catálogo concluída"),
    CATALOG_IMPORT_FAILED(EntityType.IMPORT, "Importação de catálogo falhou");

    private final EntityType entityType;

    private final String action;

    EventType(EntityType entityType, String action) {
        this.entityType = entityType;
        this.action = action;
    }
}
