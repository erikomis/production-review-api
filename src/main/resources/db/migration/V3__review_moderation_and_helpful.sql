ALTER TABLE review ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE';
ALTER TABLE review ADD COLUMN moderation_reason VARCHAR(255) NULL;
ALTER TABLE review ADD COLUMN moderated_at TIMESTAMP NULL;
ALTER TABLE review ADD COLUMN moderated_by BIGINT NULL;

CREATE INDEX idx_review_status ON review (status);

CREATE TABLE IF NOT EXISTS review_helpful (
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id, user_id),
    FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
