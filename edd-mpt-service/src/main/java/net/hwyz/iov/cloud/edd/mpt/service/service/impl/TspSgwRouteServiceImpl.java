package net.hwyz.iov.cloud.edd.mpt.service.service.impl;

import net.hwyz.iov.cloud.framework.datascope.annotation.DataScope;
import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.SysRoleDeptMapper;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.SysRoleMenuMapper;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.TspSgwRouteMapper;
import net.hwyz.iov.cloud.edd.mpt.service.service.ITspSgwRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色 业务层处理
 *
 * @author hwyz_leo
 */
@Service
public class TspSgwRouteServiceImpl implements ITspSgwRouteService {

    @Autowired
    private TspSgwRouteMapper routeMapper;

    /**
     * 根据条件分页查询路由数据
     *
     * @param route 路由信息
     * @return 路由数据集合信息
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<TspSgwRoute> selectRouteList(TspSgwRoute route) {
        return routeMapper.selectRouteList(route);
    }

    /**
     * 通过路由ID查询路由
     *
     * @param routeId 路由ID
     * @return 路由对象信息
     */
    @Override
    public TspSgwRoute selectRouteById(Long routeId) {
        return routeMapper.selectRouteById(routeId);
    }

    /**
     * 新增保存角色信息
     *
     * @param route 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRoute(TspSgwRoute route) {
        return routeMapper.insertRoute(route);
    }

    /**
     * 修改保存路由信息
     *
     * @param route 路由信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoute(TspSgwRoute route) {
        return routeMapper.updateRoute(route);
    }

    @Override
    public int deleteRouteById(Long routeId) {
        return 0;
    }

    /**
     * 批量删除路由信息
     *
     * @param routeIds 需要删除的路由ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRouteByIds(Long[] routeIds) {
        return routeMapper.deleteRoleByIds(routeIds);
    }

}
