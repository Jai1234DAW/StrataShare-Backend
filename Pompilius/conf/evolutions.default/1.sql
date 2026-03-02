# Crear schema inicial

# --- !Ups

CREATE TABLE `role` (
     `role_id` bigint NOT NULL,
     `name` varchar(64) NOT NULL,
     `notes` text,
     PRIMARY KEY (`id`),
     UNIQUE KEY `NAME_IDX` (`name`)
) CHARSET=utf8mb4;


CREATE TABLE `role_permissions` (
    `role_id` BIGINT NOT NULL,
    `permission` ENUM(
            -- Admin
            'ADMIN_ROLES',
        'ADMIN_USERS',
        'ADMIN_ACCOUNTS',
        'ADMIN_REGIONS',

        -- Readonly
        'VIEW_ROLES',
        'VIEW_USERS',
        'VIEW_ACCOUNTS',
        'VIEW_REGIONS',
        'VIEW_ALL_REPORTS',

        -- Support
        'SUPPORT',
        'LOGIN_AS',
        'IMAGES',

        -- Estudiante
        'CREATE_STUDIES',
        'EDIT_STUDIES',
        'DELETE_STUDIES',
        'CREATE_SAMPLES',
        'EDIT_SAMPLES',
        'DELETE_SAMPLES',
        'RATE_STUDIES',
        'REQUEST_ACCESS',
        'PARTICIPATE_TRADES',
        'MONETIZE_RESOURCES',
        'VIEW_MAPS',
        'VIEW_STATS_BASIC',
        'VIEW_HISTORY',

        -- Profesional
        'UNLIMITED_TRADES',
        'VIEW_STATS_FULL',
        'RATE_CREDIBILITY',
        'GRADE_STUDIES',

        -- Aficionado o Investigador independiente
        'VIEW_PUBLIC_STUDIES',
        'COMMENT_PUBLIC',
        'LIMITED_TRADES',
        'PURCHASE_RESOURCES',
        'VIEW_HISTORY_LIMITED'
) NOT NULL,
    PRIMARY KEY (`role_id`, `permission`),
    CONSTRAINT `fk_role_permissions_role`
        FOREIGN KEY (`role_id`)
        REFERENCES `roles`(`role_id`)
        ON DELETE CASCADE
) CHARSET=utf8mb4;


CREATE TABLE `users` (
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
     `role_id` bigint NOT NULL,

     PRIMARY KEY (`id`),
     UNIQUE KEY `USERS_USERNAME_IDX` (`username`),
     UNIQUE KEY `USER_EMAIL_IDX` (`email`),
     CONSTRAINT `fk_users_role`
         FOREIGN KEY (`role_id`)
             REFERENCES `role`(`id`)
             ON DELETE RESTRICT
) CHARSET=utf8mb4;


