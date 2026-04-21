package net.hwyz.iov.cloud.edd.mpt.service.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.edd.mpt.api.domain.TspSgwRoute;
import net.hwyz.iov.cloud.edd.mpt.service.service.ExTspSgwRouteService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ITspSgwRouteService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.AjaxResult;
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
 * TSP服务网关路由信息
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/tsp-sgw-route")
public class TspSgwRouteController extends BaseController {
    @Autowired
    private ITspSgwRouteService routeService;
    @Autowired
    private ExTspSgwRouteService exTspSgwRouteService;

    @RequiresPermissions("tsp:sgw:route:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<TspSgwRoute>> list(TspSgwRoute route) {
        startPage();
        List<TspSgwRoute> list = routeService.selectRouteList(route);
        return ApiResponse.ok(getPageResult(list));
    }

    @Log(title = "TSP服务网关路由管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("tsp:sgw:route:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, TspSgwRoute route) {
        List<TspSgwRoute> list = routeService.selectRouteList(route);
        ExcelUtil<TspSgwRoute> util = new ExcelUtil<>(TspSgwRoute.class);
        util.exportExcel(response, list, "TSP服务网关路由数据");
    }

    /**
     * 根据路由ID获取详细信息
     */
    @RequiresPermissions("tsp:sgw:route:query")
    @GetMapping(value = "/{routeId}")
    public AjaxResult getInfo(@PathVariable Long routeId) {
        return success(routeService.selectRouteById(routeId));
    }

    /**
     * 新增路由
     */
    @RequiresPermissions("tsp:sgw:route:add")
    @Log(title = "TSP服务网关路由管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TspSgwRoute route) {
        route.setCreateBy(SecurityUtils.getUserId().toString());
        int result = routeService.insertRoute(route);
        exTspSgwRouteService.add(route);
        return toAjax(result);
    }

    /**
     * 修改保存路由
     */
    @RequiresPermissions("tsp:sgw:route:edit")
    @Log(title = "TSP服务网关路由管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TspSgwRoute route) {
        route.setModifyBy(SecurityUtils.getUserId().toString());
        int result = routeService.updateRoute(route);
        exTspSgwRouteService.update(route);
        return toAjax(result);
    }

    /**
     * 删除路由
     */
    @RequiresPermissions("tsp:sgw:route:remove")
    @Log(title = "TSP服务网关路由管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{routeIds}")
    public AjaxResult remove(@PathVariable Long[] routeIds) {
        int result = routeService.deleteRouteByIds(routeIds);
        exTspSgwRouteService.delete(routeIds);
        return toAjax(result);
    }

    /**
     * 刷新路由
     */
    @RequiresPermissions("tsp:sgw:route:refresh")
    @PostMapping("/refresh")
    public AjaxResult refresh() {
        exTspSgwRouteService.refresh();
        return success();
    }

}
