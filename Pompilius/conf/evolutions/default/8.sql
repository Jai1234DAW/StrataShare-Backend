# Creación de la tabla request_log para almacenar los logs de las peticiones realizadas a la aplicación.

# --- !Ups
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