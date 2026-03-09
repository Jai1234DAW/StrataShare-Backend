# Tabla de sesiones de usuario

# --- !Ups
CREATE TABLE `session` (
       `id` bigint NOT NULL,
       `user_id` bigint NOT NULL,
       `deleted` tinyint(1) NOT NULL DEFAULT 0,
       `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
       `address` varchar(39) NOT NULL,
       `user_agent` varchar(512) DEFAULT NULL,
       `country` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
       PRIMARY KEY (`id`),
       CONSTRAINT `fk_sessions_user`
           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) CHARSET=utf8mb4;