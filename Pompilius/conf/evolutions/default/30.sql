# Nuevo ENUM para resource_user_type

# --- !Ups
ALTER TABLE `resource_user` MODIFY COLUMN `resource_user_type` ENUM('OWNER', 'PURCHASED', 'ACCEPTED_AS_PAYMENT', 'BARTERED') NOT NULL;