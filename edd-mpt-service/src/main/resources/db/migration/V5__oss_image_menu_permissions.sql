-- EDD-OSS：对象存储管理菜单与权限（EDD-OSS-DSN-CR-001 §10.2）
-- ============================================================
-- 背景：
--   OSS 已交付 MPT 管理接口 /api/mpt/image/v1/objects（list/query/preview/remove），
--   MPT UI 新增页面 src/views/edd/oss/image/index.vue。
--   本脚本在「数字底座」菜单下新增「对象存储管理」页面，并补齐按钮权限点。
-- 权限点：oss:image:list / oss:image:query / oss:image:preview / oss:image:remove
-- ============================================================

-- ------------------------------------------------------------
-- 1. 定位「数字底座」菜单（按名称定位，回退到服务网关目录的父级、再回退到 2000）
-- ------------------------------------------------------------
SET @base_root = COALESCE(
    (SELECT menu_id FROM sys_menu WHERE menu_name = '数字底座' ORDER BY menu_id LIMIT 1),
    (SELECT parent_id FROM sys_menu WHERE path = 'sgw' ORDER BY menu_id LIMIT 1),
    (SELECT menu_id FROM sys_menu WHERE menu_id = 2000)
);

-- ------------------------------------------------------------
-- 2. 对象存储管理页面（组件 edd/oss/image/index）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '对象存储管理', @base_root, 60, 'ossImage', 'edd/oss/image/index', NULL, '', 1, 0, 'C', '0', '0',
       'oss:image:list', 'picture', 'admin', sysdate()
FROM DUAL
WHERE @base_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @base_root AND path = 'ossImage');
SET @oss_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @base_root AND path = 'ossImage' LIMIT 1);

-- ------------------------------------------------------------
-- 3. 按钮权限点（与后端 @RequiresPermissions 对齐）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '图片查询', @oss_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'oss:image:query', '#',
       'admin', sysdate()
FROM DUAL WHERE @oss_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'oss:image:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '图片预览', @oss_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'oss:image:preview', '#',
       'admin', sysdate()
FROM DUAL WHERE @oss_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'oss:image:preview');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '图片删除', @oss_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'oss:image:remove', '#',
       'admin', sysdate()
FROM DUAL WHERE @oss_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'oss:image:remove');

-- ------------------------------------------------------------
-- 4. 授予超级管理员与超级用户
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON (m.menu_id = @oss_menu OR m.perms LIKE 'oss:image:%')
WHERE @oss_menu IS NOT NULL
  AND r.role_id IN (1, 100);
