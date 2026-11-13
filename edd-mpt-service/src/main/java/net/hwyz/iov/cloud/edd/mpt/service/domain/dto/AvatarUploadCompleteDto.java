package net.hwyz.iov.cloud.edd.mpt.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 头像上传完成确认请求（EDD-MPT-DSN-CR-001 §3.3、§5.2）
 * <p>
 * 凭证放请求体而非 query，避免进入访问日志。
 *
 * @author hwyz_leo
 */
@Data
public class AvatarUploadCompleteDto {

    /**
     * 申请上传凭证时返回的 uploadToken
     */
    @NotBlank(message = "上传凭证不能为空")
    private String uploadToken;

}
