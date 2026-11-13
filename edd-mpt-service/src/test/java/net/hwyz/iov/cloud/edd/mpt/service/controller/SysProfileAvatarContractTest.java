package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import net.hwyz.iov.cloud.edd.mpt.api.model.LoginUser;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadCompleteVo;
import net.hwyz.iov.cloud.edd.mpt.service.domain.vo.AvatarUploadTicketVo;
import net.hwyz.iov.cloud.edd.mpt.service.exception.AvatarUploadException;
import net.hwyz.iov.cloud.edd.mpt.service.exception.AvatarUploadExceptionHandler;
import net.hwyz.iov.cloud.edd.mpt.service.service.IProfileAvatarService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ISysUserService;
import net.hwyz.iov.cloud.framework.common.constant.SecurityConstants;
import net.hwyz.iov.cloud.framework.security.service.TokenService;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.handler.ValidationExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人中心头像上传 HTTP 契约测试（EDD-MPT-DSN-CR-001 §5）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class SysProfileAvatarContractTest {

    private static final long OPERATOR_ID = 7L;

    private static final String USERNAME = "admin";

    private static final String AVATAR_URL = "https://bucket.cos.region.myqcloud.com/images/edd-mpt/obj-1.png";

    @Mock
    private IProfileAvatarService profileAvatarService;

    @Mock
    private ISysUserService userService;

    @Mock
    private TokenService tokenService;

    private MockMvc mockMvc;

    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        SysProfileController controller = new SysProfileController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "profileAvatarService", profileAvatarService);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ValidationExceptionHandler(), new AvatarUploadExceptionHandler())
                .build();

        loginUser = new LoginUser();
        loginUser.setUserid(OPERATOR_ID);
        loginUser.setUsername(USERNAME);
        loginUser.setSysUser(new SysUser());
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    @DisplayName("申请头像上传凭证返回 objectId / uploadUrl / uploadToken")
    void createTicketReturnsCredential() throws Exception {
        AvatarUploadTicketVo vo = new AvatarUploadTicketVo();
        vo.setObjectId("obj-1");
        vo.setUploadUrl("https://bucket.cos/put");
        vo.setHttpMethod("PUT");
        vo.setContentType("image/png");
        vo.setExpiresAt(1763000000000L);
        vo.setUploadToken("token-1");
        when(profileAvatarService.generateTicket(any(), eq(OPERATOR_ID))).thenReturn(vo);

        mockMvc.perform(post("/user/profile/avatar/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFilename\":\"avatar.png\",\"contentType\":\"image/png\",\"size\":32768}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.objectId").value("obj-1"))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://bucket.cos/put"))
                .andExpect(jsonPath("$.data.httpMethod").value("PUT"))
                .andExpect(jsonPath("$.data.uploadToken").value("token-1"));
    }

    @Test
    @DisplayName("申请头像上传凭证：参数校验失败由框架前置拦截，不进入业务层")
    void createTicketValidatesRequest() throws Exception {
        mockMvc.perform(post("/user/profile/avatar/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFilename\":\"  \",\"contentType\":\"image/png\",\"size\":32768}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("100001"))
                .andExpect(jsonPath("$.message").value("原始文件名不能为空"));

        mockMvc.perform(post("/user/profile/avatar/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFilename\":\"avatar.png\",\"contentType\":\"image/png\",\"size\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("100001"));

        verifyNoInteractions(profileAvatarService);
    }

    @Test
    @DisplayName("申请头像上传凭证：业务失败返回 HTTP 200 + code=500 + 可读消息")
    void createTicketMapsBusinessFailure() throws Exception {
        when(profileAvatarService.generateTicket(any(), eq(OPERATOR_ID)))
                .thenThrow(new AvatarUploadException("申请头像上传凭证失败，请稍后重试"));

        mockMvc.perform(post("/user/profile/avatar/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFilename\":\"avatar.png\",\"contentType\":\"image/png\",\"size\":32768}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("申请头像上传凭证失败，请稍后重试"));
    }

    @Test
    @DisplayName("完成头像上传返回 imgUrl 并刷新登录会话头像")
    void completeReturnsImgUrlAndRefreshesSession() throws Exception {
        AvatarUploadCompleteVo vo = new AvatarUploadCompleteVo();
        vo.setImgUrl(AVATAR_URL);
        vo.setObjectId("obj-1");
        vo.setContentType("image/png");
        vo.setSize(32768L);
        vo.setStatus("UPLOADED");
        when(profileAvatarService.complete("obj-1", "token-1", OPERATOR_ID, USERNAME)).thenReturn(vo);

        mockMvc.perform(post("/user/profile/avatar/tickets/obj-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadToken\":\"token-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.imgUrl").value(AVATAR_URL))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));

        assertThat(loginUser.getSysUser().getAvatar()).isEqualTo(AVATAR_URL);
        verify(tokenService).setLoginUser(loginUser);
    }

    @Test
    @DisplayName("完成头像上传：凭证无效时返回失败且不刷新会话头像")
    void completeMapsBusinessFailure() throws Exception {
        when(profileAvatarService.complete("obj-1", "token-1", OPERATOR_ID, USERNAME))
                .thenThrow(new AvatarUploadException("上传凭证无效、已过期或已使用"));

        mockMvc.perform(post("/user/profile/avatar/tickets/obj-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadToken\":\"token-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("上传凭证无效、已过期或已使用"));

        assertThat(loginUser.getSysUser().getAvatar()).isNull();
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("完成头像上传：uploadToken 缺失时由框架前置拦截")
    void completeValidatesUploadToken() throws Exception {
        mockMvc.perform(post("/user/profile/avatar/tickets/obj-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadToken\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("100001"))
                .andExpect(jsonPath("$.message").value("上传凭证不能为空"));

        verifyNoInteractions(profileAvatarService);
    }

    @Test
    @DisplayName("旧 multipart 头像接口已删除")
    void legacyAvatarEndpointRemoved() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatarfile", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/user/profile/avatar").file(file))
                .andExpect(status().isNotFound());

        verifyNoInteractions(profileAvatarService);
    }

}
