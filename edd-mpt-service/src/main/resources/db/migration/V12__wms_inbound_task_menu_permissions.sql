-- EDD-MPT：OTD-WMS 交付库待入库菜单与权限（OTD-WMS-DSN-CR-003）
-- ============================================================
-- 背景：
--   1) OTD-WMS 新增交付库入库闭环（CR-003）：外部请求方（本期 TMS 确认交车）生产
--      wms.vehicle-inbound.requested，WMS 生成待入库任务，仓库员到货/入库/取消。
--      服务端接口：
--        GET  /mpt/inboundTask/list
--        GET  /mpt/inboundTask/{inboundTaskId}
--        POST /mpt/inboundTask/{orderNum}/arrival
--        POST /mpt/inboundTask/{orderNum}/inbound
--        POST /mpt/inboundTask/{orderNum}/cancellation
--      权限点 completeVehicle:warehouse:inboundTask:{list,query,arrival,inbound,cancel}。
--   2) 前端页面 completevehicle/warehouse/inboundtask/index 与库存页去 PDC 硬编码同批交付。
-- 前置假设：仓储管理目录（M）与仓库信息管理页面（C）已存在，全部按 perms 定位。
-- 注意：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧权限集合渲染。
-- ============================================================

-- ------------------------------------------------------------
-- 0. 锚点与顺序号
-- ------------------------------------------------------------
SET @wms_root = (SELECT parent_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:list' LIMIT 1);
SET @next_order = (SELECT COALESCE(MAX(order_num), 0) + 10 FROM sys_menu WHERE parent_id = @wms_root);

-- ------------------------------------------------------------
-- 1. 新增“交付库待入库”页面（C 类菜单）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '交付库待入库', @wms_root, @next_order, 'inboundTask', 'completevehicle/warehouse/inboundtask/index',
       NULL, '', 1, 0, 'C', '0', '0', 'completeVehicle:warehouse:inboundTask:list', 'download', 'admin', sysdate()
FROM DUAL
WHERE @wms_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:list');

SET @inbound_task_menu = (SELECT menu_id FROM sys_menu
                          WHERE perms = 'completeVehicle:warehouse:inboundTask:list' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 按钮权限（与服务端 @RequiresPermissions 一一对应）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '待入库详情', @inbound_task_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundTask:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_task_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '到货确认', @inbound_task_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundTask:arrival', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_task_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:arrival');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '入库确认', @inbound_task_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundTask:inbound', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_task_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:inbound');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '取消待入库', @inbound_task_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundTask:cancel', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_task_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:cancel');

-- ------------------------------------------------------------
-- 3. 授权
-- 3.1 已拥有仓储管理目录的角色获得页面
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @inbound_task_menu
FROM sys_role_menu rm
WHERE @wms_root IS NOT NULL
  AND @inbound_task_menu IS NOT NULL
  AND rm.menu_id = @wms_root;

-- 3.2 已拥有任一仓储域页面的角色获得页面
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @inbound_task_menu
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE @inbound_task_menu IS NOT NULL
  AND m.menu_type = 'C'
  AND m.perms LIKE 'completeVehicle:warehouse:%:list';

-- 3.3 已拥有待入库页面的角色获得全部按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.parent_id = rm.menu_id
WHERE @inbound_task_menu IS NOT NULL
  AND rm.menu_id = @inbound_task_menu
  AND child.menu_type = 'F';

-- 3.4 超级管理员（1）与超级用户（100）兜底
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:warehouse:inboundTask:%')
WHERE r.role_id IN (1, 100);
