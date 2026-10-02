-- Fase 3: busca sem acento, fotos e denúncias de avaliações, resposta oficial, notificações e seguidores.
-- SQL simples, compatível com MariaDB e com o H2 (MODE=MySQL) dos testes.
-- O backfill de product.search_name é feito pelo SearchNameBackfill na subida da aplicação
-- (a remoção de acentos usa a mesma normalização Java do save; não há função SQL portável para isso).

ALTER TABLE product ADD COLUMN search_name VARCHAR(255) NULL;
CREATE INDEX idx_product_search_name ON product (search_name);
CREATE INDEX idx_product_sub_search_name ON product (sub_category_id, search_name);

ALTER TABLE user ADD COLUMN email_notifications BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE review ADD COLUMN reply_text VARCHAR(1000) NULL;
ALTER TABLE review ADD COLUMN reply_author_id BIGINT NULL;
ALTER TABLE review ADD COLUMN replied_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS review_image (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_image_key UNIQUE (object_key),
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE
);
CREATE INDEX idx_review_image_review ON review_image (review_id);

CREATE TABLE IF NOT EXISTS review_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    details VARCHAR(500) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_report_user UNIQUE (review_id, user_id),
    CONSTRAINT fk_review_report_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_report_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    link VARCHAR(500) NULL,
    review_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);
CREATE INDEX idx_notification_user ON notification (user_id, is_read, created_at);
CREATE INDEX idx_notification_review ON notification (review_id, type);

CREATE TABLE IF NOT EXISTS product_follow (
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, user_id),
    CONSTRAINT fk_product_follow_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_follow_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);
CREATE INDEX idx_product_follow_user ON product_follow (user_id);
