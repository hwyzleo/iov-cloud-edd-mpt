package net.hwyz.iov.cloud.edd.mpt.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * MPT 模块错误码枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum MptErrorCode implements ErrorCode {

    UNAUTHORIZED("802002", "未认证"),
    FORBIDDEN("802003", "无权限");

    private final String code;
    private final String message;

}