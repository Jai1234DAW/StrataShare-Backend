#Tabla de usuarios y roles

# --- !Ups
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `ROLE_IDX` (`role_id`),
  CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) CHARSET=utf8mb4;