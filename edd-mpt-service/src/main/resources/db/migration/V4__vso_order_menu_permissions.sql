-- EDD-MPT：VSO 订单管理菜单与权限调整（OTD-VSO-DSN-CR-002 / US-011）
-- ============================================================
-- 背景：
--   1) MPT 订单页（订单信息/改配/配车/发运/分配交付人员）早期由界面手工建菜单，
--      权限标识为 completeVehicle:order:*；VSO 服务端在 US-011 中把写动作权限点收敛为
--      assignVehicle:assign|reassign|unbind、transport:apply、assignDeliveryPerson:assign、
--      info:audit|lock|close，并移除了逻辑删除端点及其 info:remove 权限。
--   2) 新增购车协议后台管理页（/otd/order/purchaseAgreement），权限点 agreement:list|edit|remove。
-- 本脚本做增量对齐：清理废弃权限 → 补页面与按钮 → 按“已拥有该页面菜单的角色”授权。
-- 前置假设：VSO 订单管理目录及其子页面菜单已存在（历史环境由界面维护）。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 清理已废弃的订单域权限点（前后端均已不再引用）
--    assignVehicle:edit  → 拆分为 assign/reassign/unbind
--    info:remove         → 逻辑删除端点与权限点已由 OTD-VSO T-018 移除
--    info:add/info:edit/info:export → 订单不允许界面新增/修改，且后端无导出端点
-- ------------------------------------------------------------
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms IN (
    'completeVehicle:order:assignVehicle:edit',
    'completeVehicle:order:info:remove',
    'completeVehicle:order:info:add',
    'completeVehicle:order:info:edit',
    'completeVehicle:order:info:export'
);

DELETE FROM sys_menu
WHERE perms IN (
    'completeVehicle:order:assignVehicle:edit',
    'completeVehicle:order:info:remove',
    'completeVehicle:order:info:add',
    'completeVehicle:order:info:edit',
    'completeVehicle:order:info:export'
);

-- ------------------------------------------------------------
-- 2. 定位订单域菜单锚点（全部按 perms 定位，不依赖固定 menu_id）
-- ------------------------------------------------------------
SET @order_info_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:info:list' LIMIT 1);
SET @change_model_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:changeModel:list' LIMIT 1);
SET @assign_vehicle_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:assignVehicle:list' LIMIT 1);
SET @transport_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:transport:list' LIMIT 1);
SET @assign_dp_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:assignDeliveryPerson:list' LIMIT 1);

-- 订单管理目录：优先取订单信息页的父菜单，其次退化为其它订单页的父菜单
SET @order_root = COALESCE(
    (SELECT parent_id FROM sys_menu WHERE menu_id = @order_info_menu),
    (SELECT parent_id FROM sys_menu WHERE menu_id = @change_model_menu),
    (SELECT parent_id FROM sys_menu WHERE menu_id = @transport_menu),
    (SELECT parent_id FROM sys_menu WHERE menu_id = @assign_vehicle_menu)
);

-- ------------------------------------------------------------
-- 3. 新增“购车协议”页面（C 类菜单）
--    路由：订单管理目录下 purchaseAgreement，组件 otd/purchaseAgreement/index
-- ------------------------------------------------------------
SET @next_order = (SELECT COALESCE(MAX(order_num), 0) + 10 FROM sys_menu WHERE parent_id = @order_root);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '购车协议', @order_root, @next_order, 'purchaseAgreement', 'otd/purchaseAgreement/index', NULL, '',
       1, 0, 'C', '0', '0', 'completeVehicle:order:agreement:list', 'documentation', 'admin', sysdate()
FROM DUAL
WHERE @order_root IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:agreement:list');

