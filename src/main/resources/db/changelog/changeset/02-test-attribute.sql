--liquibase formatted sql

--changeset DaniilYehorov:20260312-03
ALTER TABLE user ADD COLUMN test_attribute VARCHAR(255) NULL;

--changeset DaniilYehorov:20260312-04
ALTER TABLE `user` DROP COLUMN test_attribute;