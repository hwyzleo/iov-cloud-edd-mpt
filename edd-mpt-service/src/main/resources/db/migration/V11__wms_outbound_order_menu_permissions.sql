-- EDD-MPT：OTD-WMS 出库管理菜单与权限（补齐 V7／V8 遗留的出库页面）
-- ============================================================
-- 背景：
--   1) OTD-WMS 服务端出库 MPT 接口早已就绪（OTD-WMS-DSN-CR-001 §5.5）：
--      GET  /mpt/outboundOrder/list
--      GET  /mpt/outboundOrder/{outboundOrderId}
--      GET  /mpt/outboundOrder/{outboundOrderId}/detail
--      POST /mpt/outboundOrder/{orderNum}/vins/{vin}/loading
--      POST /mpt/outboundOrder/{orderNum}/vins/{vin}/outbound
--      POST /mpt/outboundOrder/{orderNum}/cancellation
--      权限点 completeVehicle:warehouse:outboundOrder:{list,query,load,outbound,cancel}。
--   2) V7／V8 因 mpt-ui 缺少出库页面组件而明确不创建该菜单，导致外部系统（TMS 调度）
--      预留出库单后，仓储运营人员在管理后台无法执行装车／出库确认，整车出库闭环断在前端。
--   3) 本次已补齐前端页面 completevehicle/warehouse/outboundorder/index，
--      同步创建 C 类页面菜单与 F 类按钮权限，并做角色兜底授权。
-- 前置假设：仓储管理目录（M）与仓库信息管理页面（C）已存在，全部按 perms 定位，
--          不依赖固定 menu_id；出库单不区分库类型（后端无 warehouseLevel 过滤）。
-- 注意：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧权限集合渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 0. 锚点：仓储管理目录（仓库信息管理页面的父级）
-- ------------------------------------------------------------
SET @wms_root = (SELECT parent_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:list' LIMIT 1);
SET @next_order = (SELECT COALESCE(MAX(order_num), 0) + 10 FROM sys_menu WHERE parent_id = @wms_root);

-- ------------------------------------------------------------
-- 1. 新增“出库管理”页面（C 类菜单）
--    路由：仓储管理目录下 outboundOrder，组件 completevehicle/warehouse/outboundorder/index
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '出库管理', @wms_root, @next_order, 'outboundOrder', 'completevehicle/warehouse/outboundorder/index',
       NULL, '', 1, 0, 'C', '0', '0', 'completeVehicle:warehouse:outboundOrder:list', 'shopping', 'admin', sysdate()
FROM DUAL
WHERE @wms_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:list');

SET @outbound_order_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:list' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 出库管理按钮权限（与服务端 @RequiresPermissions 一一对应）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '出库单详情', @outbound_order_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:outboundOrder:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @outbound_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '装车确认', @outbound_order_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:outboundOrder:load', '#', 'admin', sysdate()
FROM DUAL
WHERE @outbound_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:load');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '出库确认', @outbound_order_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:outboundOrder:outbound', '#', 'admin', sysdate()
FROM DUAL
WHERE @outbound_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:outbound');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '取消出库单', @outbound_order_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:outboundOrder:cancel', '#', 'admin', sysdate()
FROM DUAL
WHERE @outbound_order_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:outboundOrder:cancel');

-- ------------------------------------------------------------
-- 3. 授权
-- 3.1 已拥有仓储管理目录（M）的角色，自动获得出库页面（C 类）
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @outbound_order_menu
FROM sys_role_menu rm
WHERE @wms_root IS NOT NULL
  AND @outbound_order_menu IS NOT NULL
  AND rm.menu_id = @wms_root;

-- 3.2 已拥有任一仓储域页面（C 类）的角色，自动获得出库页面
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @outbound_order_menu
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @outbound_order_menu IS NOT NULL
  AND m.menu_type = 'C'
  AND m.perms LIKE 'completeVehicle:warehouse:%:list';

-- 3.3 已拥有出库页面的角色，自动获得该页面下全部按钮权限（F 类）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.parent_id = rm.menu_id
WHERE @outbound_order_menu IS NOT NULL
  AND rm.menu_id = @outbound_order_menu
  AND child.menu_type = 'F';

-- 3.4 超级管理员（1）与超级用户（100）兜底授权出库页面与按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:warehouse:outboundOrder:%')
WHERE r.role_id IN (1, 100);
