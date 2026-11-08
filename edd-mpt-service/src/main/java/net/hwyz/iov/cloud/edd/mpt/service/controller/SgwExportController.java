package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.service.SgwExportService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ExportResponseVo;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务网关结构化配置导出（MPT 管理端）。
 * <p>
 * 导出内容不包含 HMAC 密钥、CIAM 签名密钥等敏感信息。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/exports")
public class SgwExportController {

    private final SgwExportService exportService;

    public SgwExportController(SgwExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 导出路由配置
     *
     * @return 导出内容
     */
    @RequiresPermissions("edd:sgw:route:export")
    @Log(title = "服务网关路由导出", businessType = BusinessType.EXPORT)
    @GetMapping("/routes")
    public ApiResponse<ExportResponseVo> exportRoutes() {
        return exportService.export("routes");
    }

    /**
     * 导出策略配置
     *
     * @param type 策略类型
     * @return 导出内容
     */
    @RequiresPermissions("edd:sgw:policy:export")
    @Log(title = "服务网关策略导出", businessType = BusinessType.EXPORT)
    @GetMapping("/policies/{type}")
    public ApiResponse<ExportResponseVo> exportPolicies(@PathVariable String type) {
        return exportService.export(type + "-policies");
    }

    /**
     * 导出完整草稿配置
     *
     * @return 导出内容
     */
    @RequiresPermissions("edd:sgw:config:query")
    @Log(title = "服务网关配置导出", businessType = BusinessType.EXPORT)
    @GetMapping("/config")
    public ApiResponse<ExportResponseVo> exportConfig() {
        return exportService.export("config");
    }

}
