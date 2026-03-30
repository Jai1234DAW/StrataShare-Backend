# Modificacion del nombre de una columna

# --- !Ups
ALTER TABLE `resource_user`
    CHANGE COLUMN `granted_at` `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ;

