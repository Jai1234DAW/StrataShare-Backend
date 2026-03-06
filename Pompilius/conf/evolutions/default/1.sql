# Crear schema inicial

# --- !Ups

CREATE TABLE `user` (
     `id` bigint NOT NULL,
     `username` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
     `password_hash` varchar(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
     `enabled` tinyint(1) NOT NULL DEFAULT '1',
     `email` varchar(256) NOT NULL,
     `country` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
     `first_name` varchar(64) DEFAULT NULL,
     `last_name` varchar(64) DEFAULT NULL,
     `phone` varchar(32) DEFAULT NULL,
     `avatar` bigint DEFAULT NULL,
     `language` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
     `created` datetime NOT NULL,
     `updated` datetime NOT NULL,
     `notes` text,
     `bio` text,

     PRIMARY KEY (`id`),
     UNIQUE KEY `USERS_USERNAME_IDX` (`username`),
     UNIQUE KEY `USER_EMAIL_IDX` (`email`)
) CHARSET=utf8mb4;


CREATE TABLE `user_role` (
                             `user_id` BIGINT NOT NULL,
                             `role` ENUM('STUDENT', 'PROFESSIONAL', 'AMATEUR', 'SUPPORT', 'ADMIN') NOT NULL,
                             PRIMARY KEY (`user_id`, `role`),
                             CONSTRAINT `fk_user_role_user`
                                 FOREIGN KEY (`user_id`)
                                     REFERENCES `user` (`id`)
                                     ON DELETE CASCADE
) ENGINE=InnoDB
CHARSET=utf8mb4;

