package net.hwyz.iov.cloud.edd.mpt.service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwPolicyAdminService;
import net.hwyz.iov.cloud.edd.sgw.api.vo.request.CopyRequest;
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
 * 服务网关治理策略管理（MPT 管理端）。
 * <p>
 * 策略类型：auth、traffic、canary。
 *
 * @author hwyz_leo
 */
@RestController
@RequestMapping("/sgw/policies/{type}")
public class SgwPolicyController {

    private final SgwPolicyAdminService policyAdminService;

    public SgwPolicyController(SgwPolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    /**
     * 分页查询策略
     *
     * @param type    策略类型
     * @param page    页码
     * @param size    每页条数
     * @param name    名称模糊匹配
     * @param enabled 启用状态
     * @return 分页结果
     */
    @RequiresPermissions("edd:sgw:policy:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<Object>> list(@PathVariable String type,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Boolean enabled) {
        return policyAdminService.page(type, page, size, name, enabled);
    }

    /**
     * 查询策略详情
     *
     * @param type       策略类型
     * @param policyCode 策略编码
     * @return 策略详情
     */
    @RequiresPermissions("edd:sgw:policy:query")
    @GetMapping("/{policyCode}")
    public ApiResponse<Object> get(@PathVariable String type, @PathVariable String policyCode) {
        return policyAdminService.get(type, policyCode);
    }

    /**
     * 新增策略草稿
     *
     * @param type 策略类型
     * @param body 结构化策略请求
     * @return 策略详情
     */
    @RequiresPermissions("edd:sgw:policy:add")
    @Log(title = "服务网关策略管理", businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResponse<Object> add(@PathVariable String type, @RequestBody JsonNode body) {
        return policyAdminService.create(type, body);
    }

    /**
     * 更新策略草稿
     *
     * @param type       策略类型
     * @param policyCode 策略编码
     * @param body       结构化策略请求
     * @return 策略详情
     */
    @RequiresPermissions("edd:sgw:policy:edit")
    @Log(title = "服务网关策略管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{policyCode}")
    public ApiResponse<Object> edit(@PathVariable String type, @PathVariable String policyCode,
                                    @RequestBody JsonNode body) {
        return policyAdminService.update(type, policyCode, body);
    }

    /**
     * 复制策略草稿
     *
     * @param type       源策略类型
     * @param policyCode 源策略编码
     * @param request    复制请求
     * @return 新策略详情
     */
    @RequiresPermissions("edd:sgw:policy:add")
    @Log(title = "服务网关策略管理", businessType = BusinessType.INSERT)
    @PostMapping("/{policyCode}/copy")
    public ApiResponse<Object> copy(@PathVariable String type, @PathVariable String policyCode,
                                    @RequestBody CopyRequest request) {
        return policyAdminService.copy(type, policyCode, request);
    }

    /**
     * 删除未被引用的策略
     *
     * @param type       策略类型
     * @param policyCode 策略编码
     * @return 空响应
     */
    @RequiresPermissions("edd:sgw:policy:remove")
    @Log(title = "服务网关策略管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{policyCode}")
    public ApiResponse<Void> remove(@PathVariable String type, @PathVariable String policyCode) {
        return policyAdminService.delete(type, policyCode);
    }

    /**
     * 校验策略配置
     *
     * @param type 策略类型
     * @param body 结构化策略请求
     * @return 校验结果
     */
    @RequiresPermissions("edd:sgw:policy:query")
    @PostMapping("/validate")
    public ApiResponse<ValidationResultVo> validate(@PathVariable String type, @RequestBody JsonNode body) {
        return policyAdminService.validate(type, body);
    }

}
