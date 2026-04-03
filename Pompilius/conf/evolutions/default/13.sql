# Agregar campo deleted a resource_user y remover de resource, sample, study

# --- !Ups
ALTER TABLE `resource_user` ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 AFTER `created`;

ALTER TABLE `resource` DROP COLUMN `deleted`;

ALTER TABLE `sample` DROP COLUMN `deleted`;


ALTER TABLE `study` DROP COLUMN `deleted`;

ALTER TABLE `study_sample` DROP COLUMN `deleted`;