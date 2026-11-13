package net.hwyz.iov.cloud.edd.mpt.service.service.impl;

import cn.hutool.core.util.IdUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mpt.service.domain.dto.AvatarUploadTicketDto;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadCompleteVo;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadTicketVo;
import net.hwyz.iov.cloud.edd.mpt.service.exception.AvatarUploadException;
import net.hwyz.iov.cloud.edd.mpt.service.service.IProfileAvatarService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ISysUserService;
import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 个人头像上传 业务层处理（EDD-MPT-DSN-CR-001）
 * <p>
 * 上传凭证与操作人绑定在 Redis，完成确认前必须校验归属；Redis 不可用时 fail-closed，禁止无绑定降级。
 * 日志只记录 objectId、操作人与尺寸，不记录预签名地址与 uploadToken。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class ProfileAvatarServiceImpl implements IProfileAvatarService {

    /**
     * Redis 上传凭证绑定键前缀，完整键：mpt:avatar:upload:{objectId}
     */
    private static final String BIND_KEY_PREFIX = "mpt:avatar:upload:";

    /**
     * 绑定值分隔符：operatorId|expireAtMillis；分隔符不输出到日志
     */
    private static final String BIND_VALUE_SEPARATOR = "|";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /**
     * 扩展名与 MIME 映射，用于前置校验声明一致性
     */
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg jpeg",
            "image/png", "png",
            "image/webp", "webp");

    private static final long MAX_AVATAR_SIZE_BYTES = 10L * 1024 * 1024;

    private static final long MAX_FILENAME_LENGTH = 255;

    private static final String STATUS_UPLOADED = "UPLOADED";

    private static final String DEFAULT_HTTP_METHOD = "PUT";

    private final ImageService imageService;

    private final StringRedisTemplate redisTemplate;

    private final ISysUserService userService;

    /**
     * 上传凭证绑定有效期（秒），不短于预签名有效期
     */
    private final long bindTtlSeconds;

    public ProfileAvatarServiceImpl(ImageService imageService,
                                    StringRedisTemplate redisTemplate,
                                    ISysUserService userService,
                                    @Value("${mpt.avatar.upload.bind-ttl-seconds:1800}") long bindTtlSeconds) {
        this.imageService = imageService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.bindTtlSeconds = bindTtlSeconds;
    }

    @Override
    public AvatarUploadTicketVo generateTicket(AvatarUploadTicketDto dto, Long operatorId) {
        if (dto == null || operatorId == null) {
            throw new AvatarUploadException("未获取到当前登录操作人，拒绝申请头像上传凭证");
        }
        String originalFilename = StrUtil.trim(dto.getOriginalFilename());
        String contentType = normalizeContentType(dto.getContentType());
        validateTicketRequest(originalFilename, contentType, dto.getSize());

        ImageUploadResponse upload = requestUpload(originalFilename, contentType, dto.getSize(), operatorId);
        if (StrUtil.isBlank(upload.getObjectId()) || StrUtil.isBlank(upload.getUploadUrl())) {
            log.error("[头像上传] OSS 未返回有效上传任务 operatorId[{}] filename[{}] contentType[{}] size[{}]",
                    operatorId, originalFilename, contentType, dto.getSize());
            throw new AvatarUploadException("申请头像上传凭证失败，请稍后重试");
        }

        String uploadToken = IdUtil.fastSimpleUUID();
        bindOperator(upload.getObjectId(), operatorId, upload.getExpiresAt());

        AvatarUploadTicketVo vo = new AvatarUploadTicketVo();
        vo.setObjectId(upload.getObjectId());
        vo.setUploadUrl(upload.getUploadUrl());
        vo.setHttpMethod(StrUtil.isBlank(upload.getHttpMethod()) ? DEFAULT_HTTP_METHOD : upload.getHttpMethod());
        vo.setContentType(StrUtil.isBlank(upload.getContentType()) ? contentType : upload.getContentType());
        vo.setExpiresAt(upload.getExpiresAt());
        vo.setUploadToken(uploadToken);
        log.info("[头像上传] 上传凭证签发成功 objectId[{}] operatorId[{}] filename[{}] contentType[{}] size[{}]",
                upload.getObjectId(), operatorId, originalFilename, contentType, dto.getSize());
        return vo;
    }

    @Override
    public AvatarUploadCompleteVo complete(String objectId, String uploadToken, Long operatorId, String username) {
        if (operatorId == null || StrUtil.isBlank(username)) {
            throw new AvatarUploadException("未获取到当前登录操作人，拒绝完成头像上传确认");
        }
        if (StrUtil.isBlank(objectId) || StrUtil.isBlank(uploadToken)) {
            throw new AvatarUploadException("上传凭证无效、已过期或已使用");
        }
        String bindKey = BIND_KEY_PREFIX + objectId;
        verifyBinding(bindKey, operatorId);

        ImageObjectResponse confirmed = requestComplete(objectId, operatorId);
        if (confirmed == null || StrUtil.isBlank(confirmed.getAccessUrl())
                || !STATUS_UPLOADED.equalsIgnoreCase(confirmed.getStatus())) {
            log.error("[头像上传] 完成确认未达 UPLOADED objectId[{}] operatorId[{}] status[{}]",
                    objectId, operatorId, confirmed == null ? null : confirmed.getStatus());
            throw new AvatarUploadException("完成头像上传确认失败，请重新上传");
        }

        String imgUrl = confirmed.getAccessUrl();
        if (!userService.updateUserAvatar(username, imgUrl)) {
            // 头像未更新时保留绑定，允许同一 objectId 重试完成确认
            log.error("[头像上传] 头像更新失败 objectId[{}] operatorId[{}]", objectId, operatorId);
            throw new AvatarUploadException("头像更新失败，请稍后重试");
        }

        consumeBinding(bindKey, objectId, operatorId);

        AvatarUploadCompleteVo vo = new AvatarUploadCompleteVo();
        vo.setImgUrl(imgUrl);
        vo.setObjectId(StrUtil.isBlank(confirmed.getObjectId()) ? objectId : confirmed.getObjectId());
        vo.setContentType(confirmed.getActualContentType());
        vo.setSize(confirmed.getActualSize());
        vo.setStatus(confirmed.getStatus());
        log.info("[头像上传] 完成确认成功 objectId[{}] operatorId[{}] actualContentType[{}] actualSize[{}]",
                objectId, operatorId, confirmed.getActualContentType(), confirmed.getActualSize());
        return vo;
    }

    /**
     * 调用 EDD-OSS 创建上传任务；异常统一转业务错误，不回显内部堆栈与预签名地址
     */
    private ImageUploadResponse requestUpload(String originalFilename, String contentType, Long size, Long operatorId) {
        try {
            return imageService.generateUpload(GenerateImageUploadRequest.builder()
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .size(size)
                    .build());
        } catch (RuntimeException exception) {
            log.error("[头像上传] 调用 OSS 申请上传凭证失败 operatorId[{}] filename[{}] contentType[{}] size[{}] ossStatus[{}]",
                    operatorId, originalFilename, contentType, size, ossStatus(exception));
            throw new AvatarUploadException("申请头像上传凭证失败，请稍后重试");
        }
    }

    /**
     * 调用 EDD-OSS 完成确认；失败时保留绑定键以允许同一 objectId 重试
     */
    private ImageObjectResponse requestComplete(String objectId, Long operatorId) {
        try {
            return imageService.completeUpload(objectId);
        } catch (RuntimeException exception) {
            log.error("[头像上传] 调用 OSS 完成确认失败 objectId[{}] operatorId[{}] ossStatus[{}]",
                    objectId, operatorId, ossStatus(exception));
            throw new AvatarUploadException("完成头像上传确认失败，请重新上传");
        }
    }

    /**
     * 写入操作人归属绑定；Redis 异常时拒绝申请，禁止无绑定降级
     */
    private void bindOperator(String objectId, Long operatorId, Long expireAt) {
        long expireAtMillis = expireAt == null ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(bindTtlSeconds)
                : expireAt;
        String bindValue = operatorId + BIND_VALUE_SEPARATOR + expireAtMillis;
        try {
            redisTemplate.opsForValue().set(BIND_KEY_PREFIX + objectId, bindValue, bindTtlSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException exception) {
            log.error("[头像上传] 上传凭证绑定失败 objectId[{}] operatorId[{}]", objectId, operatorId);
            throw new AvatarUploadException("头像上传凭证绑定失败");
        }
    }

    /**
     * 校验绑定：凭证存在、操作人一致、未过期；读取失败拒绝确认（fail-closed）
     */
    private void verifyBinding(String bindKey, Long operatorId) {
        String bindValue;
        try {
            bindValue = redisTemplate.opsForValue().get(bindKey);
        } catch (RuntimeException exception) {
            log.error("[头像上传] 读取上传凭证绑定失败 bindKey[{}] operatorId[{}]", bindKey, operatorId);
            throw new AvatarUploadException("头像上传凭证校验失败，请稍后重试");
        }
        if (StrUtil.isBlank(bindValue)) {
            throw new AvatarUploadException("上传凭证无效、已过期或已使用");
        }
        int separatorIndex = bindValue.lastIndexOf(BIND_VALUE_SEPARATOR);
        if (separatorIndex <= 0) {
            throw new AvatarUploadException("上传凭证无效、已过期或已使用");
        }
        String boundOperatorId = bindValue.substring(0, separatorIndex);
        long expireAt = parseExpireAt(bindValue.substring(separatorIndex + BIND_VALUE_SEPARATOR.length()));
        if (!String.valueOf(operatorId).equals(boundOperatorId)) {
            log.warn("[头像上传] 上传凭证操作人不一致 bindKey[{}] operatorId[{}]", bindKey, operatorId);
            throw new AvatarUploadException("上传凭证与当前操作人不一致");
        }
        if (System.currentTimeMillis() > expireAt) {
            throw new AvatarUploadException("上传凭证无效、已过期或已使用");
        }
    }

    /**
     * 成功后消费绑定键（单次使用）
     */
    private void consumeBinding(String bindKey, String objectId, Long operatorId) {
        try {
            redisTemplate.delete(bindKey);
        } catch (RuntimeException exception) {
            // 头像已更新成功，不回滚结果；绑定键残留由 TTL 兜底
            log.warn("[头像上传] 上传凭证绑定删除失败 objectId[{}] operatorId[{}]", objectId, operatorId);
        }
    }

    private long parseExpireAt(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new AvatarUploadException("上传凭证无效、已过期或已使用");
        }
    }

    private String normalizeContentType(String contentType) {
        return StrUtil.isBlank(contentType) ? null : contentType.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 服务层兜底校验（Controller 侧已由 Bean Validation 前置拦截），不通过不得调用 EDD-OSS
     */
    private void validateTicketRequest(String originalFilename, String contentType, Long size) {
        if (StrUtil.isBlank(originalFilename)) {
            throw new AvatarUploadException("原始文件名不能为空");
        }
        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            throw new AvatarUploadException("原始文件名长度不能超过 255 字符");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AvatarUploadException("仅支持 image/jpeg、image/png、image/webp");
        }
        if (!extensionMatchesContentType(originalFilename, contentType)) {
            throw new AvatarUploadException("文件扩展名与 MIME 类型不一致");
        }
        if (size == null || size < 1 || size > MAX_AVATAR_SIZE_BYTES) {
            throw new AvatarUploadException("图片大小必须在 1 字节至 10 MiB 之间");
        }
    }

    /**
     * 校验文件扩展名与声明 MIME 是否一致，避免 EDD-OSS 侧 MIME 与扩展名不匹配被拒
     */
    private boolean extensionMatchesContentType(String originalFilename, String contentType) {
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return false;
        }
        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        String allowedExtensions = CONTENT_TYPE_EXTENSIONS.get(contentType);
        return allowedExtensions != null && StrUtil.split(allowedExtensions, ' ').contains(extension);
    }

    /**
     * 提取 OSS 返回码用于日志，仅记录状态码，不记录响应体与预签名地址
     */
    private Integer ossStatus(Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < 5; depth++) {
            if (current instanceof FeignException feignException) {
                return feignException.status();
            }
            current = current.getCause();
        }
        return null;
    }

}
