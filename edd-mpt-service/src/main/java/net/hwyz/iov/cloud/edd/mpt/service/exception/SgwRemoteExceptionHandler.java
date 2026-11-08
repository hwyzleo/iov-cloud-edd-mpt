package net.hwyz.iov.cloud.edd.mpt.service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 服务网关调用错误响应映射。
 *
 * @author hwyz_leo
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SgwRemoteExceptionHandler {

    /**
     * 映射服务网关业务错误
     *
     * @param exception 异常
     * @return 响应
     */
    @ExceptionHandler(SgwRemoteException.class)
    public ResponseEntity<ApiResponse<Void>> handleRemote(SgwRemoteException exception) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(body(exception.getCode(), exception.getMessage()));
    }

    /**
     * 映射服务网关不可用
     *
     * @param exception 异常
     * @return 响应
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeign(FeignException exception) {
        log.error("服务网关调用失败", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body("SGW-503001", "服务网关不可用"));
    }

    private ApiResponse<Void> body(String code, String message) {
        return ApiResponse.<Void>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
