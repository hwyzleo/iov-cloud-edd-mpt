package net.hwyz.iov.cloud.edd.mpt.service.util;

import net.hwyz.iov.cloud.edd.mpt.api.model.LoginUser;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;

/**
 * 员工上下文工具（EDD-MPT-DSN-CR-002）
 * <p>
 * 从当前登录会话读取 ORG 员工ID。framework-security 目前未提供该方法（其发布链路独立于本 CR），
 * 因此先在 MPT 服务内提供；待 framework 侧提供同等能力后收敛到 `SecurityUtils.getEmployeeId()`。
 *
 * @author hwyz_leo
 */
public final class EmployeeContext {

    private EmployeeContext() {
    }

    /**
     * 获取当前登录用户关联的 ORG 员工ID
     *
     * @return 员工ID；未登录或未绑定账号时返回 null
     */
    public static Long getEmployeeId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser == null ? null : loginUser.getEmployeeId();
    }
}
