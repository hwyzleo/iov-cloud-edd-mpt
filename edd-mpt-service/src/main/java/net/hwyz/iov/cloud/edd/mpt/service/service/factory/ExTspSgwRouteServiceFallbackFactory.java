package net.hwyz.iov.cloud.edd.mpt.service.service.factory;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.service.ExTspSgwRouteService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 服务网关路由相关接口异常处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class ExTspSgwRouteServiceFallbackFactory implements FallbackFactory<ExTspSgwRouteService> {

    @Override
    public ExTspSgwRouteService create(Throwable throwable) {
        return new ExTspSgwRouteService() {
            @Override
            public void add(TspSgwRoute route) {
                logger.error("服务网关新增路由[{}]调用失败", route.getTargetUri(), throwable);
            }

            @Override
            public void update(TspSgwRoute route) {
                logger.error("服务网关更新路由[{}]调用失败", route.getTargetUri(), throwable);
            }

            @Override
            public void delete(Long[] ids) {
                logger.error("服务网关删除路由[{}]调用失败", Arrays.toString(ids), throwable);
            }
        };
    }
}
