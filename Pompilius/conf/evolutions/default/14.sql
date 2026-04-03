# Refactorización: Attachments 1:N con Resource

# --- !Ups
DROP TABLE IF EXISTS `resource_attachment`;

ALTER TABLE `attachment`
    ADD COLUMN `resource_id` BIGINT NULL AFTER `id`,
    ADD INDEX `RESOURCE_ID_IDX` (`resource_id`),
    ADD CONSTRAINT `fk_attachment_resource`
        FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`)
        ON DELETE RESTRICT;