SET @agreement_menu = (SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:order:agreement:list' LIMIT 1);

-- 3.1 购车协议按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议新增/修改', @agreement_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:agreement:edit', '#', 'admin', sysdate()
FROM DUAL
WHERE @agreement_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:agreement:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '协议下架', @agreement_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:agreement:remove', '#', 'admin', sysdate()
FROM DUAL
WHERE @agreement_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:agreement:remove');

-- ------------------------------------------------------------
-- 4. 订单信息页：详情/审核/锁单/关闭/物理删除
--    parent 必须是页面菜单（info:list），不得挂在订单管理目录上
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '订单详情查询', @order_info_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:info:query', '#', 'admin', sysdate()
FROM DUAL
WHERE @order_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:info:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '订单审核', @order_info_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:info:audit', '#', 'admin', sysdate()
FROM DUAL
WHERE @order_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:info:audit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '订单锁单', @order_info_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:info:lock', '#', 'admin', sysdate()
FROM DUAL
WHERE @order_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:info:lock');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '订单关闭', @order_info_menu, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:info:close', '#', 'admin', sysdate()
FROM DUAL
WHERE @order_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:info:close');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '订单物理删除', @order_info_menu, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:info:physicalDelete', '#', 'admin', sysdate()
FROM DUAL
WHERE @order_info_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:info:physicalDelete');

-- ------------------------------------------------------------
-- 5. 可改配页：改配提交（列表权限已存在）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '改配提交', @change_model_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:changeModel:edit', '#', 'admin', sysdate()
FROM DUAL
WHERE @change_model_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:changeModel:edit');

-- ------------------------------------------------------------
-- 6. 配车页：配车/换绑/解绑
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '分配车辆', @assign_vehicle_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:assignVehicle:assign', '#', 'admin', sysdate()
FROM DUAL
WHERE @assign_vehicle_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:assignVehicle:assign');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '换绑车辆', @assign_vehicle_menu, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:assignVehicle:reassign', '#', 'admin', sysdate()
FROM DUAL
WHERE @assign_vehicle_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:assignVehicle:reassign');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '解绑车辆', @assign_vehicle_menu, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:assignVehicle:unbind', '#', 'admin', sysdate()
FROM DUAL
WHERE @assign_vehicle_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:assignVehicle:unbind');

-- ------------------------------------------------------------
-- 7. 发运页：申请发运（换绑/解绑按钮挂在配车页菜单下，供发运页共用校验）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '申请发运', @transport_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:transport:apply', '#', 'admin', sysdate()
FROM DUAL
WHERE @transport_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:transport:apply');

-- ------------------------------------------------------------
-- 8. 分配交付人员页：绑定交付人员
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '分配交付人员', @assign_dp_menu, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'completeVehicle:order:assignDeliveryPerson:assign', '#', 'admin', sysdate()
FROM DUAL
WHERE @assign_dp_menu IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'completeVehicle:order:assignDeliveryPerson:assign');

-- ------------------------------------------------------------
-- 9. 授权
-- 9.1 已拥有对应页面菜单的角色，自动获得该页面下的订单域按钮权限
--     （避免逐角色维护，也保证新权限不会因漏授权而被前端隐藏）
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu parent ON rm.menu_id = parent.menu_id
INNER JOIN sys_menu child ON child.parent_id = parent.menu_id
WHERE child.menu_type = 'F'
  AND child.perms LIKE 'completeVehicle:order:%';

-- 9.2 购车协议页及其按钮：默认授予已拥有“订单信息”页面的角色（订单运营角色）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @agreement_menu
FROM sys_role_menu rm
WHERE @order_info_menu IS NOT NULL
  AND @agreement_menu IS NOT NULL
  AND rm.menu_id = @order_info_menu;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, child.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu child ON child.parent_id = @agreement_menu
WHERE @order_info_menu IS NOT NULL
  AND @agreement_menu IS NOT NULL
  AND rm.menu_id = @order_info_menu
  AND child.menu_type = 'F';

-- 9.3 超级管理员（1）与超级用户（100）兜底授权订单域全部菜单（含新页面/按钮）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
INNER JOIN sys_menu m ON (m.perms LIKE 'completeVehicle:order:%')
WHERE r.role_id IN (1, 100);

-- ============================================================
-- 迁移后期望的订单域权限集合（用于人工核对）
--   C 页面菜单：
--     completeVehicle:order:info:list                 订单信息
--     completeVehicle:order:changeModel:list          可改配订单
--     completeVehicle:order:assignVehicle:list        配车管理
--     completeVehicle:order:transport:list            发运管理
--     completeVehicle:order:assignDeliveryPerson:list 分配交付人员
--     completeVehicle:order:agreement:list            购车协议（本脚本新增）
--   F 按钮：
--     completeVehicle:order:info:query|audit|lock|close|physicalDelete
--     completeVehicle:order:changeModel:edit
--     completeVehicle:order:assignVehicle:assign|reassign|unbind
--     completeVehicle:order:transport:apply
--     completeVehicle:order:assignDeliveryPerson:assign
--     completeVehicle:order:agreement:edit|remove
--   已删除：assignVehicle:edit、info:remove、info:add、info:edit、info:export
-- 注意：菜单/权限变更后需重新登录或清理登录缓存，否则前端仍按旧的权限集合渲染。
-- ============================================================
