-- EDD-MPT-DSN-CR-002 §11.2：账号与 ORG 员工一对一绑定
-- employee_id 为 NULL 表示未绑定（纯管理员账号）；唯一键保证一个员工最多一个账号。
ALTER TABLE `sys_user`
    ADD COLUMN `employee_id` BIGINT DEFAULT NULL COMMENT '关联ORG员工ID' AFTER `dept_id`,
    ADD COLUMN `pwd_reset_required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必须改密：0-否，1-是';

CREATE UNIQUE INDEX `uk_employee_id` ON `sys_user` (`employee_id`);

CREATE INDEX `idx_user_employee` ON `sys_user` (`employee_id`, `del_flag`);
