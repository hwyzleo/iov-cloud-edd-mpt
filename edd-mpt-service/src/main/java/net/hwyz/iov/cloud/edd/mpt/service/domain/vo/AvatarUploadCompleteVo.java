package net.hwyz.iov.cloud.edd.mpt.service.domain.vo;

import lombok.Data;

/**
 * 头像上传完成确认响应（EDD-MPT-DSN-CR-001 §3.3、§5.2）
 * <p>
 * imgUrl 为已写入 sys_user.avatar 的 EDD-OSS 公开访问地址，仅在对象状态达到 UPLOADED 时返回。
 *
 * @author hwyz_leo
 */
@Data
public class AvatarUploadCompleteVo {

    /**
     * 头像公开访问地址
     */
    private String imgUrl;

    /**
     * 上传任务标识
     */
    private String objectId;

    /**
     * 实际 MIME
     */
    private String contentType;

    /**
     * 实际字节数
     */
    private Long size;

    /**
     * 对象状态，正常为 UPLOADED
     */
    private String status;

}
