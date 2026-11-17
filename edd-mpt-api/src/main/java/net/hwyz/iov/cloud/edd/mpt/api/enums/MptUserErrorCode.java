package net.hwyz.iov.cloud.edd.mpt.api.enums;

import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 账号与员工绑定相关错误码（EDD-MPT-DSN-CR-002）
 * <p>
 * 供内部契约使用：EIAM 通过响应 code 区分"未绑定""工号冲突""账号停用""必须改密"等场景。
 *
 * @author hwyz_leo
 */
public enum MptUserErrorCode implements ErrorCode {

    /**
     * 该员工尚未绑定账号
     */
    USER_BINDING_MISSING("100101", "该员工尚未绑定账号，请先在用户管理中绑定"),

    /**
     * 工号已被其他账号占用
     */
    USER_NAME_CONFLICT("100102", "工号已被其他账号占用，请人工处理"),

    /**
     * 账号已停用或删除
     */
    USER_DISABLED("100103", "账号已停用或已删除"),

    /**
     * 初始密码未修改
     */
    PWD_RESET_REQUIRED("100104", "初始密码未修改，请先修改密码");

    private final String code;

    private final String message;

    MptUserErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
