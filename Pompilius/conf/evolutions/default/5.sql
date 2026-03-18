# Cambio de nombre de columna en la tabla attachment

# --- !Ups
ALTER TABLE `attachment`
    CHANGE COLUMN `relativePath` `relative_path` VARCHAR(256) NOT NULL ;
