package net.hwyz.iov.cloud.edd.mpt.service.controller;

import net.hwyz.iov.cloud.edd.sgw.api.service.SgwRouteAdminService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.CopyRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.RouteSaveRequest;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.RouteResponse;
import net.hwyz.iov.cloud.edd.sgw.api.vo.response.ValidationResultVo;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务网关路由管理（MPT 管理端）。
 * <p>
 * 配置唯一属主为服务网关，本控制器仅做权限校验、操作审计与请求转发。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/routes")
public class SgwRouteController {

    private final SgwRouteAdminService routeAdminService;

    public SgwRouteController(SgwRouteAdminService routeAdminService) {
        this.routeAdminService = routeAdminService;
    }

    /**
     * 物理分页查询路由草稿
     *
     * @param page       页码
     * @param size       每页条数
     * @param name       名称模糊匹配
     * @param enabled    启用状态
     * @param target     目标
     * @param clientType 客户端类型
     * @return 分页结果
     */
    @RequiresPermissions("edd:sgw:route:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<RouteResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) String name,
                                                        @RequestParam(required = false) Boolean enabled,
                                                        @RequestParam(required = false) String target,
                                                        @RequestParam(required = false) String clientType) {
        return routeAdminService.page(page, size, name, enabled, target, clientType);
    }

    /**
     * 查询路由详情
     *
     * @param routeCode 路由编码
     * @return 路由详情
     */
    @RequiresPermissions("edd:sgw:route:query")
    @GetMapping("/{routeCode}")
    public ApiResponse<RouteResponse> get(@PathVariable String routeCode) {
        return routeAdminService.get(routeCode);
    }

    /**
     * 新增路由草稿
     *
     * @param request 请求
     * @return 路由详情
     */
    @RequiresPermissions("edd:sgw:route:add")
    @Log(title = "服务网关路由管理", businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResponse<RouteResponse> add(@RequestBody RouteSaveRequest request) {
        return routeAdminService.create(request);
    }

    /**
     * 更新路由草稿
     *
     * @param routeCode 路由编码
     * @param request   请求
     * @return 路由详情
     */
    @RequiresPermissions("edd:sgw:route:edit")
    @Log(title = "服务网关路由管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{routeCode}")
    public ApiResponse<RouteResponse> edit(@PathVariable String routeCode, @RequestBody RouteSaveRequest request) {
        return routeAdminService.update(routeCode, request);
    }

    /**
     * 复制路由草稿
     *
     * @param routeCode 源路由编码
     * @param request   复制请求
     * @return 新路由详情
     */
    @RequiresPermissions("edd:sgw:route:add")
    @Log(title = "服务网关路由管理", businessType = BusinessType.INSERT)
    @PostMapping("/{routeCode}/copy")
    public ApiResponse<RouteResponse> copy(@PathVariable String routeCode, @RequestBody CopyRequest request) {
        return routeAdminService.copy(routeCode, request);
    }

    /**
     * 逻辑删除路由草稿
     *
     * @param routeCode 路由编码
     * @return 空响应
     */
    @RequiresPermissions("edd:sgw:route:remove")
    @Log(title = "服务网关路由管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{routeCode}")
    public ApiResponse<Void> remove(@PathVariable String routeCode) {
        return routeAdminService.delete(routeCode);
    }

    /**
     * 校验路由配置
     *
     * @param request 请求
     * @return 校验结果
     */
    @RequiresPermissions("edd:sgw:route:query")
    @PostMapping("/validate")
    public ApiResponse<ValidationResultVo> validate(@RequestBody RouteSaveRequest request) {
        return routeAdminService.validate(request);
    }

}
