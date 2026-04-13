package net.hwyz.iov.cloud.edd.mpt.api.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;
import org.apache.commons.lang3.StringUtils;

/**
 * 未能通过的权限认证异常
 *
 * @author hwyz_leo
 */
public class NotPermissionException extends BaseException {

    public NotPermissionException(String permission) {
        super(permission);
    }

    public NotPermissionException(String[] permissions) {
        super(StringUtils.join(permissions, ","));
    }
}
