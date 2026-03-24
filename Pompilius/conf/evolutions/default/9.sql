# Edicion de la tabla Request-Log para establecer la foreign key y creacion de la tabla mailsent

# --- !Ups

ALTER TABLE `request_log`
    ADD CONSTRAINT `fk_request_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`);

CREATE TABLE `mail_sent` (
     `id` BIGINT NOT NULL,
     `mail_type` ENUM('WELCOME') NOT NULL,
     `address` VARCHAR(256) NOT NULL,
     `sent_at` DATETIME NOT NULL DEFAULT current_timestamp,
     `user_id` BIGINT NOT NULL,
     `metadata` TEXT NULL,
     PRIMARY KEY (`id`),
     INDEX `MAIL_TYPE_IDX` (`mail_type` ASC),
     INDEX `PERSON_IDX` (`user_id` ASC),
     INDEX `ADDRESS_IDX` (`address` ASC),
    CONSTRAINT `fk_mail_sent_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE CASCADE
)CHARSET=utf8mb4;