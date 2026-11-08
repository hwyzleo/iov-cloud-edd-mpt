package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.service.SgwConfigVersionService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.PublishConfigRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.RollbackRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ConfigDiffResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ConfigVersionDetailResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ConfigVersionResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.PublicationResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.WorkspaceResponse;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务网关配置版本管理（MPT 管理端）。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/config-versions")
public class SgwConfigVersionController {

    private final SgwConfigVersionService configVersionService;

    public SgwConfigVersionController(SgwConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    /**
     * 查询当前草稿修订号与目标版本
     *
     * @return 工作区状态
     */
    @RequiresPermissions("edd:sgw:config:query")
    @GetMapping("/workspace")
    public ApiResponse<WorkspaceResponse> workspace() {
        return configVersionService.workspace();
    }

    /**
     * 分页查询版本历史
     *
     * @param page 页码
     * @param size 每页条数
     * @return 版本分页
     */
    @RequiresPermissions("edd:sgw:config:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<ConfigVersionResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return configVersionService.history(page, size);
    }

    /**
     * 查询不可变版本详情
     *
     * @param versionNo 版本号
     * @return 版本详情
     */
    @RequiresPermissions("edd:sgw:config:query")
    @GetMapping("/{versionNo}")
    public ApiResponse<ConfigVersionDetailResponse> detail(@PathVariable Long versionNo) {
        return configVersionService.detail(versionNo);
    }

    /**
     * 版本结构化差异
     *
     * @param from 起始版本
     * @param to   目标版本
     * @return 差异
     */
    @RequiresPermissions("edd:sgw:config:query")
    @GetMapping("/diff")
    public ApiResponse<ConfigDiffResponse> diff(@RequestParam Long from, @RequestParam Long to) {
        return configVersionService.diff(from, to);
    }

    /**
     * 校验并发布当前草稿修订
     *
     * @param request 发布请求
     * @return 发布记录
     */
    @RequiresPermissions("edd:sgw:config:publish")
    @Log(title = "服务网关配置发布", businessType = BusinessType.UPDATE)
    @PostMapping
    public ApiResponse<PublicationResponse> publish(@RequestBody PublishConfigRequest request) {
        return configVersionService.publish(request);
    }

    /**
     * 创建并发布回滚版本
     *
     * @param versionNo 目标历史版本
     * @param request   回滚请求
     * @return 发布记录
     */
    @RequiresPermissions("edd:sgw:config:rollback")
    @Log(title = "服务网关配置回滚", businessType = BusinessType.UPDATE)
    @PostMapping("/{versionNo}/rollback")
    public ApiResponse<PublicationResponse> rollback(@PathVariable Long versionNo,
                                                     @RequestBody RollbackRequest request) {
        return configVersionService.rollback(versionNo, request);
    }

}
