package net.hwyz.iov.cloud.edd.mpt.service.service;

import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.service.factory.ExTspSgwRouteServiceFallbackFactory;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务网关路由相关接口
 *
 * @author hwyz_leo
 */
@FeignClient(name = ServiceNameConstants.EDD_SGW, path = "/route", fallbackFactory = ExTspSgwRouteServiceFallbackFactory.class)
public interface ExTspSgwRouteService {

    /**
     * 新增路由
     *
     * @param route 路由
     */
    @PostMapping
    void add(@RequestBody @Validated TspSgwRoute route);

    /**
     * 更新路由
     *
     * @param route 路由
     */
    @PutMapping
    void update(@RequestBody @Validated TspSgwRoute route);

    /**
     * 删除路由
     *
     * @param ids 路由ID列表
     */
    @DeleteMapping("/{ids}")
    void delete(@PathVariable Long[] ids);

    /**
     * 刷新路由
     * 从数据库重新加载所有路由配置，使增删改过的路由实时生效
     */
    @PostMapping("/refresh")
    void refresh();

}
