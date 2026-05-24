# Ajuste de campos en resource, study y payment_intent, y corregir FK de payment

# --- !Ups
ALTER TABLE `resource`
    MODIFY `location` VARCHAR(256) NOT NULL;

ALTER TABLE `study`
    MODIFY COLUMN antecedents TINYINT(1) NOT NULL DEFAULT 1;

-- Remover FK incorrecta de payment a transaction
ALTER TABLE `payment`
    DROP CONSTRAINT `fk_payment_transaction`;

-- Agregar FK correcta de payment a payment_intent con claves compuestas
ALTER TABLE `payment`
    ADD CONSTRAINT `fk_payment_payment_intent`
        FOREIGN KEY (`id`, `transaction_id`)
            REFERENCES `payment_intent` (`payment_id`, `transaction_id`)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;
