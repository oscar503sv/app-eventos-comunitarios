-- DropForeignKey
ALTER TABLE `event_attendances` DROP FOREIGN KEY `event_attendances_eventId_fkey`;

-- DropForeignKey
ALTER TABLE `reviews` DROP FOREIGN KEY `reviews_eventId_fkey`;

-- DropIndex
DROP INDEX `event_attendances_eventId_fkey` ON `event_attendances`;

-- DropIndex
DROP INDEX `reviews_eventId_fkey` ON `reviews`;

-- AlterTable
ALTER TABLE `events` ADD COLUMN `category` ENUM('CULTURA', 'MUSICA', 'DEPORTE', 'EDUCACION', 'GASTRONOMIA', 'SALUD', 'OTRO') NOT NULL DEFAULT 'OTRO';

-- AddForeignKey
ALTER TABLE `event_attendances` ADD CONSTRAINT `event_attendances_eventId_fkey` FOREIGN KEY (`eventId`) REFERENCES `events`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `reviews` ADD CONSTRAINT `reviews_eventId_fkey` FOREIGN KEY (`eventId`) REFERENCES `events`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;
