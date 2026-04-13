package net.hwyz.iov.cloud.edd.mpt.api.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;
import org.apache.commons.lang3.StringUtils;

/**
 * 未能通过的角色认证异常
 *
 * @author hwyz_leo
 */
public class NotRoleException extends BaseException {

    public NotRoleException(String role) {
        super(role);
    }

    public NotRoleException(String[] roles) {
        super(StringUtils.join(roles, ","));
    }
}
