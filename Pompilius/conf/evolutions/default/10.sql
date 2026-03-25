# Crear tabla de estudios (studies) con relaciones N:M a samples y attachments

# --- !Ups

CREATE TABLE `studies` (
     `id` BIGINT NOT NULL,
     `name` VARCHAR(256) NOT NULL,
     `visibility` VARCHAR(32) NOT NULL,
     `localization` VARCHAR(256) NOT NULL,
     `start_date` DATETIME NOT NULL,
     `end_date` DATETIME NULL,
     `description` TEXT NOT NULL,
     `coordinates` TEXT NOT NULL,
     `observations` TEXT NULL,
     `summary` TEXT NULL,
     `area` TEXT NOT NULL,
     `methods` TEXT NOT NULL,
     `created` DATETIME NOT NULL,
     `updated` DATETIME NOT NULL,

     PRIMARY KEY (`id`),
     KEY `IDX_STUDY_NAME` (`name`),
     KEY `IDX_STUDY_VISIBILITY` (`visibility`),
     KEY `IDX_STUDY_LOCALIZATION` (`localization`),
     KEY `IDX_STUDY_START_DATE` (`start_date`),
     KEY `IDX_STUDY_CREATED` (`created`)
) CHARSET=utf8mb4;

-- Tabla intermedia para relación N:M entre estudios y archivos adjuntos
CREATE TABLE `studies_attachments` (
     `study_id` BIGINT NOT NULL,
     `attachment_id` BIGINT NOT NULL,

     PRIMARY KEY (`study_id`, `attachment_id`),
     KEY `IDX_ATTACHMENT` (`attachment_id`),
     CONSTRAINT `fk_studies_attachments_study`
         FOREIGN KEY (`study_id`)
             REFERENCES `studies` (`id`)
             ON DELETE CASCADE
) CHARSET=utf8mb4;

-- Tabla intermedia para relación N:M entre estudios y muestras
CREATE TABLE `study_samples` (
     `study_id` BIGINT NOT NULL,
     `sample_id` BIGINT NOT NULL,

     PRIMARY KEY (`study_id`, `sample_id`),
     KEY `IDX_SAMPLE` (`sample_id`),
     CONSTRAINT `fk_study_samples_study`
         FOREIGN KEY (`study_id`)
             REFERENCES `studies` (`id`)
             ON DELETE CASCADE
) CHARSET=utf8mb4;

# --- !Downs

DROP TABLE IF EXISTS `study_samples`;
DROP TABLE IF EXISTS `studies_attachments`;
DROP TABLE IF EXISTS `studies`;


