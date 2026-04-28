# Modificacion de la fecha porque realmente debe exisir la fecha donde se tomaron los datos

# --- !Ups
ALTER TABLE `sample` ADD COLUMN `collected_date` DATETIME NOT NULL DEFAULT  CURRENT_TIMESTAMP AFTER `resource_id`;