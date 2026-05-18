# Modificacion para la edicion de foto de portada

# -- !Ups
ALTER TABLE `users` ADD COLUMN `cover_photo` BIGINT DEFAULT NULL AFTER `avatar`;