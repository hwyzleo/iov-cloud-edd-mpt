package net.hwyz.iov.cloud.edd.mpt.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 服务网关返回的业务错误。
 *
 * @author hwyz_leo
 */
@Getter
public class SgwRemoteException extends RuntimeException {

    private final String code;

    private final HttpStatus httpStatus;

    public SgwRemoteException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

}
