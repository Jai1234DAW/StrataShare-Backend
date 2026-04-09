# Crear tabla transaction (base para todas las transacciones)

# --- !Ups
CREATE TABLE `transaction` (
    `id` BIGINT NOT NULL,
    `transaction_type` ENUM('PAYMENT', 'BARTER') NOT NULL,
    `transaction_status` ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REJECTED') NOT NULL,
    `seller_id` BIGINT NOT NULL,
    `buyer_id` BIGINT NOT NULL,
    `gateway` ENUM('STRIPE', 'STRIPE_MOBILE', 'BARTER') NOT NULL,
    `gateway_payment_id` VARCHAR(255) NOT NULL,
    `receipt_url` TEXT NULL,
    `resource_id` BIGINT NOT NULL,
    `fee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `created` DATETIME NOT NULL,
    `updated` DATETIME NOT NULL,
    `metadata` TEXT NULL,
    `voucher` TINYINT(1) NOT NULL DEFAULT 0,
    `completed_at` DATETIME NULL,
    `cancelled_at` DATETIME NULL,

    PRIMARY KEY (`id`),
    KEY `TRANSACTION_TYPE_IDX` (`transaction_type`),
    KEY `TRANSACTION_STATUS_IDX` (`transaction_status`),
    KEY `TRANSACTION_GATEWAY_IDX` (`gateway`),
    KEY `TRANSACTION_RESOURCE_IDX` (`resource_id`),
    KEY `TRANSACTION_SELLER_IDX` (`seller_id`),
    KEY `TRANSACTION_BUYER_IDX` (`buyer_id`),
    KEY `TRANSACTION_CREATED_IDX` (`created`),
    UNIQUE KEY `unique_gateway_payment_id` (`gateway_payment_id`),

    CONSTRAINT `fk_transaction_seller`
        FOREIGN KEY (`seller_id`) REFERENCES `users`(`id`)
        ON DELETE RESTRICT,

    CONSTRAINT `fk_transaction_buyer`
        FOREIGN KEY (`buyer_id`) REFERENCES `users`(`id`)
        ON DELETE RESTRICT,

    CONSTRAINT `fk_transaction_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT
) CHARSET=utf8mb4;

