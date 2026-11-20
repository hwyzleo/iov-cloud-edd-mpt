-- EDD-MPT：协议内容管理菜单与权限（MKT-CMS-DSN-CR-002）
-- ============================================================
-- 背景：
--   MKT-CMS 协议内容子域（MKT-CMS-DSN-CR-001）已交付管理接口
--   /api/mpt/agreement/v1/{agreement-drafts,agreement-revisions,agreement-bundles,agreement-metadata}；
--   MPT UI 新增页面 src/views/cms/agreement/index.vue。
--   本脚本新建一级目录「营销管理」，其下新增二级页面「内容管理」，并补齐按钮权限点。
-- 权限点：mkt:cms:agreement:query / add / edit / freeze / publish
-- 说明：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧权限集合渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 一级目录：营销管理（M 类）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '营销管理', 0, 25, 'marketing', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'marketing');

SET @marketing_root = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 二级页面：内容管理（C 类，组件 cms/agreement/index）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '内容管理', @marketing_root, 1, 'content', 'cms/agreement/index', NULL, '', 1, 0, 'C', '0', '0',
       'mkt:cms:agreement:query', 'documentation', 'admin', sysdate()
FROM DUAL
WHERE @marketing_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @marketing_root AND path = 'content');

SET @content_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @marketing_root AND path = 'content' LIMIT 1);

-- ------------------------------------------------------------
-- 3. 按钮权限点（与后端 @RequiresPermissions 对齐）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议查询', @content_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:agreement:query', '#',
       'admin', sysdate()
FROM DUAL WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:agreement:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议新增', @content_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:agreement:add', '#',
       'admin', sysdate()
FROM DUAL WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:agreement:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议修改', @content_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:agreement:edit', '#',
       'admin', sysdate()
FROM DUAL WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:agreement:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议定稿', @content_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:agreement:freeze', '#',
       'admin', sysdate()
FROM DUAL WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:agreement:freeze');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议发布', @content_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'mkt:cms:agreement:publish', '#',
       'admin', sysdate()
FROM DUAL WHERE @content_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mkt:cms:agreement:publish');

-- ------------------------------------------------------------
-- 4. 授权
-- 4.1 已拥有「内容管理」页面的角色，自动获得该页面下的协议按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.parent_id = rm.menu_id
WHERE child.perms LIKE 'mkt:cms:agreement:%';

-- 4.2 超级管理员（1）与超级用户（100）兜底授权协议域全部菜单（含新页面/按钮）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON m.perms LIKE 'mkt:cms:agreement:%'
WHERE r.role_id IN (1, 100);

-- 4.3 目录与页面：授予已拥有协议按钮权限的角色，保证菜单可见
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @content_menu
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.menu_id = rm.menu_id
WHERE child.perms LIKE 'mkt:cms:agreement:%'
  AND @content_menu IS NOT NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @marketing_root
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.menu_id = rm.menu_id
WHERE child.perms LIKE 'mkt:cms:agreement:%'
  AND @marketing_root IS NOT NULL;

-- ============================================================
-- 迁移后期望的协议域菜单（用于人工核对）
--   目录：营销管理（M，path=marketing）
--   页面：内容管理（C，path=content，component=cms/agreement/index，perms=mkt:cms:agreement:query）
--   按钮：mkt:cms:agreement:query|add|edit|freeze|publish
--   注意：SGW 需另行配置 /mkt-cms/** 路由（本脚本不含网关配置）。
-- ============================================================
