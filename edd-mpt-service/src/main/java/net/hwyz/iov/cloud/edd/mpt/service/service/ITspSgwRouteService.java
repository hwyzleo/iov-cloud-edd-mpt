package net.hwyz.iov.cloud.edd.mpt.service.service;

import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;

import java.util.List;

/**
 * TSP服务网关路由业务层
 *
 * @author hwyz_leo
 */
public interface ITspSgwRouteService {
    /**
     * 根据条件分页查询路由数据
     *
     * @param route 路由信息
     * @return 路由数据集合信息
     */
    List<TspSgwRoute> selectRouteList(TspSgwRoute route);

    /**
     * 通过角色ID查询路由
     *
     * @param routeId 路由ID
     * @return 路由对象信息
     */
    TspSgwRoute selectRouteById(Long routeId);

    /**
     * 新增保存路由信息
     *
     * @param route 路由信息
     * @return 结果
     */
    int insertRoute(TspSgwRoute route);

    /**
     * 修改保存路由信息
     *
     * @param route 路由信息
     * @return 结果
     */
    int updateRoute(TspSgwRoute route);

    /**
     * 通过路由ID删除路由
     *
     * @param routeId 路由ID
     * @return 结果
     */
    int deleteRouteById(Long routeId);

    /**
     * 批量删除路由信息
     *
     * @param routeIds 需要删除的路由ID
     * @return 结果
     */
    int deleteRouteByIds(Long[] routeIds);

}
