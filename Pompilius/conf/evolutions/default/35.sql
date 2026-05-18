# Migración de users_attachent a una nueva estructura con una tabla de unión para manejar múltiples tipos de adjuntos (avatar, cover photo, etc.)

# --- !Ups
-- Agregar nuevas columnas si no existen
ALTER TABLE users_attachment
    ADD COLUMN attachment_type VARCHAR(50) NOT NULL DEFAULT 'OTHER' AFTER attachment_id,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER attachment_type,
    ADD COLUMN is_current BOOLEAN NOT NULL DEFAULT true AFTER created_at;

-- Migrar datos existentes de la tabla users a users_attachment
-- Avatares
INSERT INTO users_attachment (user_id, attachment_id, attachment_type, created_at, is_current)
SELECT id, avatar, 'AVATAR', NOW(), true
FROM users
WHERE avatar IS NOT NULL
    ON DUPLICATE KEY UPDATE
                         attachment_type = 'AVATAR',
                         is_current = true;

-- Cover photos
INSERT INTO users_attachment (user_id, attachment_id, attachment_type, created_at, is_current)
SELECT id, cover_photo, 'COVER_PHOTO', NOW(), true
FROM users
WHERE cover_photo IS NOT NULL
    ON DUPLICATE KEY UPDATE
                         attachment_type = 'COVER_PHOTO',
                         is_current = true;

-- Crear índice para búsquedas eficientes
CREATE INDEX idx_user_attachment_type_current
    ON users_attachment(user_id, attachment_type, is_current);

-- Establecer la PRIMARY KEY solo en attachment_id
ALTER TABLE users_attachment
DROP PRIMARY KEY,
    ADD PRIMARY KEY (attachment_id);

-- Eliminar las columnas avatar y cover_photo de la tabla users
ALTER TABLE users
DROP COLUMN avatar,
    DROP COLUMN cover_photo;