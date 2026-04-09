# Eliminar el campo is_public de attachment

# --- !Ups
ALTER TABLE `attachment`
DROP COLUMN `is_public`;
