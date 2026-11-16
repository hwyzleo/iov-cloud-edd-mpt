-- EDD-MPT：WMS 仓储管理菜单结构统一（平铺 + 清理库类型空目录）
-- ============================================================
-- 背景：
--   原菜单按“前置库/中央库/区域库/交付库管理”四个目录分组，仅前置库目录下有
--   5 个页面，其余三个目录为空。OTD-WMS 的业务流程对各库类型一致（通过
--   warehouseCode 区分），菜单统一为按业务能力平铺。
-- 本脚本：
--   1) 将前置库目录下 5 个页面平铺到“仓储管理”目录下，path 去掉 pdc 前缀，
--      component 保持不变（仍指向现有前端组件，保证路由可加载）；
--   2) 删除 pdc/cdc/rdc/ddc 四个库类型目录（移动后均为空目录）及其角色授权。
-- 保留限制（纯菜单数据无法解决，需前端配套）：
--   1) 现有页面 JS 写死 warehouseLevel='PDC'，平铺后的通用菜单页面数据仍仅前置库；
--   2) 出库管理菜单待 mpt-ui 出库页面组件就绪后另行迁移，避免路由 404。
-- 全部按 perms/path 定位，不依赖固定 menu_id；脚本可重复执行。
-- ============================================================

-- ------------------------------------------------------------
-- 0. 锚点
-- ------------------------------------------------------------
SET @wms_root = (SELECT parent_id FROM sys_menu WHERE perms = 'completeVehicle:warehouse:info:list' LIMIT 1);

-- ------------------------------------------------------------
-- 1. 平铺 5 个页面到“仓储管理”下，统一 path 与显示顺序
-- ------------------------------------------------------------
UPDATE sys_menu
SET parent_id = @wms_root, path = 'preInboundOrder', order_num = 20,
    update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:preInboundOrder:list' AND @wms_root IS NOT NULL;

UPDATE sys_menu
SET parent_id = @wms_root, path = 'inboundOrder', order_num = 30,
    update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inboundOrder:list' AND @wms_root IS NOT NULL;

UPDATE sys_menu
SET parent_id = @wms_root, path = 'inventory', order_num = 40,
    update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inventory:list' AND @wms_root IS NOT NULL;

UPDATE sys_menu
SET parent_id = @wms_root, path = 'inventoryTransfer', order_num = 50,
    update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inventoryTransfer:list' AND @wms_root IS NOT NULL;

UPDATE sys_menu
SET parent_id = @wms_root, path = 'inventoryCount', order_num = 60,
    update_by = 'admin', update_time = sysdate()
WHERE perms = 'completeVehicle:warehouse:inventoryCount:list' AND @wms_root IS NOT NULL;

-- ------------------------------------------------------------
-- 2. 清理库类型目录（pdc/cdc/rdc/ddc）的角色授权
-- ------------------------------------------------------------
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.parent_id = @wms_root AND m.menu_type = 'M' AND m.path IN ('pdc', 'cdc', 'rdc', 'ddc');

-- ------------------------------------------------------------
-- 3. 删除库类型空目录（仅当目录下已无任何子菜单时）
-- ------------------------------------------------------------
DELETE m FROM sys_menu m
LEFT JOIN (SELECT DISTINCT parent_id FROM sys_menu) child ON child.parent_id = m.menu_id
WHERE m.parent_id = @wms_root AND m.menu_type = 'M' AND m.path IN ('pdc', 'cdc', 'rdc', 'ddc')
  AND child.parent_id IS NULL;

-- ============================================================
-- 迁移后期望结构：
--   仓储管理
--   ├── 仓库信息管理        (info,               order 10)
--   ├── 预入库单管理        (preInboundOrder,    order 20)
--   ├── 入库单管理          (inboundOrder,       order 30)
--   ├── 库存管理            (inventory,          order 40)
--   ├── 同库移库管理        (inventoryTransfer,  order 50)
--   └── 盘点管理            (inventoryCount,     order 60)
-- 注意：菜单变更后需重新登录或清理登录缓存，前端才会按新路由渲染。
-- ============================================================
