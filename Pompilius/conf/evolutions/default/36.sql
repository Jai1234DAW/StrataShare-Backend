no ,e suma# Agregar campos platform_fee y gateway_fee a la tabla payment

# --- !Ups
ALTER TABLE `payment`
    ADD COLUMN `platform_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER `amount`,
    ADD COLUMN `gateway_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER `platform_fee`;

-- Actualizar registros existentes: calcular platform_fee desde transaction.fee y estimar gateway_fee
UPDATE `payment` p
INNER JOIN `transaction` t ON p.transaction_id = t.id
SET p.platform_fee = COALESCE(t.fee, 0.00),
    p.gateway_fee = (p.amount * 0.029) + 0.30;

-- Recalcular net_amount para registros existentes
-- net_amount = amount - platform_fee - gateway_fee
UPDATE `payment`
SET net_amount = amount - platform_fee - gateway_fee;

