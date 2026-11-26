-- EDD-MPT：探索页管理菜单与权限（MKT-CMS-DSN-CR-003 §6.3 / D-017）
-- ============================================================
-- 背景：
--   MKT-CMS 探索页子域已交付管理接口 /api/mpt/explore/v1；
--   MPT UI 新增页面 src/views/cms/explore/index.vue（单页六页签，不新增三级菜单）。
--   本脚本在「营销管理」目录下新增二级页面「探索页管理」，并补齐按钮权限点。
-- 权限点：mkt:cms:explore:query / add / edit / publish / offline
--   页面承载 query；publish 覆盖立即/定时发布与取消定时；offline 覆盖立即下线。
-- 说明：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧权限集合渲染。
--       全部按 path、perms 定位，不依赖固定 menu_id；幂等可重跑。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 一级目录：营销管理（沿用 V14；不存在时兜底创建）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '营销管理', 0, 25, 'marketing', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'marketing');

SET @marketing_root = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 二级页面：探索页管理（C 类，组件 cms/explore/index）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '探索页管理', @marketing_root, 2, 'explore', 'cms/explore/index', NULL, '', 1, 0, 'C', '0', '0',
       'mkt:cms:explore:query', 'guide', 'admin', sysdate()
FROM DUAL
WHERE @marketing_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @marketing_root AND path = 'explore');

SET @explore_page = (SELECT menu_id FROM sys_menu
                     WHERE parent_id = @marketing_root AND path = 'explore'
                     ORDER BY menu_id LIMIT 1);

-- ------------------------------------------------------------
-- 3. 按钮权限点（与后端 @RequiresPermissions 一一对应）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '探索页新增', @explore_page, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:explore:add', '#',
       'admin', sysdate()
FROM DUAL WHERE @explore_page IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:explore:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '探索页修改', @explore_page, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:explore:edit', '#',
       'admin', sysdate()
FROM DUAL WHERE @explore_page IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:explore:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '探索页发布', @explore_page, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:explore:publish', '#',
       'admin', sysdate()
FROM DUAL WHERE @explore_page IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:explore:publish');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '探索页下线', @explore_page, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:explore:offline', '#',
       'admin', sysdate()
FROM DUAL WHERE @explore_page IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:explore:offline');

-- ------------------------------------------------------------
-- 4. 授权
-- 4.1 已拥有营销管理目录的角色自动获得探索页管理页面
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @explore_page
FROM sys_role_menu rm
WHERE @explore_page IS NOT NULL
  AND rm.menu_id = @marketing_root;

-- 4.2 页面可见 → 补齐上级目录
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @marketing_root
FROM sys_role_menu rm
WHERE @explore_page IS NOT NULL
  AND rm.menu_id = @explore_page;

-- 4.3 超级管理员（1）/超级用户（100）兜底授权页面与按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_id IN (1, 100)
  AND (m.menu_id = @explore_page
       OR m.perms LIKE 'mkt:cms:explore:%');

-- 4.4 拥有页面权限的角色获得全部探索页按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu m ON m.perms LIKE 'mkt:cms:explore:%' AND m.menu_type = 'F'
WHERE @explore_page IS NOT NULL
  AND rm.menu_id = @explore_page;
