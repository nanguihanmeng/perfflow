-- ===========================================================
-- PerfFlow 建库脚本 (MySQL 8.0+)
-- 字符集: utf8mb4 / 排序: utf8mb4_0900_ai_ci / 引擎: InnoDB
-- 密码: BCrypt(cost=10) of "Init@123456"
-- ===========================================================
CREATE DATABASE IF NOT EXISTS `perfflow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `perfflow`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------- sys_department ----------------
DROP TABLE IF EXISTS `sys_department`;
CREATE TABLE `sys_department` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(64)     NOT NULL,
  `parent_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `leader_user_id` BIGINT UNSIGNED NULL,
  `sort`         INT             NOT NULL DEFAULT 0,
  `remark`       VARCHAR(255)    NULL,
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ---------------- sys_user ----------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(32)     NOT NULL,
  `password`        VARCHAR(100)    NOT NULL,
  `real_name`       VARCHAR(32)     NOT NULL,
  `role`            VARCHAR(16)     NOT NULL,
  `dept_id`         BIGINT UNSIGNED NULL,
  `dept_lead`       TINYINT(1)      NOT NULL DEFAULT 0,
  `email`           VARCHAR(64)     NULL,
  `phone`           VARCHAR(20)     NULL,
  `status`          TINYINT         NOT NULL DEFAULT 1,
  `last_login_at`   DATETIME        NULL,
  `must_change_password` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------- assessment_period ----------------
DROP TABLE IF EXISTS `assessment_period`;
CREATE TABLE `assessment_period` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`                   VARCHAR(64)     NOT NULL,
  `year`                   INT             NOT NULL,
  `quarter`                TINYINT         NOT NULL,
  `start_date`             DATE            NOT NULL,
  `suspend_end_date`       DATE            NOT NULL,
  `dept_review_end_date`   DATE            NOT NULL,
  `lead_score_end_date`    DATE            NOT NULL,
  `auto_push_on_expire`    TINYINT(1)      NOT NULL DEFAULT 1,
  `status`                 TINYINT         NOT NULL DEFAULT 0,
  `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_quarter` (`year`,`quarter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期表';

