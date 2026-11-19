-- EDD-MPT：OTD-WMS 入库菜单合并与术语统一（OTD-WMS-DSN-CR-003 术语更正）
-- ============================================================
-- 背景：
--   1) 「预入库单管理」与「交付库待入库」本质都是「入库入口」，不应按仓库层级拆菜单；
--   2) 「交付库待入库」命名把本期场景写进能力名，需去限定（能力=到货通知 ASN，仓库由筛选决定）。
-- 本脚本：
--   1) 把“预入库单管理”C 菜单改造为“入库管理”容器页（path=inbound，组件 inbound/index），
--      权限标识仍沿用 completeVehicle:warehouse:preInboundOrder:list（避免角色授权失效）；
--   2) 把“交付库待入库”C 菜单改为容器下的 F 类页签入口（menu_name=到货通知），
--      权限 completeVehicle:warehouse:inboundTask:list 不变；
--   3) “入库单管理”显示名改为“入库记录”。
-- 全部按 perms/path 定位，不依赖固定 menu_id；幂等可重跑。
-- ============================================================

SET @wms_root = (SELECT parent_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:list' LIMIT 1);
SET @inbound_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:preInboundOrder:list' LIMIT 1);
SET @notice_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:inboundTask:list' LIMIT 1);

-- 1. 预入库单管理 → 入库管理（容器页）
UPDATE sys_menu
SET menu_name = '入库管理', path = 'inbound', component = 'completevehicle/warehouse/inbound/index',
    icon = 'tree-table', update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:preInboundOrder:list';

-- 2. 到货通知：由独立 C 菜单收敛为容器下的 F 类页签入口
UPDATE sys_menu
SET menu_name = '到货通知', menu_type = 'F', path = '', component = NULL,
    parent_id = COALESCE(@inbound_menu, parent_id),
    order_num = 9, icon = '#', update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inboundTask:list';

-- 3. 入库单管理 → 入库记录
UPDATE sys_menu
SET menu_name = '入库记录', update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inboundOrder:list';

-- 4. 授权：拥有入库管理页面的角色同时获得到货通知页签权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @notice_menu
FROM sys_role_menu rm
WHERE @inbound_menu IS NOT NULL
  AND @notice_menu IS NOT NULL
  AND rm.menu_id = @inbound_menu;

-- 5. 超级管理员（1）与超级用户（100）兜底授权仓储域全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:warehouse:%')
WHERE r.role_id IN (1, 100);
