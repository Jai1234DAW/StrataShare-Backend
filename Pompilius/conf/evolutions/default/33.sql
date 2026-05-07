# Transacciones

# --- !Ups

-- TRANSACCIONES, BARTERS Y PAYMENTS

-- Barter 1: PENDING - Laura quiere pumice de María, ofrece garnet
INSERT INTO `transaction` (
    `id`, `transaction_type`, `transaction_status`, `seller_id`, `buyer_id`,
    `resource_id`, `fee`, `created`, `updated`, `metadata`
) VALUES
    (835125411154235001, 'BARTER', 'PENDING', 835125411154231297, 835125411154231302,
     835125411154232009, 0, NOW(), NOW(), '{"offeredResourceId": "835125411154232017"}');

INSERT INTO `barter` (`barter_id`, `transaction_id`, `offered_resource_id`) VALUES
    (835125411154236001, 835125411154235001, 835125411154232017);

-- Barter 2: COMPLETED - Pedro obtuvo quartzite de John por calcite
INSERT INTO `transaction` (
    `id`, `transaction_type`, `transaction_status`, `seller_id`, `buyer_id`,
    `resource_id`, `fee`, `created`, `updated`, `completed_successfully_at`, `metadata`
) VALUES
    (835125411154235002, 'BARTER', 'COMPLETED', 835125411154231299, 835125411154231301,
     835125411154232012, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), NOW(), '{"offeredResourceId": "835125411154232015"}');

INSERT INTO `barter` (`barter_id`, `transaction_id`, `offered_resource_id`) VALUES
    (835125411154236002, 835125411154235002, 835125411154232015);

INSERT INTO `resource_user` (`resource_id`, `user_id`, `resource_user_type`, `created`, `deleted`) VALUES
   (835125411154232012, 835125411154231301, 'ACCEPTED_AS_PAYMENT', DATE_SUB(NOW(), INTERVAL 5 DAY), 0),
   (835125411154232015, 835125411154231299, 'ACCEPTED_AS_PAYMENT', DATE_SUB(NOW(), INTERVAL 5 DAY), 0);

-- Barter 3: REJECTED - Carlos quiso eclogite de John
INSERT INTO `transaction` (
    `id`, `transaction_type`, `transaction_status`, `seller_id`, `buyer_id`,
    `resource_id`, `fee`, `created`, `updated`, `cancelled_rejected_at`, `metadata`
) VALUES
    (835125411154235003, 'BARTER', 'REJECTED', 835125411154231299, 835125411154231298,
     835125411154232011, 0, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), '{"offeredResourceId": "835125411154232010"}');

INSERT INTO `barter` (`barter_id`, `transaction_id`, `offered_resource_id`) VALUES
    (835125411154236003, 835125411154235003, 835125411154232010);
