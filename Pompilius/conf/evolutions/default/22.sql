# Modificacion de la tabla user

# -- !Ups
ALTER TABLE `users` ADD COLUMN `interests` TEXT NULL AFTER `email`;

CREATE TABLE `users_followers` (
  `user_id` BIGINT NOT NULL,
  `follower_id` BIGINT NOT NULL,
    `followed_at` DATETIME NOT NULL,
  PRIMARY KEY (`user_id`, `follower_id`),
  INDEX `idx_user_follower_user_id` (`user_id`),
  INDEX `idx_user_follower_follower_id` (`follower_id`),
  CONSTRAINT `fk_user_follower_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_user_follower_follower_id` FOREIGN KEY (`follower_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_user_follower_not_self` CHECK (`user_id` != `follower_id`)
) CHARSET=utf8mb4