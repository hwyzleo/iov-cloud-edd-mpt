-- EDD-MPT：App运营目录归入「营销管理」
-- ============================================================
-- 背景：App 页面运营作为营销运营的一类能力，统一归入「营销管理」。
-- 本脚本：
--   1) 将 V17 创建的一级目录「App运营」调整为「营销管理」的二级目录；
--   2) 探索页仍归属「App运营」，其 menu_id、组件、按钮父级和权限标识均不变；
--   3) 已拥有 App运营或探索页权限的角色补齐「营销管理」上级目录授权；
--   4) 不修改 V16/V17 历史迁移，不改变页面与按钮授权范围。
-- 最终菜单层级：营销管理 / App运营 / 探索页管理，
-- 最终路由：/marketing/app-operation/explore。
-- 全部按 path、perms 定位，不依赖固定 menu_id；幂等可重跑。
-- 说明：菜单变更后需重新登录或清理登录缓存，前端才会按新菜单树渲染。
-- ============================================================

SET @marketing_root = (SELECT menu_id FROM sys_menu
                       WHERE parent_id = 0 AND path = 'marketing'
                       ORDER BY menu_id LIMIT 1);

SET @app_operation_root = (SELECT menu_id FROM sys_menu
                           WHERE path = 'app-operation' AND menu_type = 'M'
                           ORDER BY menu_id LIMIT 1);

-- 1. App运营：一级目录 → 营销管理二级目录
UPDATE sys_menu
SET parent_id   = @marketing_root,
    order_num   = 2,
    update_by   = 'admin',
    update_time = sysdate()
WHERE menu_id = @app_operation_root
  AND @marketing_root IS NOT NULL;

-- 2. 授权：拥有 App运营或探索页权限的角色必须同时可见营销管理
SET @explore_page = (SELECT menu_id FROM sys_menu
                     WHERE perms = 'mkt:cms:explore:query' AND menu_type = 'C'
                     ORDER BY menu_id LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @marketing_root
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @marketing_root IS NOT NULL
  AND (m.menu_id IN (@app_operation_root, @explore_page)
       OR m.perms LIKE 'mkt:cms:explore:%');

-- 超级管理员（1）/超级用户（100）兜底授权营销管理
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @marketing_root
FROM sys_role r
WHERE r.role_id IN (1, 100)
  AND @marketing_root IS NOT NULL;

-- ============================================================
-- 迁移后期望的探索页菜单（用于人工核对）
--   一级目录：营销管理（M，path=marketing）
--   二级目录：App运营（M，path=app-operation）
--   三级页面：探索页管理（C，path=explore，component=cms/explore/index）
--   按钮：mkt:cms:explore:add|edit|publish|offline（F，父级与权限标识不变）
--   路由：/marketing/app-operation/explore
-- ============================================================
