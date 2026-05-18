# Modificacion de asci a utf8mb4

# --- !Ups
ALTER TABLE users
    MODIFY username VARCHAR(32)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;