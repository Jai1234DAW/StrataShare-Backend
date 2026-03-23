# Edicion de la tabla Request-Log para establecer la foreign key

# --- !Ups

ALTER TABLE `request_log`
    ADD CONSTRAINT `fk_request_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`);