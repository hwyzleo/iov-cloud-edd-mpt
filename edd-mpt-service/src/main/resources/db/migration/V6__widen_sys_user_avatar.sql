-- EDD-MPT-DSN-CR-001 §3.1：头像改为存储 EDD-OSS 公开访问 URL，COS URL 约 110~120 字符，原 varchar(100) 不足
-- 仅前向演进：不改写 V1__init_schema.sql，不改动 sys_user 其他字段
ALTER TABLE `sys_user`
    MODIFY COLUMN `avatar` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '头像地址';
