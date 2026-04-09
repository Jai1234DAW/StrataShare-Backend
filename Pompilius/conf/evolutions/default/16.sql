# Agregar campo price a resource

# --- !Ups
ALTER TABLE `resource`
ADD COLUMN `price` DECIMAL(10,2) NULL AFTER `summary`,
ADD INDEX `RESOURCE_PRICE_IDX` (`price`);