-- ---------------- assessment_table ----------------
DROP TABLE IF EXISTS `assessment_table`;
CREATE TABLE `assessment_table` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `period_id`              BIGINT UNSIGNED NOT NULL,
  `user_id`                BIGINT UNSIGNED NOT NULL,
  `dept_id`                BIGINT UNSIGNED NOT NULL,
  `position`               VARCHAR(50)     NULL COMMENT '岗位（被考核人填写）',
  `state`                  VARCHAR(16)     NOT NULL DEFAULT 'SELF_DRAFTING',
  `self_total_score`       DECIMAL(6,2)    NOT NULL DEFAULT 0.00,
  `leader_score`           DECIMAL(6,2)    NULL,
  `final_score`            DECIMAL(6,2)    NULL,
  `grade`                  VARCHAR(2)      NULL,
  `suspend_extended_days`  INT             NOT NULL DEFAULT 0,
  `submitted_at`           DATETIME        NULL,
  `pushed_at`              DATETIME        NULL,
  `dept_approved_at`       DATETIME        NULL,
  `lead_finished_at`       DATETIME        NULL,
  `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_user` (`period_id`,`user_id`),
  KEY `idx_period_dept` (`period_id`,`dept_id`),
  KEY `idx_state` (`state`),
  KEY `idx_period_state` (`period_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核主表';

-- ---------------- assessment_row ----------------
DROP TABLE IF EXISTS `assessment_row`;
CREATE TABLE `assessment_row` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `table_id`          BIGINT UNSIGNED NOT NULL,
  `category`          VARCHAR(16)     NOT NULL,
  `seq`               TINYINT         NOT NULL,
  `indicator_name`    VARCHAR(128)    NULL,
  `base_score`        DECIMAL(5,2)    NOT NULL DEFAULT 0,
  `work_target`       TEXT            NULL,
  `score_criteria`    TEXT            NULL,
  `completion_rate`   DECIMAL(5,2)    NULL,
  `self_score`        DECIMAL(6,2)    NULL,
  `adjusted_score`    DECIMAL(6,2)    NULL,
  `adjust_remark`     VARCHAR(500)    NULL,
  `leader_score`      DECIMAL(6,2)    NULL,
  `row_result`        VARCHAR(64)     NULL,
  `frozen`            TINYINT(1)      NOT NULL DEFAULT 0,
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_seq` (`table_id`,`seq`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核行明细';

-- ---------------- assessment_flow_log ----------------
DROP TABLE IF EXISTS `assessment_flow_log`;
CREATE TABLE `assessment_flow_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `table_id`       BIGINT UNSIGNED NOT NULL,
  `from_state`     VARCHAR(16)     NOT NULL,
  `to_state`       VARCHAR(16)     NOT NULL,
  `action`         VARCHAR(32)     NOT NULL,
  `operator_id`    BIGINT UNSIGNED NULL,
  `operator_role`  VARCHAR(16)     NULL,
  `comment`        VARCHAR(500)    NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程日志';

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================================
-- 种子数据
-- BCrypt("Init@123456", cost=10) =
--   $2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC
-- ===========================================================
INSERT INTO `sys_department`(`id`,`name`,`parent_id`,`sort`) VALUES
  (1,'技术部', 0, 1),
  (2,'产品部', 0, 2),
  (3,'人事部', 0, 3);

-- admin / hr / leader / 部门负责人 / 员工（HR 与 ADMIN 无部门、不参与考核）
INSERT INTO `sys_user`(`id`,`username`,`password`,`real_name`,`role`,`dept_id`,`dept_lead`,`status`,`must_change_password`) VALUES
  (1,'admin',     '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','系统管理员','ADMIN',     NULL, 0, 1, 1),
  (2,'hr',        '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','人事小李',  'HR',        NULL, 0, 1, 1),
  (3,'leader',    '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','王总监',   'LEAD',       NULL, 0, 1, 1),
  (6,'emp01',     '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','赵一',     'EMP',        1,    0, 1, 1),
  (7,'emp02',     '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','钱二',     'EMP',        1,    0, 1, 1),
  (8,'emp03',     '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','孙三',     'EMP',        2,    0, 1, 1),
  (9,'wanggong',  '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','王工',     'EMP',        1,    0, 1, 1),
  (11,'bumen1',   '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','张经理',   'DEPT_LEAD',  1,    1, 1, 1),
  (12,'bumen2',   '$2a$10$nP/hgmM0jVYi89G/cCItmuSxhDLw90tCw8NFs3lXBi7FpgUYtLmhC','黄经理',   'DEPT_LEAD',  2,    1, 1, 1);

-- 2026Q3 周期：start_date=2026-07-01, suspend_end=2026-09-25, dept_review=2026-10-10, lead_score=2026-10-25
INSERT INTO `assessment_period`(`id`,`name`,`year`,`quarter`,`start_date`,`suspend_end_date`,`dept_review_end_date`,`lead_score_end_date`,`auto_push_on_expire`,`status`)
VALUES (1, '2026Q3', 2026, 3, '2026-07-01', '2026-09-25', '2026-10-10', '2026-10-25', 1, 1);

-- 主表：仅参与考核的员工（HR/ADMIN/部门负责人不参与）
INSERT INTO `assessment_table`(`id`,`period_id`,`user_id`,`dept_id`,`state`,`self_total_score`,`suspend_extended_days`) VALUES
  (1, 1, 6, 1, 'SELF_DRAFTING', 0, 0),
  (2, 1, 7, 1, 'SELF_DRAFTING', 0, 0),
  (3, 1, 8, 2, 'SELF_DRAFTING', 0, 0);

-- 模板行：每张主表 10 行（指标分数默认 0，由 HR 导入时填写）
-- Emp01 (table=1)
INSERT INTO `assessment_row`(`table_id`,`category`,`seq`,`base_score`,`frozen`) VALUES
  (1, 'PLAN',  1, 0, 0),(1, 'PLAN',  2, 0, 0),(1, 'PLAN',  3, 0, 0),(1, 'PLAN',  4, 0, 0),(1, 'PLAN',  5, 0, 0),
  (1, 'OPEN',  6, 0, 0),(1, 'OPEN',  7, 0, 0),
  (1, 'BONUS', 8, 0, 1),(1, 'BONUS', 9, 0, 1),(1, 'BONUS',10, 0, 1);
INSERT INTO `assessment_row`(`table_id`,`category`,`seq`,`base_score`,`frozen`) VALUES
  (2, 'PLAN',  1, 0, 0),(2, 'PLAN',  2, 0, 0),(2, 'PLAN',  3, 0, 0),(2, 'PLAN',  4, 0, 0),(2, 'PLAN',  5, 0, 0),
  (2, 'OPEN',  6, 0, 0),(2, 'OPEN',  7, 0, 0),
  (2, 'BONUS', 8, 0, 1),(2, 'BONUS', 9, 0, 1),(2, 'BONUS',10, 0, 1);
INSERT INTO `assessment_row`(`table_id`,`category`,`seq`,`base_score`,`frozen`) VALUES
  (3, 'PLAN',  1, 0, 0),(3, 'PLAN',  2, 0, 0),(3, 'PLAN',  3, 0, 0),(3, 'PLAN',  4, 0, 0),(3, 'PLAN',  5, 0, 0),
  (3, 'OPEN',  6, 0, 0),(3, 'OPEN',  7, 0, 0),
  (3, 'BONUS', 8, 0, 1),(3, 'BONUS', 9, 0, 1),(3, 'BONUS',10, 0, 1);
