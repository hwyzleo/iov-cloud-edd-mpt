package net.hwyz.iov.cloud.edd.mpt.service.exception;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.AjaxResult;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 头像上传异常映射（EDD-MPT-DSN-CR-001 §5.1/§5.2）
 * <p>
 * 设计约定业务失败返回 HTTP 200 + code=500 + 可读消息；消息中不含 uploadUrl、uploadToken 与 COS 凭证。
 *
 * @author hwyz_leo
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AvatarUploadExceptionHandler {

    /**
     * 映射头像上传业务错误
     *
     * @param exception 头像上传异常
     * @return 平台统一响应
     */
    @ExceptionHandler(AvatarUploadException.class)
    public AjaxResult handleAvatarUploadException(AvatarUploadException exception) {
        log.warn("[头像上传] 业务失败: {}", exception.getMessage());
        return AjaxResult.error(exception.getMessage());
    }

}
