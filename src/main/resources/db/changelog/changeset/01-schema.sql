--liquibase formatted sql

--changeset DaniilYehorov::20260312-01
CREATE TABLE IF NOT EXISTS `user` (
    `chat_id` BIGINT NOT NULL,
    `payed_acc` TINYINT(1) NOT NULL DEFAULT 0,
    `language` ENUM('English', 'Ukrainian', 'Russian', 'German') NOT NULL,
    PRIMARY KEY (`chat_id`),
    UNIQUE INDEX `chatId_UNIQUE` (`chat_id` ASC) VISIBLE
    ) ENGINE = InnoDB;

--changeset DaniilYehorov::20260312-02
CREATE TABLE IF NOT EXISTS `user_physical_data` (
    `UserID` BIGINT NOT NULL,
    `age` INT NULL,
    `sex` ENUM('MALE', 'FEMALE') NULL,
    `body_fat_percent` DOUBLE NULL,
    `height` DOUBLE NULL,
    `weight` DOUBLE NULL,
    `maingoal` ENUM('WEIGHT_LOSS', 'Maintenance', 'WEIGHT_GAIN') NULL,
    `physical_activity_level` ENUM('SEDENTARY', 'LIGHT', 'MODERATE', 'ACTIVE', 'VERY_ACTIVE') NULL,
    `allergens` TEXT NULL,
    PRIMARY KEY (`UserID`),
    CONSTRAINT `fk_UserPhysicalData_User`
    FOREIGN KEY (`UserID`)
    REFERENCES `user` (`chat_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    ) ENGINE = InnoDB;