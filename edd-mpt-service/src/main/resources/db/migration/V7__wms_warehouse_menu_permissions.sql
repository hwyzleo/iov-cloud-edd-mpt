-- EDD-MPT：OTD-WMS 仓储管理菜单与权限对齐（OTD-WMS-DSN-CR-001 / US-001~US-007）
-- ============================================================
-- 背景：
--   1) 仓储管理菜单（仓库信息管理 + 前置库下 5 个页面）由界面手工创建，只有 C 类页面菜单，
--      没有任何 F 类按钮权限，且 sys_role_menu 中无任何授权记录。
--   2) OTD-WMS 服务端在 WMS-DSN-CR-001 中已将通用 CRUD 收敛为业务动作：
--      预入库单 arrival/inbound/cancel，移库仅支持同库移库（inventoryTransfer:add），
--      库存为只读查询（禁止通用 add/edit/remove）。
--   3) 出库管理（outboundOrder:list/query/load/outbound/cancel）服务端已就绪，
--      但 mpt-ui 尚无出库页面组件，本脚本暂不创建出库 C 菜单，避免前端路由 404；
--      中央库/区域库/交付库子菜单同理，待前端页面就绪后另行迁移。
-- 本脚本做增量对齐：补 F 按钮（与后端 @RequiresPermissions 一致）→ 同库移库文案 → 兜底授权。
-- 前置假设：现有 C 页面菜单已存在，全部按 perms 定位，不依赖固定 menu_id。
-- ============================================================

-- ------------------------------------------------------------
-- 0. 锚点：现有 6 个 C 类页面菜单
-- ------------------------------------------------------------
SET @wh_info_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:list' LIMIT 1);
SET @pre_inbound_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:list' LIMIT 1);
SET @inbound_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundOrder:list' LIMIT 1);
SET @inventory_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventory:list' LIMIT 1);
SET @transfer_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:list' LIMIT 1);
SET @count_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:list' LIMIT 1);

-- ------------------------------------------------------------
-- 1. 文案边界：移库仅支持同库移库（US-005，跨库移库为 Out of Scope）
-- ------------------------------------------------------------
UPDATE sys_menu
SET menu_name = '同库移库管理', update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:list'
  AND menu_name = '移库管理';

-- ------------------------------------------------------------
-- 2. 仓库信息管理（US-001）：新增/修改/删除/详情/导出
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '仓库新增', @wh_info_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:info:add', '#', 'admin', sysdate()
FROM DUAL
WHERE @wh_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '仓库修改', @wh_info_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:info:edit', '#', 'admin', sysdate()
FROM DUAL
WHERE @wh_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '仓库删除', @wh_info_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:info:remove', '#', 'admin', sysdate()
FROM DUAL
WHERE @wh_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '仓库详情', @wh_info_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:info:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @wh_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '仓库导出', @wh_info_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:info:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @wh_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:export');

-- ------------------------------------------------------------
-- 3. 预入库单管理（US-002/US-003）：到货确认/入库确认/取消/详情/导出
--    注意：不提供 add/edit/remove，预入库单由 VMD Kafka 事件创建，完成前仅允许取消。
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '到货确认', @pre_inbound_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:preInboundOrder:arrival', '#', 'admin', sysdate()
FROM DUAL
WHERE @pre_inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:arrival');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '入库确认', @pre_inbound_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:preInboundOrder:inbound', '#', 'admin', sysdate()
FROM DUAL
WHERE @pre_inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:inbound');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '预入库取消', @pre_inbound_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:preInboundOrder:cancel', '#', 'admin', sysdate()
FROM DUAL
WHERE @pre_inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:cancel');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '预入库详情', @pre_inbound_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:preInboundOrder:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @pre_inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '预入库导出', @pre_inbound_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:preInboundOrder:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @pre_inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:export');

-- ------------------------------------------------------------
-- 4. 入库单管理（US-003）：已完成事实，仅详情/导出
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '入库单详情', @inbound_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundOrder:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundOrder:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '入库单导出', @inbound_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inboundOrder:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @inbound_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundOrder:export');

-- ------------------------------------------------------------
-- 5. 库存管理（US-004）：当前库存只读，仅详情/导出
--    需求明确禁止通用库存 add/edit/remove，不得新增此类权限点。
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '库存详情', @inventory_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventory:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @inventory_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventory:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '库存导出', @inventory_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventory:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @inventory_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventory:export');

-- ------------------------------------------------------------
-- 6. 同库移库管理（US-005）：移库执行/详情/导出
--    不提供 edit/remove：已完成移库事实不允许修改或物理删除。
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '移库执行', @transfer_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryTransfer:add', '#', 'admin', sysdate()
FROM DUAL
WHERE @transfer_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '移库详情', @transfer_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryTransfer:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @transfer_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '移库导出', @transfer_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryTransfer:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @transfer_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:export');

-- ------------------------------------------------------------
-- 7. 盘点管理（US-006）：盘点单主从文档维护
--    仅文档管理，不代表完整盘点工作流（差异计算/库存调整为 Out of Scope）。
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '盘点单新增', @count_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryCount:add', '#', 'admin', sysdate()
FROM DUAL
WHERE @count_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '盘点单修改', @count_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryCount:edit', '#', 'admin', sysdate()
FROM DUAL
WHERE @count_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '盘点单删除', @count_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryCount:remove', '#', 'admin', sysdate()
FROM DUAL
WHERE @count_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '盘点单详情', @count_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryCount:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @count_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '盘点单导出', @count_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:warehouse:inventoryCount:export', '#', 'admin', sysdate()
FROM DUAL
WHERE @count_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inventoryCount:export');

-- ------------------------------------------------------------
-- 8. 授权
-- 8.1 已拥有对应页面菜单的角色，自动获得该页面下的按钮权限
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu parent ON rm.menu_id = parent.menu_id
INNER JOIN sys_menu child ON child.parent_id = parent.menu_id
WHERE child.menu_type = 'F'
  AND child.perms LIKE 'completeVehicle:warehouse:%';

-- 8.2 超级管理员（1）与超级用户（100）兜底授权仓储域全部菜单（含目录/页面/按钮）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:warehouse:%')
WHERE r.role_id IN (1, 100);

-- ============================================================
-- 本脚本未覆盖、需前端页面就绪后另行迁移的事项：
--   1) 出库管理 C 菜单及按钮（outboundOrder:list/query/load/outbound/cancel）：
--      mpt-ui 缺少 completevehicle/warehouse 出库页面组件。
--   2) 中央库/区域库/交付库子菜单：前端无对应页面组件，且现有 PDC 页面 API
--      写死 warehouseLevel='PDC'，不能直接复用为其他库页面。
--   3) 菜单结构统一化（按业务能力平铺 + 仓库筛选器替代按库分组）：依赖前端
--      页面去 PDC 硬编码改造，需随 OTD-WMS 前端 CR 一并实施。
--   4) “交付库管理”与代码枚举 DDC（经销商库）的术语统一：待业务确认命名。
-- 注意：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧的权限集合渲染。
-- ============================================================
