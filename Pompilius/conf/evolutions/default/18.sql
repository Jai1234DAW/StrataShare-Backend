# Crear tabla barter para trueques

# --- !Ups

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
