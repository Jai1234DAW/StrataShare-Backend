# Ajuste de campo en resource

# --- !Ups
ALTER TABLE `resource`
    MODIFY COLUMN `summary` TEXT NOT NULL;

ALTER TABLE `users`
    MODIFY username VARCHAR(32)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci NOT NULL;