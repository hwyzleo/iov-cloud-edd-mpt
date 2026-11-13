package net.hwyz.iov.cloud.edd.mpt.service.service;

import net.hwyz.iov.cloud.edd.mpt.service.domain.dto.AvatarUploadTicketDto;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadCompleteVo;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadTicketVo;
import net.hwyz.iov.cloud.edd.mpt.service.exception.AvatarUploadException;
import net.hwyz.iov.cloud.edd.mpt.service.service.impl.ProfileAvatarServiceImpl;
import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 头像上传业务层单元测试（EDD-MPT-DSN-CR-001 §4、§5）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class ProfileAvatarServiceImplTest {

    private static final long BIND_TTL_SECONDS = 1800L;

    private static final long OPERATOR_ID = 7L;

    private static final String USERNAME = "admin";

    private static final String OBJECT_ID = "obj-1";

    private static final String BIND_KEY = "mpt:avatar:upload:" + OBJECT_ID;

    private static final String AVATAR_URL = "https://bucket.cos.region.myqcloud.com/images/edd-mpt/obj-1.png";

    @Mock
    private ImageService imageService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ISysUserService userService;

    private ProfileAvatarServiceImpl avatarService;

    @BeforeEach
    void setUp() {
        avatarService = new ProfileAvatarServiceImpl(imageService, redisTemplate, userService, BIND_TTL_SECONDS);
    }

    @Test
    @DisplayName("申请凭证成功：调用 OSS 并写入操作人归属绑定，不返回 objectKey")
    void generateTicketSuccess() {
        long expiresAt = System.currentTimeMillis() + 900_000L;
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenReturn(ImageUploadResponse.builder()
                        .objectId(OBJECT_ID)
                        .objectKey("images/edd-mpt/obj-1.png")
                        .uploadUrl("https://bucket.cos/put")
                        .httpMethod("PUT")
                        .contentType("image/png")
                        .expiresAt(expiresAt)
                        .build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AvatarUploadTicketVo vo = avatarService.generateTicket(ticket("avatar.png", " IMAGE/PNG ", 32768L), OPERATOR_ID);

        assertThat(vo.getObjectId()).isEqualTo(OBJECT_ID);
        assertThat(vo.getUploadUrl()).isEqualTo("https://bucket.cos/put");
        assertThat(vo.getHttpMethod()).isEqualTo("PUT");
        assertThat(vo.getContentType()).isEqualTo("image/png");
        assertThat(vo.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(vo.getUploadToken()).isNotBlank();

        verify(valueOperations).set(eq(BIND_KEY), eq(OPERATOR_ID + "|" + expiresAt),
                eq(BIND_TTL_SECONDS), eq(TimeUnit.SECONDS));

        ArgumentCaptor<GenerateImageUploadRequest> captor = ArgumentCaptor.forClass(GenerateImageUploadRequest.class);
        verify(imageService).generateUpload(captor.capture());
        assertThat(captor.getValue().getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(captor.getValue().getContentType()).isEqualTo("image/png");
        assertThat(captor.getValue().getSize()).isEqualTo(32768L);
    }

    @Test
    @DisplayName("申请凭证：参数非法时不调用 OSS")
    void generateTicketRejectsInvalidRequest() {
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("  ", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("原始文件名不能为空");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("a".repeat(256) + ".png", "image/png", 1024L),
                OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("原始文件名长度不能超过 255 字符");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("a.gif", "image/gif", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("仅支持 image/jpeg、image/png、image/webp");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.jpg", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("文件扩展名与 MIME 类型不一致");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("文件扩展名与 MIME 类型不一致");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 0L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("图片大小必须在 1 字节至 10 MiB 之间");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 10485761L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("图片大小必须在 1 字节至 10 MiB 之间");
        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 1024L), null))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("未获取到当前登录操作人，拒绝申请头像上传凭证");

        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("申请凭证：OSS 未返回有效上传任务时失败")
    void generateTicketFailsWhenOssReturnsInvalidTask() {
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenReturn(ImageUploadResponse.builder().objectId(OBJECT_ID).build());

        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("申请头像上传凭证失败，请稍后重试");
    }

    @Test
    @DisplayName("申请凭证：OSS 调用异常不暴露内部信息")
    void generateTicketFailsWhenOssUnavailable() {
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenThrow(new IllegalStateException("图片上传服务不可用，无法创建上传任务"));

        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("申请头像上传凭证失败，请稍后重试");
    }

    @Test
    @DisplayName("申请凭证：Redis 绑定写入失败时拒绝申请，不得无绑定降级")
    void generateTicketFailsWhenBindFails() {
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenReturn(ImageUploadResponse.builder()
                        .objectId(OBJECT_ID)
                        .uploadUrl("https://bucket.cos/put")
                        .expiresAt(System.currentTimeMillis() + 900_000L)
                        .build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThatThrownBy(() -> avatarService.generateTicket(ticket("avatar.png", "image/png", 1024L), OPERATOR_ID))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("头像上传凭证绑定失败");
    }

    @Test
    @DisplayName("完成确认成功：更新 sys_user.avatar 并消费绑定")
    void completeSuccess() {
        long expireAt = System.currentTimeMillis() + 900_000L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(OPERATOR_ID + "|" + expireAt);
        when(imageService.completeUpload(OBJECT_ID)).thenReturn(uploadedObject());
        when(userService.updateUserAvatar(USERNAME, AVATAR_URL)).thenReturn(true);

        AvatarUploadCompleteVo vo = avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME);

        assertThat(vo.getImgUrl()).isEqualTo(AVATAR_URL);
        assertThat(vo.getObjectId()).isEqualTo(OBJECT_ID);
        assertThat(vo.getContentType()).isEqualTo("image/png");
        assertThat(vo.getSize()).isEqualTo(32768L);
        assertThat(vo.getStatus()).isEqualTo("UPLOADED");
        verify(redisTemplate).delete(BIND_KEY);
    }

    @Test
    @DisplayName("完成确认：绑定缺失、已过期或已使用时拒绝且不调用 OSS")
    void completeRejectsMissingOrExpiredBinding() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(null);

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("上传凭证无效、已过期或已使用");

        when(valueOperations.get(BIND_KEY)).thenReturn(OPERATOR_ID + "|" + (System.currentTimeMillis() - 1000L));

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("上传凭证无效、已过期或已使用");

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, " ", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("上传凭证无效、已过期或已使用");

        verifyNoInteractions(imageService);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("完成确认：凭证与当前操作人不一致时拒绝")
    void completeRejectsOtherOperator() {
        long expireAt = System.currentTimeMillis() + 900_000L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(8L + "|" + expireAt);

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("上传凭证与当前操作人不一致");

        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("完成确认：Redis 读取异常时 fail-closed")
    void completeFailsClosedWhenRedisUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenThrow(new IllegalStateException("redis down"));

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("头像上传凭证校验失败，请稍后重试");

        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("完成确认：对象未达 UPLOADED 时失败且不更新头像、不消费绑定")
    void completeFailsWhenObjectNotUploaded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(OPERATOR_ID + "|" + (System.currentTimeMillis() + 900_000L));
        when(imageService.completeUpload(OBJECT_ID)).thenReturn(ImageObjectResponse.builder()
                .objectId(OBJECT_ID)
                .status("PENDING")
                .build());

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("完成头像上传确认失败，请重新上传");

        verifyNoInteractions(userService);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("完成确认：OSS 调用异常时失败且保留绑定")
    void completeFailsWhenOssUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(OPERATOR_ID + "|" + (System.currentTimeMillis() + 900_000L));
        when(imageService.completeUpload(OBJECT_ID)).thenThrow(new IllegalStateException("oss down"));

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("完成头像上传确认失败，请重新上传");

        verifyNoInteractions(userService);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("完成确认：头像更新失败时返回失败且不消费绑定")
    void completeFailsWhenAvatarUpdateFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BIND_KEY)).thenReturn(OPERATOR_ID + "|" + (System.currentTimeMillis() + 900_000L));
        when(imageService.completeUpload(OBJECT_ID)).thenReturn(uploadedObject());
        when(userService.updateUserAvatar(USERNAME, AVATAR_URL)).thenReturn(false);

        assertThatThrownBy(() -> avatarService.complete(OBJECT_ID, "token-1", OPERATOR_ID, USERNAME))
                .isInstanceOf(AvatarUploadException.class)
                .hasMessage("头像更新失败，请稍后重试");

        verify(redisTemplate, never()).delete(anyString());
    }

    private ImageObjectResponse uploadedObject() {
        return ImageObjectResponse.builder()
                .objectId(OBJECT_ID)
                .accessUrl(AVATAR_URL)
                .actualContentType("image/png")
                .actualSize(32768L)
                .status("UPLOADED")
                .build();
    }

    private AvatarUploadTicketDto ticket(String originalFilename, String contentType, long size) {
        AvatarUploadTicketDto dto = new AvatarUploadTicketDto();
        dto.setOriginalFilename(originalFilename);
        dto.setContentType(contentType);
        dto.setSize(size);
        return dto;
    }

}
