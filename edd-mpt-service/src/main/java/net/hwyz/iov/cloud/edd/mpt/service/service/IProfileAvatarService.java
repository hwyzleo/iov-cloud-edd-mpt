package net.hwyz.iov.cloud.edd.mpt.service.service;

import net.hwyz.iov.cloud.edd.mpt.service.domain.dto.AvatarUploadTicketDto;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadCompleteVo;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadTicketVo;

/**
 * 个人头像上传 业务层
 * <p>
 * MPT 作为 EDD-OSS 的场景化薄封装：只签发预签名凭证并做完成确认，不中转图片二进制、
 * 不持有 COS 长期凭证，仅把公开访问 URL 写入 sys_user.avatar（EDD-MPT-DSN-CR-001 §4）。
 *
 * @author hwyz_leo
 */
public interface IProfileAvatarService {

    /**
     * 申请头像上传凭证：参数校验 → 调用 EDD-OSS 创建上传任务 → 写入操作人归属绑定
     *
     * @param dto        申请请求
     * @param operatorId 当前登录用户标识（来自登录态）
     * @return 上传凭证
     */
    AvatarUploadTicketVo generateTicket(AvatarUploadTicketDto dto, Long operatorId);

    /**
     * 完成头像上传确认：校验凭证绑定 → 调用 EDD-OSS 完成确认 → 更新头像并消费绑定
     *
     * @param objectId    上传任务标识
     * @param uploadToken 上传凭证
     * @param operatorId  当前登录用户标识（来自登录态）
     * @param username    当前登录账号（用于更新 sys_user.avatar）
     * @return 完成确认结果
     */
    AvatarUploadCompleteVo complete(String objectId, String uploadToken, Long operatorId, String username);

}
