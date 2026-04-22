# Creación de tablas para el sistema de badges y eventos

# --- !Ups
-- Tabla de eventos de usuario (relación N:M entre usuarios y eventos)
CREATE TABLE `users_events` (
   `id` BIGINT NOT NULL,
   `user_id` BIGINT NOT NULL,
   `event` ENUM('PURCHASE_COMPLETED', 'BARTER_COMPLETED', 'SAMPLE_UPLOADED', 'STUDY_UPLOADED') NOT NULL,
   `created` DATETIME NOT NULL,
   PRIMARY KEY (`id`),
   INDEX `idx_users_events_user_id` (`user_id`),
   INDEX `idx_users_events_event` (`event`),
   INDEX `idx_users_events_user_event` (`user_id`, `event`),
   CONSTRAINT `fk_users_events_users`
       FOREIGN KEY (`user_id`)
           REFERENCES `users` (`id`)
           ON DELETE RESTRICT
) CHARSET=utf8mb4;

-- Tabla de badges (insignias geológicas)
CREATE TABLE `badge` (
    `id` BIGINT NOT NULL,
    `badge_type` ENUM(
    'SEDIMENT_COLLECTOR', 'MINERAL_PROSPECTOR', 'CRYSTAL_SEEKER', 'DIAMOND_EXPLORER',
    'FOSSIL_TRADER', 'ROCK_EXCHANGER', 'GEMSTONE_SWAPPER', 'GEODE_MASTER',
    'STRATA_CONTRIBUTOR', 'GEOLOGICAL_LEGEND'
) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT NOT NULL,
  `image_url` VARCHAR(255) NULL,
  `created` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_badge_type` (`badge_type`)
) CHARSET=utf8mb4;

-- Tabla relacional N:M entre usuarios y badges (PK compuesta)
CREATE TABLE `users_badge` (
   `user_id` BIGINT NOT NULL,
   `badge_id` BIGINT NOT NULL,
   `earned_at` DATETIME NOT NULL,
   PRIMARY KEY (`user_id`, `badge_id`),
   INDEX `idx_user_badges_badge_id` (`badge_id`),
   CONSTRAINT `fk_users_badges_user`
       FOREIGN KEY (`user_id`)
           REFERENCES `users` (`id`)
           ON DELETE RESTRICT,
   CONSTRAINT `fk_users_badges_badge`
       FOREIGN KEY (`badge_id`)
           REFERENCES `badge` (`id`)
           ON DELETE CASCADE
) CHARSET=utf8mb4;
