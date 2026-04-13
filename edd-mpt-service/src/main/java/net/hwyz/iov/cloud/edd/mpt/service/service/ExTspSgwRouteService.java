package net.hwyz.iov.cloud.edd.mpt.service.service;

import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.service.factory.ExTspSgwRouteServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务网关路由相关接口
 *
 * @author hwyz_leo
 */
@FeignClient(name = "tsp-sgw", path = "/route", fallbackFactory = ExTspSgwRouteServiceFallbackFactory.class)
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

}
