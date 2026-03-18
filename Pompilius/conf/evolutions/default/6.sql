# Modificacion de la tabla attachment para eliminar el campo fingerprint, es redundante

# --- !Ups
ALTER TABLE `attachment`
DROP COLUMN `fingerprint`;