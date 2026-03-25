# Crear tabla de muestras (samples)

# --- !Ups

CREATE TABLE `samples` (
     `id` BIGINT NOT NULL,
     `name` VARCHAR(256) NOT NULL,
     `description` TEXT NULL,
     `minerals` TEXT NOT NULL,
     `collection_methods` TEXT NOT NULL,
     `is_fresh` TINYINT(1) NOT NULL,
     `sample_type` VARCHAR(256) NOT NULL,
     `materials_used` TEXT NOT NULL,
     `rock_type` VARCHAR(256) NOT NULL,
     `geological_processes` TEXT NOT NULL,
     `created` DATETIME NOT NULL,
     `updated` DATETIME NOT NULL,

     PRIMARY KEY (`id`),
     KEY `IDX_SAMPLE_NAME` (`name`),
     KEY `IDX_SAMPLE_TYPE` (`sample_type`),
     KEY `IDX_ROCK_TYPE` (`rock_type`),
     KEY `IDX_IS_FRESH` (`is_fresh`),
     KEY `IDX_SAMPLE_CREATED` (`created`)
) CHARSET=utf8mb4;

# --- !Downs

DROP TABLE IF EXISTS `samples`;

