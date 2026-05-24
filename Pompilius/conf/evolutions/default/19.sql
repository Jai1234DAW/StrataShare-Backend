# Crear tabla payment_intent

# --- !Ups
CREATE TABLE `payment_intent` (
    `payment_id` BIGINT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `gateway` ENUM('STRIPE', 'STRIPE_MOBILE') NOT NULL,
    `gateway_intent_id` VARCHAR(255) NOT NULL,
    `price` DECIMAL (10, 2) NOT NULL,
    `surcharge` DECIMAL(10, 2) NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `status` ENUM('requires_payment_method', 'requires_confirmation', 'requires_action', 'processing', 'succeeded', 'canceled') NOT NULL,
    `discount` DECIMAL(10, 2) NOT NULL,
    `url` VARCHAR(2000) NULL,
    `created` DATETIME NOT NULL,
    `buyer_reference` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `instrument` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `fingerprint` VARCHAR(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `return_url_params` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `metadata` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `extra_info` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated` DATETIME NOT NULL,
    PRIMARY KEY (`payment_id`,`transaction_id`),
    UNIQUE KEY `unique_gateway_intent` (`gateway`, `gateway_intent_id`),
    KEY `PAYMENT_INTENT_GATEWAY_IDX` (`gateway`),
    KEY `PAYMENT_INTENT_STATUS_IDX` (`status`),
    KEY `PAYMENT_INTENT_CREATED_IDX` (`created`),
    CONSTRAINT `fk_payment_intent_transaction`
        FOREIGN KEY (`transaction_id`) REFERENCES `transaction`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;

# Crear tabla payment

CREATE TABLE `payment` (
    `id` BIGINT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `gateway` ENUM('STRIPE', 'STRIPE_MOBILE') NOT NULL,
    `gateway_payment_id` VARCHAR(255) NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `net_amount` DECIMAL(10, 2) NOT NULL,
    `currency` VARCHAR(3) NOT NULL,
    `receipt_url` VARCHAR(2000) NULL,
    `instrument` VARCHAR(255) NULL,
    `refunded` TINYINT(1) NOT NULL DEFAULT 0,
    `refunded_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `created` DATETIME NOT NULL,
    `updated` DATETIME NOT NULL,
    `metadata` TEXT NULL,
    PRIMARY KEY (`id`,`transaction_id`),
    UNIQUE KEY `unique_transaction_id` (`transaction_id`),
    UNIQUE KEY `unique_gateway_payment` (`gateway`, `gateway_payment_id`),
    KEY `PAYMENT_GATEWAY_IDX` (`gateway`),
    KEY `PAYMENT_CREATED_IDX` (`created`),
    CONSTRAINT `fk_payment_transaction`
       FOREIGN KEY (`transaction_id`) REFERENCES `transaction`(`id`)
           ON DELETE RESTRICT
) CHARSET=utf8mb4;