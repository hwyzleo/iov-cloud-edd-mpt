package net.hwyz.iov.cloud.edd.mpt.service.controller;

import cn.hutool.core.util.ObjUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.util.ExcelUtil;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.bean.AjaxResult;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.InnerAuth;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.service.TokenService;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysDept;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysRole;
import net.hwyz.iov.cloud.edd.mpt.api.constant.UserConstants;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import net.hwyz.iov.cloud.edd.mpt.api.enums.MptUserErrorCode;
import net.hwyz.iov.cloud.edd.mpt.api.model.LoginUser;
import net.hwyz.iov.cloud.edd.org.api.vo.EmployeeOptionExService;
import net.hwyz.iov.cloud.edd.mpt.service.service.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class SysUserController extends BaseController {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysPermissionService permissionService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private TokenService tokenService;

    /**
     * 获取用户列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/list")
    public ApiResponse<PageResult<SysUser>> list(SysUser user) {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return ApiResponse.ok(getPageResult(list));
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user) {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 获取当前用户信息
     */
    @InnerAuth
    @GetMapping("/info/{username}")
    public ApiResponse<LoginUser> info(@PathVariable("username") String username) {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (ObjUtil.isNull(sysUser)) {
            return ApiResponse.fail("用户名或密码错误");
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        return ApiResponse.ok(buildLoginUser(sysUser, roles, permissions));
    }

    /**
     * 注册用户信息
     */
    @InnerAuth
    @PostMapping("/register")
    public ApiResponse<Boolean> register(@RequestBody SysUser sysUser) {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            return ApiResponse.fail("当前系统没有开启注册功能！");
        }
        if (!userService.checkUserNameUnique(sysUser)) {
            return ApiResponse.fail("保存用户'" + username + "'失败，注册账号已存在");
        }
        return ApiResponse.ok(userService.registerUser(sysUser));
    }

    /**
     * 记录用户登录IP地址和登录时间
     */
    @InnerAuth
    @PutMapping("/recordlogin")
    public ApiResponse<Boolean> recordlogin(@RequestBody SysUser sysUser) {
        return ApiResponse.ok(userService.updateUserProfile(sysUser));
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getSysUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 根据用户编号获取详细信息
     */
    @RequiresPermissions("system:user:query")
    @GetMapping(value = {"/", "/{userId}"})
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        AjaxResult ajax = AjaxResult.success();
        if (ObjUtil.isNotNull(userId)) {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user)) {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StrUtil.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StrUtil.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StrUtil.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StrUtil.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId())) {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @RequiresPermissions("system:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId) {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds) {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept) {
        return success(deptService.selectDeptTreeList(dept));
    }

    /**
     * 按员工ID获取登录用户（内部契约，供 EIAM 单点登录使用，EDD-MPT-DSN-CR-002）
     */
    @InnerAuth
    @GetMapping("/info/by-employee/{employeeId}")
    public ApiResponse<LoginUser> infoByEmployee(@PathVariable("employeeId") Long employeeId) {
        SysUser sysUser = userService.selectUserByEmployeeId(employeeId);
        if (ObjUtil.isNull(sysUser)) {
            return ApiResponse.fail(MptUserErrorCode.USER_BINDING_MISSING);
        }
        if (UserConstants.USER_DISABLE.equals(sysUser.getStatus())) {
            return ApiResponse.fail(MptUserErrorCode.USER_DISABLED);
        }
        Set<String> roles = permissionService.getRolePermission(sysUser);
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        return ApiResponse.ok(buildLoginUser(sysUser, roles, permissions));
    }

    /**
     * 按员工信息幂等开通账号（内部契约，供 EIAM 自动开通，EDD-MPT-DSN-CR-002）
     */
    @InnerAuth
    @PostMapping("/provision")
    public ApiResponse<SysUser> provision(@RequestBody SysUser sysUser) {
        log.info("内部契约：按员工[{}]开通账号", sysUser.getEmployeeId());
        SysUser provisioned = userService.provisionUser(sysUser);
        return ApiResponse.ok(provisioned);
    }

    /**
     * 按员工ID停用账号（内部契约，供 EIAM 离职联动，EDD-MPT-DSN-CR-002）
     */
    @InnerAuth
    @PostMapping("/provision/disable/{employeeId}")
    public ApiResponse<Integer> disableByEmployee(@PathVariable("employeeId") Long employeeId) {
        log.info("内部契约：按员工[{}]停用账号", employeeId);
        return ApiResponse.ok(userService.disableByEmployeeId(employeeId));
    }

    /**
     * 可绑定账号的在职员工选项（内部契约，供 EIAM/前端选人，EDD-MPT-DSN-CR-002）
     */
    @InnerAuth
    @GetMapping("/employee/options")
    public ApiResponse<List<EmployeeOptionExService>> employeeOptions(@RequestParam(required = false) String key) {
        return ApiResponse.ok(((net.hwyz.iov.cloud.edd.mpt.service.service.impl.SysUserServiceImpl) userService)
                .listEmployeeOptions(key));
    }

    /**
     * 解绑账号与员工的关联
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{userId}/employee/unbind")
    public AjaxResult unbindEmployee(@PathVariable Long userId) {
        return toAjax(userService.unbindEmployee(userId));
    }

    /**
     * 组装登录用户（角色/权限/员工标识）
     */
    private LoginUser buildLoginUser(SysUser sysUser, Set<String> roles, Set<String> permissions) {
        LoginUser loginUser = new LoginUser();
        loginUser.setSysUser(sysUser);
        loginUser.setEmployeeId(sysUser.getEmployeeId());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return loginUser;
    }
}
