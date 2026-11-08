package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.service.SgwUpstreamService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.UpstreamOptionsResponse;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务网关上游可选项（MPT 管理端）。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/upstreams")
public class SgwUpstreamController {

    private final SgwUpstreamService upstreamService;

    public SgwUpstreamController(SgwUpstreamService upstreamService) {
        this.upstreamService = upstreamService;
    }

    /**
     * 查询注册中心服务与获批直连主机
     *
     * @return 上游可选项
     */
    @RequiresPermissions("edd:sgw:upstream:list")
    @GetMapping("/options")
    public ApiResponse<UpstreamOptionsResponse> options() {
        return upstreamService.options();
    }

}
