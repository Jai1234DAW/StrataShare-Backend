# Creacion de la tabla REVIEW

# --- !Ups
CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `resource_id` BIGINT NOT NULL,
    `rating` INT NOT NULL,
    `comment` TEXT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `REVIEW_USER_IDX` (`user_id`),
    KEY `REVIEW_RESOURCE_IDX` (`resource_id`),
    CONSTRAINT `fk_review_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
        ON DELETE RESTRICT,
    CONSTRAINT `fk_review_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;


