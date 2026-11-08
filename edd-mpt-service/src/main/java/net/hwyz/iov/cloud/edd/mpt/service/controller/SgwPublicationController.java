package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.enums.PublicationStatus;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwPublicationService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.RetryPublicationRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.PublicationResponse;
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
 * 服务网关发布记录管理（MPT 管理端）。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/publications")
public class SgwPublicationController {

    private final SgwPublicationService publicationService;

    public SgwPublicationController(SgwPublicationService publicationService) {
        this.publicationService = publicationService;
    }

    /**
     * 重试失败的发布
     *
     * @param request 重试请求
     * @return 新的发布记录
     */
    @RequiresPermissions("edd:sgw:publication:retry")
    @Log(title = "服务网关发布重试", businessType = BusinessType.UPDATE)
    @PostMapping
    public ApiResponse<PublicationResponse> retry(@RequestBody RetryPublicationRequest request) {
        return publicationService.retry(request);
    }

    /**
     * 查询发布记录与逐实例结果
     *
     * @param publicationId 发布ID
     * @return 发布记录
     */
    @RequiresPermissions("edd:sgw:publication:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<PublicationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) Long targetVersionNo) {
        return publicationService.page(page, size, status, targetVersionNo);
    }

    @RequiresPermissions("edd:sgw:publication:query")
    @GetMapping("/{publicationId}")
    public ApiResponse<PublicationResponse> get(@PathVariable String publicationId) {
        return publicationService.get(publicationId);
    }

}
