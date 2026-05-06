package net.hwyz.iov.cloud.edd.mpt.service.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.service.ExTspSgwRouteService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ITspSgwRouteService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.util.ExcelUtil;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业数字底座服务网关路由信息
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw-route")
public class SgwRouteController extends BaseController {
    @Autowired
    private ITspSgwRouteService routeService;
    @Autowired
    private ExTspSgwRouteService exTspSgwRouteService;

    @RequiresPermissions("edd:sgw:route:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<TspSgwRoute>> list(TspSgwRoute route) {
        startPage();
        List<TspSgwRoute> list = routeService.selectRouteList(route);
        return ApiResponse.ok(getPageResult(list));
    }

    @Log(title = "服务网关路由管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("edd:sgw:route:export")
    @PostMapping("/export")
    public ApiResponse<Void> export(HttpServletResponse response, TspSgwRoute route) {
        List<TspSgwRoute> list = routeService.selectRouteList(route);
        ExcelUtil<TspSgwRoute> util = new ExcelUtil<>(TspSgwRoute.class);
        util.exportExcel(response, list, "TSP服务网关路由数据");
        return ApiResponse.ok();
    }

    /**
     * 根据路由ID获取详细信息
     */
    @RequiresPermissions("edd:sgw:route:query")
    @GetMapping(value = "/{routeId}")
    public ApiResponse<TspSgwRoute> getInfo(@PathVariable Long routeId) {
        return ApiResponse.ok(routeService.selectRouteById(routeId));
    }

    /**
     * 新增路由
     */
    @RequiresPermissions("edd:sgw:route:add")
    @Log(title = "服务网关路由管理", businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody TspSgwRoute route) {
        route.setCreateBy(SecurityUtils.getUserId().toString());
        int result = routeService.insertRoute(route);
        exTspSgwRouteService.add(route);
        return ApiResponse.ok(result);
    }

    /**
     * 修改保存路由
     */
    @RequiresPermissions("edd:sgw:route:edit")
    @Log(title = "服务网关路由管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody TspSgwRoute route) {
        route.setModifyBy(SecurityUtils.getUserId().toString());
        int result = routeService.updateRoute(route);
        exTspSgwRouteService.update(route);
        return ApiResponse.ok(result);
    }

    /**
     * 删除路由
     */
    @RequiresPermissions("edd:sgw:route:remove")
    @Log(title = "服务网关路由管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{routeIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] routeIds) {
        int result = routeService.deleteRouteByIds(routeIds);
        exTspSgwRouteService.delete(routeIds);
        return ApiResponse.ok(result);
    }

    /**
     * 刷新路由
     */
    @RequiresPermissions("edd:sgw:route:refresh")
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh() {
        exTspSgwRouteService.refresh();
        return ApiResponse.ok();
    }

}
