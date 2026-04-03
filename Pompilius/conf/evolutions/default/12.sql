# Crear tabla resource_user para gestionar acceso de usuarios a recursos

# --- !Ups
CREATE TABLE `resource_user` (
    `resource_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `resource_user_type` ENUM('OWNER', 'PURCHASED', 'ACCEPTED_AS_PAYMENT') NOT NULL,
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`resource_id`, `user_id`),

    KEY `RESOURCE_USER_TYPE_IDX` (`resource_user_type`),
    KEY `RESOURCE_USER_CREATED_IDX` (`granted_at`),

    CONSTRAINT `fk_resource_user_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT,

    CONSTRAINT `fk_resource_user_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;

