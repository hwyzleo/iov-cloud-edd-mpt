-- 删除车辆零件管理菜单及其子菜单和权限
-- 使用JOIN方式避免MySQL子查询限制

-- 首先删除角色菜单关联（父菜单）
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.menu_name = '车辆零件管理' AND m.path = 'vehiclePart';

-- 删除角色菜单关联（子菜单）
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
INNER JOIN sys_menu parent ON m.parent_id = parent.menu_id
WHERE parent.menu_name = '车辆零件管理' AND parent.path = 'vehiclePart';

-- 删除子菜单
DELETE m FROM sys_menu m
INNER JOIN sys_menu parent ON m.parent_id = parent.menu_id
WHERE parent.menu_name = '车辆零件管理' AND parent.path = 'vehiclePart';

-- 删除车辆零件管理菜单
DELETE FROM sys_menu WHERE menu_name = '车辆零件管理' AND path = 'vehiclePart';
