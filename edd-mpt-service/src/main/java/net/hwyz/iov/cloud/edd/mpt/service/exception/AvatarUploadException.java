package net.hwyz.iov.cloud.edd.mpt.service.exception;

/**
 * 头像上传业务异常（EDD-MPT-DSN-CR-001 §5.1/§5.2）
 * <p>
 * 用于参数校验兜底、EDD-OSS 调用失败、Redis 绑定不可用、对象状态未达 UPLOADED 与头像更新失败等场景；
 * 由 {@link AvatarUploadExceptionHandler} 统一映射为 HTTP 200 + code=500 + 可读消息。
 *
 * @author hwyz_leo
 */
public class AvatarUploadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AvatarUploadException(String message) {
        super(message);
    }

}
