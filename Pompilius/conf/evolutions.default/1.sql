# Crear schema inicial

# --- !Ups


CREATE TABLE `sessions` (
                            `user_id` bigint NOT NULL,
                            `token` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                            `deleted` tinyint(1) NOT NULL DEFAULT '0',
                            `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `address` varchar(39) NOT NULL,
                            `user_agent` varchar(512) DEFAULT NULL,
                            `country` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
                            PRIMARY KEY (`user_id`,`token`)
) CHARSET=utf8mb4;


CREATE TABLE `images` (
                          `id` bigint NOT NULL,
                          `user_id` bigint NOT NULL,
                          `storage` enum('CLOUDFLARE') NOT NULL,
                          `path` varchar(64) NOT NULL,
                          `uploaded` datetime NOT NULL,
                          `require_signature` tinyint(1) NOT NULL DEFAULT '0',
                          `fingerprint` varchar(2000) DEFAULT NULL,
                          `metadata` text,
                          PRIMARY KEY (`id`),
                          KEY `IMAGE_USER_IDX` (`user_id`),
                          KEY `IMAGE_PATH_IDX` (`storage`,`path`)
) CHARSET=utf8mb4;

CREATE TABLE `mail_codes` (
                              `code` varchar(16) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
                              `scope` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
                              `address` varchar(256) NOT NULL,
                              `created` datetime NOT NULL,
                              PRIMARY KEY (`code`,`scope`,`address`)
) CHARSET=utf8mb4;


CREATE TABLE `request_log` (
                               `id` bigint NOT NULL,
                               `user_id` bigint NOT NULL,
                               `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `address` varchar(39) NOT NULL,
                               `method` varchar(16) NOT NULL,
                               `path` varchar(1024) NOT NULL,
                               `body` text,
                               `metadata` text,
                               PRIMARY KEY (`id`),
                               KEY `REQUEST_LOG_USER_IDX` (`user_id`),
                               KEY `REQUEST_LOG_PATH_IDX` (`path`(768)),
                               KEY `REQUEST_LOG_ADDRESS_IDX` (`address`)
) CHARSET=utf8mb4;

CREATE TABLE `roles` (
                         `id` bigint NOT NULL,
                         `name` varchar(64) NOT NULL,
                         `notes` text,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `NAME_IDX` (`name`)
) CHARSET=utf8mb4;


CREATE TABLE `role_permissions` (
                                    `role_id` bigint NOT NULL,
                                    `permission` enum(
        'ADMIN_ROLES',
        'ADMIN_USERS',
        'ADMIN_ACCOUNTS',
        'ADMIN_REGIONS',
        'VIEW_ROLES',
        'VIEW_USERS',
        'VIEW_ACCOUNTS',
        'VIEW_REGIONS',
        'VIEW_ALL_REPORTS',
        'SUPPORT',
        'LOGIN_AS',
        'IMAGES'
        ) NOT NULL,
                                    PRIMARY KEY (`role_id`,`permission`)
) CHARSET=utf8mb4;

CREATE TABLE `users` (
                         `id` bigint NOT NULL,
                         `username` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
                         `password_hash` varchar(96) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                         `enabled` tinyint(1) NOT NULL DEFAULT '1',
                         `level` int NOT NULL DEFAULT '0',
                         `email` varchar(256) NOT NULL,
                         `country` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
                         `first_name` varchar(64) DEFAULT NULL,
                         `last_name` varchar(64) DEFAULT NULL,
                         `phone` varchar(32) DEFAULT NULL,
                         `avatar` bigint DEFAULT NULL,
                         `header` bigint DEFAULT NULL,
                         `language` varchar(3) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
                         `created` datetime NOT NULL,
                         `updated` datetime NOT NULL,
                         `notes` text,
                         `bio` text,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `USERS_USERNAME_IDX` (`username`),
                         UNIQUE KEY `USER_EMAIL_IDX` (`email`)
) CHARSET=utf8mb4;

CREATE TABLE `user_roles` (
                              `user_id` bigint NOT NULL,
                              `role_id` bigint NOT NULL,
                              PRIMARY KEY (`user_id`,`role_id`),
                              KEY `ROLE_IDX` (`role_id`)
) CHARSET=utf8mb4;





