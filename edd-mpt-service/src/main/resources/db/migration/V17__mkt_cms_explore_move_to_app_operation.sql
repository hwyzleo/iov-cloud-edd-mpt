-- EDD-MPT：探索页管理迁移至「App运营」一级目录
-- ============================================================
-- 背景：探索页、后续服务页属于 App 页面运营，不应挂在「营销管理」下。
-- 本脚本：
--   1) 新增一级目录「App运营」（M，path=app-operation）；
--   2) 将既有「探索页管理」页面移动到该目录，保留 menu_id、组件和权限标识；
--   3) 已拥有探索页页面或按钮的角色补齐「App运营」目录授权，不扩大页面与按钮授权范围；
--   4) 不修改 V16 历史迁移，不调整探索页按钮父级，不回收「营销管理」既有授权。
-- 最终菜单层级：App运营 / 探索页管理，路由 /app-operation/explore。
-- 全部按 path、perms 定位，不依赖固定 menu_id；幂等可重跑。
-- 说明：菜单变更后需重新登录或清理登录缓存，前端才会按新菜单树渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 一级目录：App运营
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 'App运营', 0, 26, 'app-operation', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'app-operation');

SET @app_operation_root = (SELECT menu_id FROM sys_menu
                           WHERE parent_id = 0 AND path = 'app-operation'
                           ORDER BY menu_id LIMIT 1);

-- ------------------------------------------------------------
-- 2. 探索页管理：营销管理 → App运营
--    通过页面查询权限定位，避免重复执行时受旧 parent_id 影响
-- ------------------------------------------------------------
SET @explore_page = (SELECT menu_id FROM sys_menu
                     WHERE perms = 'mkt:cms:explore:query' AND menu_type = 'C'
                     ORDER BY menu_id LIMIT 1);

UPDATE sys_menu
SET parent_id   = @app_operation_root,
    order_num   = 1,
    update_by   = 'admin',
    update_time = sysdate()
WHERE menu_id = @explore_page
  AND @app_operation_root IS NOT NULL;

-- ------------------------------------------------------------
-- 3. 授权：仅补齐新上级目录，不改变探索页页面及按钮授权
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @app_operation_root
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @app_operation_root IS NOT NULL
  AND (m.menu_id = @explore_page OR m.perms LIKE 'mkt:cms:explore:%');

-- 超级管理员（1）/超级用户（100）兜底授权新目录
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @app_operation_root
FROM sys_role r
WHERE r.role_id IN (1, 100)
  AND @app_operation_root IS NOT NULL;

-- ============================================================
-- 迁移后期望的探索页菜单（用于人工核对）
--   一级目录：App运营（M，path=app-operation）
--   二级页面：探索页管理（C，path=explore，component=cms/explore/index）
--   按钮：mkt:cms:explore:add|edit|publish|offline（F，父级与权限标识不变）
--   路由：/app-operation/explore
-- ============================================================
