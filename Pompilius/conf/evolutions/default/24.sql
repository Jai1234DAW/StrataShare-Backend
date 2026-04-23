# Modificacion de la tabla Attachment

# -- !Ups
ALTER TABLE `attachment` ADD COLUMN `preview_image` TINYINT(1) NOT NULL DEFAULT 0 AFTER `resource_id`;