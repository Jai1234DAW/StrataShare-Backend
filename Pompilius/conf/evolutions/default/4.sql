# Añadir la tabla users_attachments

# --- !Ups

CREATE TABLE `users_attachment` (
   `user_id` BIGINT NOT NULL,
   `attachment_id` BIGINT NOT NULL,

   PRIMARY KEY (`user_id`, `attachment_id`),

   INDEX `user_attachment_idx` (`attachment_id`),

   CONSTRAINT `fk_users_attachment_attachment`
       FOREIGN KEY (`attachment_id`)
           REFERENCES `attachment` (`id`)
           ON DELETE CASCADE
           ON UPDATE CASCADE,

   CONSTRAINT `fk_users_attachment_users`
       FOREIGN KEY (`user_id`)
           REFERENCES `users` (`id`)
           ON DELETE CASCADE
           ON UPDATE CASCADE
);