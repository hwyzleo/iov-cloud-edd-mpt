package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.service.SgwRuntimeService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.RuntimeInstanceResponse;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务网关运行时状态（MPT 管理端）。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/runtime")
public class SgwRuntimeController {

    private final SgwRuntimeService runtimeService;

    public SgwRuntimeController(SgwRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /**
     * 查询服务发现中的实例及其配置版本状态
     *
     * @return 实例状态列表
     */
    @RequiresPermissions("edd:sgw:runtime:list")
    @GetMapping("/instances")
    public ApiResponse<List<RuntimeInstanceResponse>> instances() {
        return runtimeService.instances();
    }

}
