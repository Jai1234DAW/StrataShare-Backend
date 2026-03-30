# Crear tabla base de recursos (resource), muestras (sample), estudios (study) y relación N:M con attachments

# --- !Ups
# Tabla base de recursos
CREATE TABLE `resource` (
    `id` BIGINT NOT NULL,
    `resource_type` ENUM('SAMPLE', 'STUDY') NOT NULL,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    `visibility` ENUM('PUBLIC', 'PRIVATE', 'SHARED') NOT NULL DEFAULT 'PRIVATE',
    `created` DATETIME NOT NULL,
    `updated` DATETIME NOT NULL,
    `localization` VARCHAR(256) NULL,
    `observations` TEXT NULL,
    `summary` TEXT NULL,

    PRIMARY KEY (`id`),
    KEY `IDX_RESOURCE_TYPE` (`resource_type`),
    KEY `IDX_RESOURCE_VISIBILITY` (`visibility`),
    KEY `IDX_RESOURCE_CREATED` (`created`)
) CHARSET=utf8mb4;

# Tabla de muestras (sample)
CREATE TABLE `sample` (
    `id` BIGINT NOT NULL,
    `resource_id` BIGINT NOT NULL,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    `name` VARCHAR(256) NOT NULL,
    `minerals` TEXT NULL,
    `collection_methods` TEXT NULL,
    `is_fresh` TINYINT(1) NOT NULL,
    `sample_type` VARCHAR(256) NULL,
    `materials_used` TEXT NULL,
    `rock_type` VARCHAR(256) NULL,
    `geological_processes` TEXT NULL,

    PRIMARY KEY (`resource_id,id`),
    UNIQUE KEY `unique_resource_id` (`resource_id`),
    KEY `IDX_SAMPLE_NAME` (`name`),
    KEY `IDX_SAMPLE_TYPE` (`sample_type`),
    KEY `IDX_ROCK_TYPE` (`rock_type`),
    KEY `IDX_IS_FRESH` (`is_fresh`),
    CONSTRAINT `fk_sample_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;

# Tabla de estudios (study)
CREATE TABLE `study` (
    `id` BIGINT NOT NULL,
    `resource_id` BIGINT NOT NULL,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    `name` VARCHAR(256) NOT NULL,
    `start_date` DATETIME NOT NULL,
    `end_date` DATETIME NULL,
    `description` TEXT NOT NULL,
    `coordinates` TEXT NOT NULL,
    `area` ENUM('GEOMORPHOLOGY', 'GEOPHYSICS', 'PETROLEUM_GEOLOGY', 'PETROLOGY', 'MINERALOGY', 'STRATIGRAPHY', 'PALEONTOLOGY', 'VOLCANOLOGY', 'SEISMOLOGY', 'GEODYNAMICS', 'HYDROGEOLOGY', 'ENVIRONMENTAL_GEOLOGY', 'SEDIMENTOLOGY', 'GEOMETALLURGY', 'GEOTECHNICS', 'MARINE_GEOLOGY', 'HISTORICAL_GEOLOGY', 'OTHER') NOT NULL,
    `methods` TEXT NULL,
    `authors` TEXT NOT NULL,
    `section` TINYINT(1) NOT NULL,
    `antecedents` TINYINT(1) NULL DEFAULT 1,
    `name_section` VARCHAR(256) NULL,

    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_resource_id` (`resource_id`),
    KEY `IDX_STUDY_NAME` (`name`),
    KEY `IDX_STUDY_START_DATE` (`start_date`),
    CONSTRAINT `fk_study_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;

# Tabla de relación N:M entre resource y attachment
CREATE TABLE `resource_attachment` (
    `resource_id` BIGINT NOT NULL,
    `attachment_id` BIGINT NOT NULL,

    PRIMARY KEY (`resource_id`, `attachment_id`),
    KEY `IDX_ATTACHMENT` (`attachment_id`),
    CONSTRAINT `fk_resource_attachment_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT ,
    CONSTRAINT `fk_resource_attachment_attachment`
        FOREIGN KEY (`attachment_id`) REFERENCES `attachment`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;