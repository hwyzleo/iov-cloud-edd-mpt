package net.hwyz.iov.cloud.edd.mpt.api.factory;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mpt.api.RemoteFileService;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysFile;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class RemoteFileFallbackFactory implements FallbackFactory<RemoteFileService> {

    @Override
    public RemoteFileService create(Throwable throwable) {
        log.error("文件服务调用失败:{}", throwable.getMessage());
        return new RemoteFileService() {
            @Override
            public ApiResponse<SysFile> upload(MultipartFile file) {
                return ApiResponse.fail("上传文件失败:" + throwable.getMessage());
            }
        };
    }
}
