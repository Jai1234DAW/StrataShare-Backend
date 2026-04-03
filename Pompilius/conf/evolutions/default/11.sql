# Creacion de la tabla study_sample

# ---!Ups
CREATE TABLE `study_sample` (
    `study_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL,
    `deleted` TINYINT(1) NOT NULL DEFAULT '0',

    PRIMARY KEY (`study_id`, `sample_id`),
    CONSTRAINT `fk_study_sample_study`
        FOREIGN KEY (`study_id`) REFERENCES `study`(`id`)
            ON DELETE RESTRICT,
    CONSTRAINT `fk_study_sample_sample`
        FOREIGN KEY (`sample_id`) REFERENCES `sample`(`id`)
            ON DELETE RESTRICT
) CHARSET=utf8mb4;
