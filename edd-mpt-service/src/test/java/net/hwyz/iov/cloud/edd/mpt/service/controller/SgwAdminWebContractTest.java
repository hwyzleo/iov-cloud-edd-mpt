package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.mpt.service.exception.SgwRemoteException;
import net.hwyz.iov.cloud.edd.mpt.service.exception.SgwRemoteExceptionHandler;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwConfigVersionService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwExportService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwPolicyAdminService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwPublicationService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwRouteAdminService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwRuntimeService;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwUpstreamService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.RouteSaveRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ConfigVersionResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ExportResponseVo;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.PublicationResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.RouteResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.RuntimeInstanceResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.UpstreamOptionsResponse;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 服务网关管理端 HTTP 契约测试。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class SgwAdminWebContractTest {

    @Mock
    private SgwRouteAdminService routeAdminService;

    @Mock
    private SgwPolicyAdminService policyAdminService;

    @Mock
    private SgwConfigVersionService configVersionService;

    @Mock
    private SgwPublicationService publicationService;

    @Mock
    private SgwRuntimeService runtimeService;

    @Mock
    private SgwUpstreamService upstreamService;

    @Mock
    private SgwExportService exportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SgwRouteController(routeAdminService),
                        new SgwPolicyController(policyAdminService),
                        new SgwConfigVersionController(configVersionService),
                        new SgwPublicationController(publicationService),
                        new SgwRuntimeController(runtimeService),
                        new SgwUpstreamController(upstreamService),
                        new SgwExportController(exportService))
                .setControllerAdvice(new SgwRemoteExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("路由分页原样透传物理分页参数")
    void routeListPassesParameters() throws Exception {
        PageResult<RouteResponse> pageResult = PageResult.<RouteResponse>builder()
                .items(List.of(RouteResponse.builder().routeCode("vehicle-profile").build()))
                .total(1).page(2).size(5).build();
        when(routeAdminService.page(2, 5, "vehicle", true, "edd-vmd-service", "MOBILE"))
                .thenReturn(ApiResponse.ok(pageResult));

        mockMvc.perform(get("/sgw/routes/list")
                        .param("page", "2").param("size", "5").param("name", "vehicle")
                        .param("enabled", "true").param("target", "edd-vmd-service")
                        .param("clientType", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].routeCode").value("vehicle-profile"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("路由新增与复制转发到服务网关契约")
    void routeMutationsDelegate() throws Exception {
        when(routeAdminService.create(any(RouteSaveRequest.class)))
                .thenReturn(ApiResponse.ok(RouteResponse.builder().routeCode("r1").build()));
        when(routeAdminService.copy(eq("r1"), any()))
                .thenReturn(ApiResponse.ok(RouteResponse.builder().routeCode("r2").build()));

        mockMvc.perform(post("/sgw/routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeCode\":\"r1\",\"routeName\":\"r1\",\"priority\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeCode").value("r1"));

        mockMvc.perform(post("/sgw/routes/r1/copy").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newCode\":\"r2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeCode").value("r2"));

        ArgumentCaptor<RouteSaveRequest> captor = ArgumentCaptor.forClass(RouteSaveRequest.class);
        verify(routeAdminService).create(captor.capture());
        assertThat(captor.getValue().getRouteCode()).isEqualTo("r1");
    }

    @Test
    @DisplayName("路由更新使用路径编码并透传乐观锁版本")
    void routeUpdateUsesPathCode() throws Exception {
        when(routeAdminService.update(eq("r1"), any(RouteSaveRequest.class)))
                .thenReturn(ApiResponse.ok(RouteResponse.builder().routeCode("r1").rowVersion(4).build()));

        mockMvc.perform(put("/sgw/routes/r1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeCode\":\"r1\",\"routeName\":\"r1\",\"priority\":100,\"rowVersion\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rowVersion").value(4));

        ArgumentCaptor<RouteSaveRequest> captor = ArgumentCaptor.forClass(RouteSaveRequest.class);
        verify(routeAdminService).update(eq("r1"), captor.capture());
        assertThat(captor.getValue().getRowVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("发布与回滚转发并携带草稿修订号")
    void publishAndRollbackDelegate() throws Exception {
        when(configVersionService.publish(any()))
                .thenReturn(ApiResponse.ok(PublicationResponse.builder()
                        .publicationId("p1").targetVersionNo(2L).status(
                                net.hwyz.iov.cloud.edd.sgw.api.enums.PublicationStatus.WAITING_ACK).build()));
        when(configVersionService.rollback(eq(1L), any()))
                .thenReturn(ApiResponse.ok(PublicationResponse.builder()
                        .publicationId("p2").targetVersionNo(3L).build()));

        mockMvc.perform(post("/sgw/config-versions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftRevision\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationId").value("p1"));

        mockMvc.perform(post("/sgw/config-versions/1/rollback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftRevision\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationId").value("p2"));
    }

    @Test
    @DisplayName("服务网关业务错误保留状态码与错误码")
    void remoteErrorMapped() throws Exception {
        when(configVersionService.publish(any()))
                .thenThrow(new SgwRemoteException("SGW-409001", "草稿修订号不匹配", HttpStatus.CONFLICT));

        mockMvc.perform(post("/sgw/config-versions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftRevision\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SGW-409001"))
                .andExpect(jsonPath("$.message").value("草稿修订号不匹配"));
    }

    @Test
    @DisplayName("策略校验与发布记录查询转发")
    void policyAndPublicationDelegate() throws Exception {
        when(policyAdminService.validate(eq("canary"), any()))
                .thenReturn(ApiResponse.ok(net.hwyz.iov.cloud.edd.sgw.api.vo.response.ValidationResultVo.builder()
                        .valid(true).errors(List.of()).build()));
        when(publicationService.get("p1"))
                .thenReturn(ApiResponse.ok(PublicationResponse.builder().publicationId("p1").build()));
        when(publicationService.page(1, 20, null, null))
                .thenReturn(ApiResponse.ok(PageResult.<PublicationResponse>builder()
                        .items(List.of(PublicationResponse.builder().publicationId("p1").build()))
                        .total(1).page(1).size(20).build()));

        mockMvc.perform(post("/sgw/policies/canary/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyCode\":\"canary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(get("/sgw/publications/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationId").value("p1"));

        mockMvc.perform(get("/sgw/publications/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].publicationId").value("p1"));
    }

    @Test
    @DisplayName("版本历史与差异查询转发")
    void configVersionQueriesDelegate() throws Exception {
        when(configVersionService.history(1, 20))
                .thenReturn(ApiResponse.ok(PageResult.<ConfigVersionResponse>builder()
                        .items(List.of(ConfigVersionResponse.builder().versionNo(1L).current(true).build()))
                        .total(1).page(1).size(20).build()));
        when(configVersionService.diff(1L, 2L))
                .thenReturn(ApiResponse.ok(net.hwyz.iov.cloud.edd.sgw.api.vo.response.ConfigDiffResponse.builder()
                        .fromVersionNo(1L).toVersionNo(2L).items(List.of()).build()));

        mockMvc.perform(get("/sgw/config-versions/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].versionNo").value(1));

        mockMvc.perform(get("/sgw/config-versions/diff").param("from", "1").param("to", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromVersionNo").value(1))
                .andExpect(jsonPath("$.data.toVersionNo").value(2));
    }

    @Test
    @DisplayName("运行时、上游选项与导出转发")
    void runtimeUpstreamAndExportDelegate() throws Exception {
        when(runtimeService.instances())
                .thenReturn(ApiResponse.ok(List.of(RuntimeInstanceResponse.builder()
                        .instanceId("10.0.0.1:10803").activeVersionNo(2L).build())));
        when(upstreamService.options())
                .thenReturn(ApiResponse.ok(UpstreamOptionsResponse.builder()
                        .services(List.of()).directHosts(List.of()).build()));
        when(exportService.export("routes"))
                .thenReturn(ApiResponse.ok(ExportResponseVo.builder().type("routes")
                        .generatedAt(Instant.now()).content("[]").build()));

        mockMvc.perform(get("/sgw/runtime/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].instanceId").value("10.0.0.1:10803"));

        mockMvc.perform(get("/sgw/upstreams/options"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sgw/exports/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("routes"));
    }

}
