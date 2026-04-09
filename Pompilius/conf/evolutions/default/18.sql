# Actualizar transaction para soportar BARTER

# --- !Ups
# Hacer opcionales los campos específicos de pagos pues no aplican para BARTER
ALTER TABLE `transaction`
    MODIFY COLUMN `gateway` ENUM('STRIPE', 'STRIPE_MOBILE') NULL DEFAULT NULL;

ALTER TABLE `transaction`
    MODIFY COLUMN `gateway_payment_id` VARCHAR(255) NULL DEFAULT NULL;

# Quitar el índice único que falla con NULLs
ALTER TABLE `transaction`
DROP INDEX `unique_gateway_payment_id`;

# Agregar índice único solo para casos no-null (cuando es pago real)
ALTER TABLE `transaction`
    ADD UNIQUE KEY `UNIQUE_GATEWAY_PAYMENT_ID` (`gateway_payment_id`);


# Crear tabla barter para trueques

# Tabla barter
CREATE TABLE `barter` (
    `barter_id` BIGINT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `offered_resource_id` BIGINT NOT NULL,
    `rejected_at` DATETIME NULL,

    PRIMARY KEY (`barter_id`),
    UNIQUE KEY `UNIQUE_TRANSACTION_ID` (`transaction_id`),
    KEY `BARTER_OFFERED_RESOURCE_IDX` (`offered_resource_id`),

    CONSTRAINT `fk_barter_transaction`
      FOREIGN KEY (`transaction_id`) REFERENCES `transaction`(`id`)
          ON DELETE CASCADE,

    CONSTRAINT `fk_barter_offered_resource`
      FOREIGN KEY (`offered_resource_id`) REFERENCES `resource`(`id`)
          ON DELETE RESTRICT
) CHARSET=utf8mb4;
