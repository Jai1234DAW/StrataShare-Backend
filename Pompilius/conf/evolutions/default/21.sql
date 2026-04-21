# Modificacion de la tabla resource para saber si el recurso es pago o barter

# -- !Ups
ALTER TABLE `resource` ADD COLUMN is_barter BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE `sample` DROP COLUMN `name`;

ALTER TABLE `study` DROP COLUMN `name`;

ALTER TABLE `resource` ADD COLUMN `name` VARCHAR(255) NOT NULL AFTER `id`;