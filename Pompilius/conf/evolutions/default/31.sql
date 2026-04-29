# Modificar la tabla transaction

# --- !Ups
ALTER TABLE `barter` DROP COLUMN `rejected_at`;
ALTER TABLE `transaction` RENAME COLUMN cancelled_at TO cancelled_rejected_at;
ALTER TABLE `transaction` RENAME COLUMN completed_at TO completed_successfully_at;