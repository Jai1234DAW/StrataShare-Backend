# Creacion de una tabla attachment que gestionará archivos de la aplicacion

# --- !Ups

CREATE TABLE `attachment` (
   `id` BIGINT NOT NULL,
   `node` INT NOT NULL,
   `relativePath` VARCHAR(256) NOT NULL,
   `filename` VARCHAR(256) NOT NULL,
   `description` VARCHAR(256) NULL,
   `content_type` VARCHAR(256) NOT NULL,
   `size` BIGINT NULL,
   `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `is_public` TINYINT DEFAULT 1,
   `fingerprint` VARCHAR(200) NULL,
    `deleted` TINYINT DEFAULT 0,
    `metadata` TEXT NULL,
   PRIMARY KEY (`id`)
) CHARSET=utf8mb4;