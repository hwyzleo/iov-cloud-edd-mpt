-- EDD-MPT：内容管理目录化并新增「协议管理」页面（MKT-CMS-DSN-CR-002 菜单层级调整）
-- ============================================================
-- 背景：
--   V14 把协议内容页面直接注册为「营销管理 / 内容管理」二级菜单（C 类，组件 cms/agreement/index），
--   页面能力名与“内容管理”这一领域名重叠，后续再新增内容类页面（公告、Banner 等）时无处挂载。
-- 本脚本：
--   1) 「内容管理」由 C 类页面改造为 M 类目录（path=content，component=NULL → ParentView 容器）；
--   2) 在其下新增 C 类页面「协议管理」（path=agreement，component=cms/agreement/index，
--      perms=mkt:cms:agreement:query），承载原协议内容页面；
--   3) 协议按钮（F 类）统一挂到「协议管理」页面下，权限标识不变，角色授权不失效；
--   4) 已拥有协议菜单的角色自动获得「协议管理」页面，超级管理员（1）/超级用户（100）兜底。
-- 最终菜单层级：营销管理 / 内容管理 / 协议管理，路由 /marketing/content/agreement。
-- 全部按 path、perms 定位，不依赖固定 menu_id；幂等可重跑。
-- 说明：菜单变更后需重新登录或清理登录缓存，前端才会按新菜单树渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 定位：营销管理目录 → 内容管理菜单
-- ------------------------------------------------------------
SET @marketing_root = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing' LIMIT 1);

SET @content_menu = (SELECT menu_id FROM sys_menu
                     WHERE menu_name = '内容管理' AND path = 'content'
                       AND (@marketing_root IS NULL OR parent_id = @marketing_root)
                     ORDER BY menu_id LIMIT 1);

-- ------------------------------------------------------------
-- 2. 内容管理：C 类页面 → M 类目录（组件交由 ParentView 承载）
--    权限标识清空：协议查询权限由下级「协议管理」页面承载
-- ------------------------------------------------------------
UPDATE sys_menu
SET menu_type   = 'M',
    component   = NULL,
    perms       = '',
    icon        = 'documentation',
    order_num   = 1,
    update_by   = 'admin',
    update_time = sysdate()
WHERE menu_id = @content_menu;

-- ------------------------------------------------------------
-- 3. 新增三级页面：协议管理（C 类，组件 cms/agreement/index）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议管理', @content_menu, 1, 'agreement', 'cms/agreement/index', NULL, '', 1, 0, 'C', '0', '0',
       'mkt:cms:agreement:query', 'form', 'admin', sysdate()
FROM DUAL
WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @content_menu AND path = 'agreement');

SET @agreement_page = (SELECT menu_id FROM sys_menu
                       WHERE parent_id = @content_menu AND path = 'agreement'
                       ORDER BY menu_id LIMIT 1);

-- ------------------------------------------------------------
-- 4. 协议按钮（F 类）迁移到「协议管理」页面下，权限标识与顺序保持不变
-- ------------------------------------------------------------
UPDATE sys_menu
SET parent_id   = @agreement_page,
    update_by   = 'admin',
    update_time = sysdate()
WHERE menu_type = 'F'
  AND perms LIKE 'mkt:cms:agreement:%'
  AND @agreement_page IS NOT NULL;

-- ------------------------------------------------------------
-- 5. 授权
-- 5.1 已拥有内容管理菜单或协议权限的角色，自动获得「协议管理」页面
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @agreement_page
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @agreement_page IS NOT NULL
  AND (m.menu_id = @content_menu OR m.perms LIKE 'mkt:cms:agreement:%');

-- 5.2 协议管理页面可见 → 补齐上级目录（内容管理、营销管理）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @content_menu
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @content_menu IS NOT NULL
  AND (m.menu_id = @agreement_page OR m.perms LIKE 'mkt:cms:agreement:%');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @marketing_root
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @marketing_root IS NOT NULL
  AND (m.menu_id IN (@content_menu, @agreement_page) OR m.perms LIKE 'mkt:cms:agreement:%');

-- 5.3 超级管理员（1）与超级用户（100）兜底授权协议域全部菜单（含目录与页面）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m
        ON m.perms LIKE 'mkt:cms:agreement:%'
        OR m.menu_id IN (@content_menu, @marketing_root)
WHERE r.role_id IN (1, 100);

-- ============================================================
-- 迁移后期望的协议域菜单（用于人工核对）
--   一级目录：营销管理（M，path=marketing）
--   二级目录：内容管理（M，path=content，component=NULL）
--   三级页面：协议管理（C，path=agreement，component=cms/agreement/index，perms=mkt:cms:agreement:query）
--   按钮：mkt:cms:agreement:add|edit|freeze|publish（F，父级=协议管理；查询权限由页面承载）
--   路由：/marketing/content/agreement
-- ============================================================
