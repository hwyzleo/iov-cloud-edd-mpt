-- EDD-MPT：OTD-TMS 运单管理菜单与权限（OTD-TMS-DSN-CR-001 / 前端页面补齐）
-- ============================================================
-- 背景：
--   1) OTD-TMS 侧运单 MPT 接口已就绪（list / getInfo / listDetail / dispatch / depart /
--      handover / receipt / cancel / manualClose，权限点 completeVehicle:transport:transportOrder:*），
--      但 MPT 前端一直没有运单页面组件、菜单与按钮权限，发运信息（调度/发车/交车/接车）无法在界面操作。
--   2) 本次补齐前端页面 completevehicle/transport/transportorder/index，并同步菜单与按钮权限。
--   3) 顺带对齐装载车菜单权限标识：服务端为 loadVehicle:list，历史菜单误写为 loadingVehicle:list。
-- 前置假设：运输管理目录（M）与基础信息管理（M）已存在，全部按 perms 定位，不依赖固定 menu_id。
-- 注意：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧权限集合渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 定位运输管理目录（父级为“运输管理”，不挂在“基础信息管理”下）
--    路径：运输管理(M) → 基础信息管理(M) → 各主数据页面(C)，需向上取两级
-- ------------------------------------------------------------
SET @node_type_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:transport:logisticsNodeType:list' LIMIT 1);
SET @base_info_menu = (SELECT parent_id FROM sys_menu WHERE menu_id = @node_type_menu);
SET @transport_root = COALESCE(
    (SELECT parent_id FROM sys_menu WHERE menu_id = @base_info_menu),
    (SELECT menu_id FROM sys_menu WHERE menu_name = '运输管理' AND parent_id <> 0 LIMIT 1)
);

-- ------------------------------------------------------------
-- 2. 装载车菜单权限标识对齐服务端（loadVehicle:list）
-- ------------------------------------------------------------
UPDATE sys_menu
SET perms = 'completeVehicle:transport:loadVehicle:list', update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:transport:loadingVehicle:list';

-- ------------------------------------------------------------
-- 3. 新增“运单管理”页面（C 类菜单）
--    路由：运输管理目录下 transportorder，组件 completevehicle/transport/transportorder/index
-- ------------------------------------------------------------
SET @next_order = (SELECT COALESCE(MAX(order_num), 0) + 10 FROM sys_menu WHERE parent_id = @transport_root);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运单管理', @transport_root, @next_order, 'transportorder', 'completevehicle/transport/transportorder/index',
       NULL, '', 1, 0, 'C', '0', '0', 'completeVehicle:transport:transportOrder:list', 'shopping', 'admin', sysdate()
FROM DUAL
WHERE @transport_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:list');

SET @transport_order_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:list' LIMIT 1);

-- ------------------------------------------------------------
-- 4. 运单管理按钮权限（与服务端 @RequiresPermissions 一一对应）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运单明细查询', @transport_order_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运单调度', @transport_order_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:dispatch', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:dispatch');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '确认发车', @transport_order_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:depart', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:depart');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运单交车', @transport_order_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:handover', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:handover');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '运单接车', @transport_order_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:receipt', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:receipt');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '取消运单', @transport_order_menu, 6, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:cancel', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:cancel');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '手工结束', @transport_order_menu, 7, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:transport:transportOrder:manualClose', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:manualClose');

-- ------------------------------------------------------------
-- 5. 授权
-- 5.1 已拥有运输域页面菜单的角色，自动获得运单页面与按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu parent ON rm.menu_id = parent.menu_id
INNER JOIN sys_menu child ON child.parent_id = parent.menu_id
WHERE child.perms LIKE 'completeVehicle:transport:transportOrder:%';

-- 5.2 超级管理员（1）与超级用户（100）兜底授权运单页面与按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:transport:transportOrder:%')
WHERE r.role_id IN (1, 100);

-- 5.3 兜底：已拥有“运单管理”页面菜单的角色授权页面本身
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @transport_order_menu
FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE @transport_order_menu IS NOT NULL
  AND m.perms LIKE 'completeVehicle:transport:%';
