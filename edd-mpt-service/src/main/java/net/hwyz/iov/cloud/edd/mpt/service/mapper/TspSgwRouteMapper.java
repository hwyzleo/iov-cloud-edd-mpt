package net.hwyz.iov.cloud.edd.mpt.service.mapper;

import net.hwyz.iov.cloud.edd.mpt.api.domain.SysRole;
import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;

import java.util.List;

/**
 * TSP服务网关路由表 数据层
 *
 * @author hwyz_leo
 */
public interface TspSgwRouteMapper {
    /**
     * 根据条件分页查询路由数据
     *
     * @param route 路由信息
     * @return 路由数据集合信息
     */
    List<TspSgwRoute> selectRouteList(TspSgwRoute route);

    /**
     * 通过路由ID查询路由
     *
     * @param routeId 路由ID
     * @return 路由对象信息
     */
    TspSgwRoute selectRouteById(Long routeId);

    /**
     * 修改路由信息
     *
     * @param route 路由信息
     * @return 结果
     */
    int updateRoute(TspSgwRoute route);

    /**
     * 新增路由信息
     *
     * @param route 路由信息
     * @return 结果
     */
    int insertRoute(TspSgwRoute route);

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    int deleteRoleById(Long roleId);

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    int deleteRoleByIds(Long[] roleIds);
}
