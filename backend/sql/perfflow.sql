-- ===========================================================
-- PerfFlow 绩效考核系统 建库脚本 (MySQL 8.0+)
-- 字符集: utf8mb4 / 排序: utf8mb4_0900_ai_ci / 引擎: InnoDB
-- 密码: BCrypt(cost=10) of "12345678"
-- ===========================================================
CREATE DATABASE IF NOT EXISTS `perfflow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `perfflow`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===========================================================
-- 一、个人考核域（员工 → 部门领导 → 公司领导）
-- ===========================================================

-- ---------------- sys_department 部门表 ----------------
DROP TABLE IF EXISTS `sys_department`;
CREATE TABLE `sys_department` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(64)     NOT NULL,
  `parent_id`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `leader_user_id` BIGINT UNSIGNED NULL COMMENT '部门负责人用户ID（冗余）',
  `sort`           INT             NOT NULL DEFAULT 0,
  `remark`         VARCHAR(255)    NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ---------------- sys_user 用户表 ----------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`             VARCHAR(32)     NOT NULL,
  `password`             VARCHAR(100)    NOT NULL,
  `real_name`            VARCHAR(32)     NOT NULL,
  `role`                 VARCHAR(16)     NOT NULL COMMENT 'EMP/DEPT_LEAD/LEAD/PERFORMANCE_HR/ADMIN/DEPT_STAFF/OPERATION/COMMITTEE',
  `dept_id`              BIGINT UNSIGNED NULL,
  `dept_lead`            TINYINT(1)      NOT NULL DEFAULT 0,
  `email`                VARCHAR(64)     NULL,
  `phone`                VARCHAR(20)     NULL,
  `status`               TINYINT         NOT NULL DEFAULT 1,
  `last_login_at`        DATETIME        NULL,
  `must_change_password` TINYINT(1)      NOT NULL DEFAULT 0,
  `token_version`        INT             NOT NULL DEFAULT 0 COMMENT '登录令牌版本号（多设备互踢）',
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------- assessment_period 考核周期表 ----------------
DROP TABLE IF EXISTS `assessment_period`;
CREATE TABLE `assessment_period` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`                 VARCHAR(64)     NOT NULL,
  `period_type`          VARCHAR(20)     NOT NULL DEFAULT 'QUARTER_ASSESS' COMMENT '周期类型(表1-表7)：QUARTER_GOAL/QUARTER_ASSESS/ANNUAL_ASSESS/BONUS_APPLY/DEPT_YEAR_TASK/DEPT_QUARTER_ADJUST/DEPT_YEAR_SCORE',
  `year`                 INT             NOT NULL,
  `quarter`              TINYINT         NOT NULL COMMENT '季度类型1-4；年度类型为0',
  `start_date`           DATE            NOT NULL,
  `suspend_end_date`     DATE            NOT NULL COMMENT '员工自评截止日',
  `dept_review_end_date` DATE            NOT NULL COMMENT '部门审核截止日',
  `lead_score_end_date`  DATE            NOT NULL COMMENT '领导评分截止日',
  `auto_push_on_expire`  TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '挂起到期是否自动推送',
  `status`               TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束',
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_quarter_type` (`year`,`quarter`,`period_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期表';

-- ---------------- assessment_table 个人考核主表 ----------------
DROP TABLE IF EXISTS `assessment_table`;
CREATE TABLE `assessment_table` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `period_id`            BIGINT UNSIGNED NOT NULL,
  `user_id`              BIGINT UNSIGNED NOT NULL,
  `dept_id`              BIGINT UNSIGNED NULL COMMENT '部门ID（无部门被考核人可为空，如公司领导）',
  `position`             VARCHAR(50)     NULL COMMENT '岗位（被考核人填写）',
  `state`                VARCHAR(16)     NOT NULL DEFAULT 'SELF_DRAFTING' COMMENT 'SELF_DRAFTING/SELF_SUSPENDED/DEPT_REVIEW/LEAD_SCORING/FINISHED',
  `self_total_score`     DECIMAL(6,2)    NOT NULL DEFAULT 0.00,
  `leader_score`         DECIMAL(6,2)    NULL,
  `final_score`          DECIMAL(6,2)    NULL COMMENT '总分（中层含部门分权重）',
  `grade`                VARCHAR(2)      NULL COMMENT '考核等级 A/B/C/D（等级联动写入）',
  `dept_grade`           CHAR(1)         NULL COMMENT '部门等级（冗余）',
  `quota_grade_a`        INT             NULL COMMENT '本部门该层级A名额',
  `quota_grade_b`        INT             NULL,
  `quota_grade_c`        INT             NULL,
  `quota_grade_d`        INT             NULL,
  `dept_score_weight`    DECIMAL(5,2)    NULL COMMENT '权重快照-部门分',
  `personal_score_weight` DECIMAL(5,2)   NULL COMMENT '权重快照-个人分',
  `suspend_extended_days` INT            NOT NULL DEFAULT 0 COMMENT '挂起延长天数',
  `submitted_at`         DATETIME        NULL,
  `pushed_at`            DATETIME        NULL,
  `dept_approved_at`     DATETIME        NULL,
  `lead_finished_at`     DATETIME        NULL,
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_user` (`period_id`,`user_id`),
  KEY `idx_period_dept` (`period_id`,`dept_id`),
  KEY `idx_state` (`state`),
  KEY `idx_period_state` (`period_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人考核主表';

-- ---------------- assessment_row 考核行明细 ----------------
DROP TABLE IF EXISTS `assessment_row`;
CREATE TABLE `assessment_row` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `table_id`        BIGINT UNSIGNED NOT NULL,
  `category`        VARCHAR(16)     NOT NULL COMMENT 'PLAN(1-5)/OPEN(6-7)/BONUS(8-10)',
  `seq`             TINYINT         NOT NULL,
  `indicator_name`  VARCHAR(128)    NULL,
  `base_score`      DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '指标满分',
  `work_target`     TEXT            NULL,
  `score_criteria`  TEXT            NULL,
  `completion_rate` DECIMAL(5,2)    NULL COMMENT '完成率%',
  `self_score`      DECIMAL(6,2)    NULL COMMENT '自评得分=指标分×完成率',
  `adjusted_score`  DECIMAL(6,2)    NULL COMMENT '部门领导调整后得分',
  `adjust_remark`   VARCHAR(500)    NULL COMMENT '调整原因（DB可见，前端隐藏）',
  `leader_score`    DECIMAL(6,2)    NULL COMMENT '领导评分',
  `row_result`      VARCHAR(64)     NULL,
  `frozen`          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'BONUS行不可编辑',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_seq` (`table_id`,`seq`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人考核行明细';

-- ---------------- assessment_flow_log 流程日志 ----------------
DROP TABLE IF EXISTS `assessment_flow_log`;
CREATE TABLE `assessment_flow_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `table_id`      BIGINT UNSIGNED NOT NULL,
  `from_state`    VARCHAR(16)     NOT NULL,
  `to_state`      VARCHAR(16)     NOT NULL,
  `action`        VARCHAR(32)     NOT NULL COMMENT 'SUBMIT/PUSH/APPROVE/REJECT/LEAD_SCORE/EXTEND_SUSPEND/CREATE/AUTO_PUSH',
  `operator_id`   BIGINT UNSIGNED NULL,
  `operator_role` VARCHAR(16)     NULL,
  `comment`       VARCHAR(500)    NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人考核流程日志';

-- ===========================================================
-- 二、部门考核域（专员 → 负责人 → 运营 → 委员会）
-- ===========================================================

-- ---------------- dept_assessment 部门考核主表 ----------------
DROP TABLE IF EXISTS `dept_assessment`;
CREATE TABLE `dept_assessment` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `period_id`        BIGINT UNSIGNED NOT NULL,
  `dept_id`          BIGINT UNSIGNED NOT NULL,
  `kpi_score`        DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '经营业绩得分',
  `operation_score`  DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '运营指标得分',
  `key_work_score`   DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '重点工作得分',
  `bonus_score`      DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '加减分',
  `total_score`      DECIMAL(10,2)   NOT NULL DEFAULT 0,
  `dept_grade`       CHAR(1)         NULL COMMENT '部门等级 A/B/C/D',
  `status`           TINYINT         NOT NULL DEFAULT 0 COMMENT '0未开始/1填报中/2待复核/3待初审/4待审批/5已完成',
  `submitted_at`     DATETIME        NULL,
  `reviewed_at`      DATETIME        NULL,
  `approved_at`      DATETIME        NULL,
  `version`          INT             NOT NULL DEFAULT 0 COMMENT '调整版本号（乐观锁）',
  `adjust_reason`    VARCHAR(500)    NULL,
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_dept` (`period_id`,`dept_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门考核主表';

-- ---------------- dept_kpi_row 部门KPI明细行 ----------------
DROP TABLE IF EXISTS `dept_kpi_row`;
CREATE TABLE `dept_kpi_row` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_assessment_id` BIGINT UNSIGNED NOT NULL,
  `row_type`           VARCHAR(20)     NOT NULL COMMENT 'KPI经营业绩/OPERATION运营指标/KEY_WORK重点工作',
  `seq_no`             INT             NOT NULL,
  `indicator_name`     VARCHAR(200)    NULL,
  `target_value`       VARCHAR(200)    NULL,
  `actual_value`       VARCHAR(200)    NULL,
  `scoring_standard`   VARCHAR(500)    NULL,
  `score`              DECIMAL(10,2)   NULL,
  `weight`             DECIMAL(5,2)    NULL,
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_assessment_id` (`dept_assessment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门KPI明细行';

-- ---------------- grade_quota_config 等级名额配置 ----------------
DROP TABLE IF EXISTS `grade_quota_config`;
CREATE TABLE `grade_quota_config` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_grade`    CHAR(1)         NOT NULL COMMENT '部门等级 A/B/C/D',
  `staff_level`   VARCHAR(20)     NOT NULL COMMENT '员工层级 MIDDLE/BASIC',
  `grade_a_ratio` DECIMAL(5,2)    NOT NULL DEFAULT 0,
  `grade_b_ratio` DECIMAL(5,2)    NOT NULL DEFAULT 0,
  `grade_c_ratio` DECIMAL(5,2)    NOT NULL DEFAULT 0,
  `grade_d_ratio` DECIMAL(5,2)    NOT NULL DEFAULT 0,
  `is_default`    TINYINT(1)      NOT NULL DEFAULT 0,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_grade_level` (`dept_grade`,`staff_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级名额配置';

-- ---------------- weight_config 权重配置 ----------------
DROP TABLE IF EXISTS `weight_config`;
CREATE TABLE `weight_config` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_level`     VARCHAR(20)     NOT NULL COMMENT '员工层级 MIDDLE/BASIC',
  `dept_weight`     DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '部门分权重%',
  `personal_weight` DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '个人分权重%',
  `period_id`       BIGINT UNSIGNED NULL COMMENT 'NULL=全局默认，非空=周期覆盖',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权重配置';

-- ===========================================================
-- 三、支撑域（通知 / 审计）
-- ===========================================================

-- ---------------- adjust_log 调整审计日志 ----------------
DROP TABLE IF EXISTS `adjust_log`;
CREATE TABLE `adjust_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `target_type`   VARCHAR(20)     NOT NULL COMMENT 'DEPT部门考核/PERSONAL个人考核',
  `target_id`     BIGINT UNSIGNED NOT NULL,
  `field_name`    VARCHAR(50)     NOT NULL,
  `before_value`  VARCHAR(500)    NULL,
  `after_value`   VARCHAR(500)    NULL,
  `operator_id`   BIGINT UNSIGNED NULL,
  `adjust_reason` VARCHAR(500)    NULL,
  `is_visible`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否对前台可见（默认0不可见）',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调整审计日志';

-- ---------------- notification 站内通知 ----------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `target_user_id` BIGINT UNSIGNED NOT NULL,
  `title`          VARCHAR(100)    NOT NULL,
  `content`        VARCHAR(500)    NULL,
  `type`           VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/REMIND/OVERDUE/REJECT',
  `read_flag`      TINYINT(1)      NOT NULL DEFAULT 0,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target_user` (`target_user_id`,`read_flag`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================================
-- 种子数据
-- BCrypt("12345678", cost=10) =
--   $2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6
-- ===========================================================

-- 部门（1技术部/2产品部/3人事部）
INSERT INTO `sys_department`(`id`,`name`,`parent_id`,`sort`) VALUES
  (1,'技术部', 0, 1),
  (2,'产品部', 0, 2),
  (3,'人事部', 0, 3);

-- 用户：ADMIN/HR/LEAD 无部门、不参与考核；其余按角色归属部门
-- 部门负责人：bumen1(技术部)/bumen2(产品部)；公司领导：leader
INSERT INTO `sys_user`
  (`id`,`username`,`password`,`real_name`,`role`,`dept_id`,`dept_lead`,`status`,`must_change_password`) VALUES
  (1,'admin',    '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','系统管理员','ADMIN',          NULL, 0, 1, 1),
  (2,'hr',       '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','人事小李',   'PERFORMANCE_HR', NULL, 0, 1, 1),
  (3,'leader',   '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','王总监',    'LEAD',           NULL, 0, 1, 1),
  (6,'emp01',    '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','赵一',      'EMP',            1,    0, 1, 1),
  (7,'emp02',    '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','钱二',      'EMP',            1,    0, 1, 1),
  (8,'emp03',    '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','孙三',      'EMP',            2,    0, 1, 1),
  (9,'wanggong', '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','王工',      'EMP',            1,    0, 1, 1),
  (11,'bumen1',  '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','张经理',    'DEPT_LEAD',      1,    1, 1, 1),
  (12,'bumen2',  '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','黄经理',    'DEPT_LEAD',      2,    1, 1, 1),
  (13,'deptstaff','$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','刘专员',   'DEPT_STAFF',     1,    0, 1, 1),
  (14,'yunying', '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','周运营',    'OPERATION',      3,    0, 1, 1),
  (15,'weiyuan', '$2a$10$A/AjsVrRlMlr/EPFwiIBrORuNYUDrNMuPyu86JkJzwanumoBCyRV6','吴委员',    'COMMITTEE',      3,    0, 1, 1);

-- 考核周期/个人考核/部门考核表由业务创建（周期通过 HR 发布，主表与模板行随周期开启自动生成），建库不预置数据

-- 默认等级配额：部门等级(A/B/C/D) × 员工层级(MIDDLE/BASIC) → A/B/C/D 比例
INSERT INTO `grade_quota_config`(`dept_grade`,`staff_level`,`grade_a_ratio`,`grade_b_ratio`,`grade_c_ratio`,`grade_d_ratio`,`is_default`) VALUES
  ('A','MIDDLE',20,40,30,10,1),('A','BASIC',15,35,35,15,1),
  ('B','MIDDLE',15,35,35,15,1),('B','BASIC',10,30,40,20,1),
  ('C','MIDDLE',10,30,40,20,1),('C','BASIC',5,25,45,25,1),
  ('D','MIDDLE',5,20,45,30,1),('D','BASIC',0,15,50,35,1);

-- 默认权重：部门分 × 权重 + 个人分 × 权重（period_id=NULL 为全局默认）
INSERT INTO `weight_config`(`staff_level`,`dept_weight`,`personal_weight`,`period_id`) VALUES
  ('MIDDLE',50,50,NULL),('BASIC',30,70,NULL);
