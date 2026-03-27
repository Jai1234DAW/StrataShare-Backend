# Modificacion del nombre de la tabla session a sessions para evitar conflictos y agregar el campo updated_at para guardar la fecha de actualizacion de la sesion

# --- !Ups
ALTER TABLE `session`
    ADD COLUMN `updated_at` DATETIME NULL AFTER `country`, RENAME TO  `sessions`;
