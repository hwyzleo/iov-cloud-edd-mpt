package net.hwyz.iov.cloud.edd.mpt.service.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 头像上传凭证申请请求（EDD-MPT-DSN-CR-001 §3.3、§5.1）
 * <p>
 * 申请阶段由 MPT 前置校验，避免无效请求打到 EDD-OSS；最终以 EDD-OSS 对 COS 实际对象的核验结果为准。
 *
 * @author hwyz_leo
 */
@Data
public class AvatarUploadTicketDto {

    /**
     * 原始文件名，去空白后非空且不超过 255 字符
     */
    @NotBlank(message = "原始文件名不能为空")
    @Size(max = 255, message = "原始文件名长度不能超过 255 字符")
    private String originalFilename;

    /**
     * 声明 MIME，服务层限定 image/jpeg、image/png、image/webp
     */
    @NotBlank(message = "图片 MIME 类型不能为空")
    private String contentType;

    /**
     * 声明大小，1 ≤ size ≤ 10 MiB
     */
    @NotNull(message = "图片大小不能为空")
    @Min(value = 1, message = "图片大小必须在 1 字节至 10 MiB 之间")
    @Max(value = 10485760, message = "图片大小必须在 1 字节至 10 MiB 之间")
    private Long size;

}
