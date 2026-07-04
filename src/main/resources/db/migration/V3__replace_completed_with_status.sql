-- Replaces the boolean `completed` flag with a four-state lifecycle status:
-- PLANNED / IN_PROGRESS / COMPLETED / DROPPED.
-- Existing data: completed = 1 -> COMPLETED, completed = 0 -> PLANNED.

ALTER TABLE `games` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'PLANNED';
UPDATE `games` SET `status` = 'COMPLETED' WHERE `completed` = 1;
ALTER TABLE `games` DROP COLUMN `completed`;

ALTER TABLE `movies` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'PLANNED';
UPDATE `movies` SET `status` = 'COMPLETED' WHERE `completed` = 1;
ALTER TABLE `movies` DROP COLUMN `completed`;

ALTER TABLE `books` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'PLANNED';
UPDATE `books` SET `status` = 'COMPLETED' WHERE `completed` = 1;
ALTER TABLE `books` DROP COLUMN `completed`;

ALTER TABLE `shows` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'PLANNED';
UPDATE `shows` SET `status` = 'COMPLETED' WHERE `completed` = 1;
ALTER TABLE `shows` DROP COLUMN `completed`;
