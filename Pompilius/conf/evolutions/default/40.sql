# Creacion de la tabla report

# --- !Ups
CREATE TABLE `report` (
    `id` BIGINT NOT NULL,
    `name` varchar(255) NOT NULL,
    `title` varchar(255) DEFAULT NULL,
    `authorized_users` text NOT NULL,
    `sheets` text NOT NULL,
    PRIMARY KEY (`id`)
  ) CHARSET=utf8mb4 ;

INSERT INTO `report` (
`id`, `name`, `title`, `authorized_users`, `sheets`
) VALUES
    (1, 'Reporte de ejemplo', 'Reporte de ejemplo', '[]', '[{
  "name": "Sheet1",
  "query": "select id,first_name from user where id = ?",
  "columns": [
    {
      "name": "id",
      "title": "User id",
      "dataType": "STRING",
      "bold": false,
      "highlight": false,
      "autoSize": false
    },
    {
      "name": "first_name",
      "title": "Name",
      "dataType": "STRING",
      "bold": false,
      "highlight": false,
      "autoSize": false
    }
  ],
  "parameters": [{
    "name": "uid",
    "dataType": "STRING",
 "description":"Identificador unico" }]
}]');