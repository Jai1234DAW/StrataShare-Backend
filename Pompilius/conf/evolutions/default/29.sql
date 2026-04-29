# Modificacion  de tabla userResource

# --- !Ups
ALTER TABLE `resource_user` ADD COLUMN `updated` DATETIME AFTER `created`;