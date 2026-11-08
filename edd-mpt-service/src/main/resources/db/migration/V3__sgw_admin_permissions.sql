-- EDD-SGW-DSN-CR-001：服务网关管理权限与菜单
-- SGW 为配置唯一属主，MPT 仅保留权限校验、操作审计与转发。

-- 1. 修正历史权限前缀
UPDATE sys_menu SET perms = REPLACE(perms, 'tsp:sgw:', 'edd:sgw:') WHERE perms LIKE 'tsp:sgw:%';

-- 2. 移除不再存在的导入能力
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms = 'edd:sgw:route:import';
DELETE FROM sys_menu WHERE perms = 'edd:sgw:route:import';

-- 3. 定位服务网关根菜单
SET @sgw_root = (SELECT menu_id FROM sys_menu WHERE path = 'sgw' AND parent_id = 2000 LIMIT 1);
SET @route_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @sgw_root AND path = 'route' LIMIT 1);

-- 4. 路由管理：补充上游可选项查询按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '上游选项', @route_menu, 7, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:upstream:list', '#',
       'admin', sysdate()
FROM DUAL
WHERE @route_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:upstream:list');

-- 5. 策略管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略管理', @sgw_root, 20, 'policies', 'edd/sgw/policy/index', NULL, '', 1, 0, 'C', '0', '0',
       'edd:sgw:policy:list', 'shopping', 'admin', sysdate()
FROM DUAL
WHERE @sgw_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @sgw_root AND path = 'policies');
SET @policy_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @sgw_root AND path = 'policies' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略查询', @policy_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:policy:query', '#',
       'admin', sysdate()
FROM DUAL WHERE @policy_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:policy:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略新增', @policy_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:policy:add', '#',
       'admin', sysdate()
FROM DUAL WHERE @policy_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:policy:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略修改', @policy_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:policy:edit', '#',
       'admin', sysdate()
FROM DUAL WHERE @policy_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:policy:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略删除', @policy_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:policy:remove', '#',
       'admin', sysdate()
FROM DUAL WHERE @policy_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:policy:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '策略导出', @policy_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:policy:export', '#',
       'admin', sysdate()
FROM DUAL WHERE @policy_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:policy:export');

-- 6. 版本管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '版本管理', @sgw_root, 30, 'versions', 'edd/sgw/version/index', NULL, '', 1, 0, 'C', '0', '0',
       'edd:sgw:config:list', 'tree', 'admin', sysdate()
FROM DUAL
WHERE @sgw_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @sgw_root AND path = 'versions');
SET @version_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @sgw_root AND path = 'versions' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '版本查询', @version_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:config:query', '#',
       'admin', sysdate()
FROM DUAL WHERE @version_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:config:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '配置发布', @version_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:config:publish', '#',
       'admin', sysdate()
FROM DUAL WHERE @version_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:config:publish');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '配置回滚', @version_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:config:rollback', '#',
       'admin', sysdate()
FROM DUAL WHERE @version_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:config:rollback');

-- 7. 发布记录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '发布记录', @sgw_root, 40, 'publications', 'edd/sgw/publication/index', NULL, '', 1, 0, 'C', '0', '0',
       'edd:sgw:publication:query', 'list', 'admin', sysdate()
FROM DUAL
WHERE @sgw_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @sgw_root AND path = 'publications');
SET @publication_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @sgw_root AND path = 'publications' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '发布重试', @publication_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'edd:sgw:publication:retry', '#',
       'admin', sysdate()
FROM DUAL WHERE @publication_menu IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'edd:sgw:publication:retry');

-- 8. 运行时
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运行时', @sgw_root, 50, 'runtime', 'edd/sgw/runtime/index', NULL, '', 1, 0, 'C', '0', '0',
       'edd:sgw:runtime:list', 'monitor', 'admin', sysdate()
FROM DUAL
WHERE @sgw_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @sgw_root AND path = 'runtime');

-- 9. 授予超级管理员与超级用户
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON (m.menu_id = @sgw_root OR m.perms LIKE 'edd:sgw:%')
WHERE @sgw_root IS NOT NULL
  AND r.role_id IN (1, 100);
