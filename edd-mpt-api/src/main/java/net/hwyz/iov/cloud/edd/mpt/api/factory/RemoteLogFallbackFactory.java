package net.hwyz.iov.cloud.edd.mpt.api.factory;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.edd.mpt.api.RemoteLogService;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysLogininfor;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysOperLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 日志服务降级处理
 *
 * @author hwyz_leo
 */
@Component
public class RemoteLogFallbackFactory implements FallbackFactory<RemoteLogService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteLogFallbackFactory.class);

    @Override
    public RemoteLogService create(Throwable throwable) {
        log.error("日志服务调用失败:{}", throwable.getMessage());
        return new RemoteLogService() {
            @Override
            public ApiResponse<Boolean> saveLog(SysOperLog sysOperLog, String source) {
                return ApiResponse.fail("保存操作日志失败:" + throwable.getMessage());
            }

            @Override
            public ApiResponse<Boolean> saveLogininfor(SysLogininfor sysLogininfor, String source) {
                return ApiResponse.fail("保存登录日志失败:" + throwable.getMessage());
            }
        };

    }
}
