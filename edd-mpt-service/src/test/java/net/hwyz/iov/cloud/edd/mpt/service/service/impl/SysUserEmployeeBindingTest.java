package net.hwyz.iov.cloud.edd.mpt.service.service.impl;

import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import net.hwyz.iov.cloud.edd.mpt.api.enums.MptUserErrorCode;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.SysUserMapper;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.SysUserRoleMapper;
import net.hwyz.iov.cloud.edd.org.api.service.OrgEmployeeService;
import net.hwyz.iov.cloud.edd.org.api.vo.EmployeeOptionExService;
import net.hwyz.iov.cloud.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EDD-MPT-DSN-CR-002 / US-030、US-031：账号与员工绑定内部契约测试。
 * <p>
 * 覆盖：按员工解析、自动开通幂等与工号冲突、按员工停用、解绑、可绑定员工选项过滤（排除已绑定）。
 *
 * @author hwyz_leo
 * @since 2026-11-17
 */
class SysUserEmployeeBindingTest {

    private SysUserMapper userMapper;
    private SysUserRoleMapper userRoleMapper;
    private OrgEmployeeService orgEmployeeService;
    private SysUserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        orgEmployeeService = mock(OrgEmployeeService.class);
        userService = new SysUserServiceImpl();
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "userRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(userService, "orgEmployeeService", orgEmployeeService);
    }

    @Test
    @DisplayName("按员工解析：已绑定返回账号，未绑定返回 null")
    void selectUserByEmployeeId() {
        SysUser bound = new SysUser();
        bound.setUserId(2001L);
        bound.setEmployeeId(1001L);
        when(userMapper.selectUserByEmployeeId(1001L)).thenReturn(bound);

        assertNotNull(userService.selectUserByEmployeeId(1001L));
        assertEquals(null, userService.selectUserByEmployeeId(null));
        verify(userMapper, never()).selectUserByEmployeeId(null);
    }

    @Test
    @DisplayName("自动开通：已存在账号时幂等返回既有账号，不重复建号")
    void provisionIsIdempotent() {
        SysUser existing = new SysUser();
        existing.setUserId(2001L);
        existing.setEmployeeId(1001L);
        when(userMapper.selectUserByEmployeeId(1001L)).thenReturn(existing);

        SysUser request = new SysUser();
        request.setEmployeeId(1001L);
        request.setUserName("E1001");

        SysUser result = userService.provisionUser(request);

        assertEquals(2001L, result.getUserId());
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("自动开通：工号被占用时拒绝并提示人工处理")
    void provisionRejectsUserNameConflict() {
        when(userMapper.selectUserByEmployeeId(1001L)).thenReturn(null);
        SysUser occupied = new SysUser();
        occupied.setUserId(9L);
        when(userMapper.checkUserNameUnique("E1001")).thenReturn(occupied);

        SysUser request = new SysUser();
        request.setEmployeeId(1001L);
        request.setUserName("E1001");

        ServiceException ex = assertThrows(ServiceException.class, () -> userService.provisionUser(request));
        assertTrue(ex.getMessage().contains("工号已被其他账号占用"));
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("自动开通：建号默认启用、必须改密、绑定员工且不授予角色")
    void provisionCreatesEnabledUserRequiringPasswordReset() {
        when(userMapper.selectUserByEmployeeId(1001L)).thenReturn(null);
        when(userMapper.checkUserNameUnique("E1001")).thenReturn(null);

        SysUser request = new SysUser();
        request.setEmployeeId(1001L);
        request.setUserName("E1001");
        request.setNickName("张三");
        request.setPhonenumber("13900000000");

        userService.provisionUser(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insertUser(captor.capture());
        SysUser created = captor.getValue();
        assertEquals(1001L, created.getEmployeeId());
        assertEquals("E1001", created.getUserName());
        assertEquals("张三", created.getNickName());
        assertEquals("0", created.getStatus(), "默认启用");
        assertEquals(1, created.getPwdResetRequired(), "必须改密");
        assertNotNull(created.getPassword(), "必须生成随机密码（BCrypt）");
        assertTrue(created.getPassword().startsWith("$2"), "密码必须为 BCrypt 哈希");
        // 不授予任何角色：用户-角色关联表不应被写入
        verify(userRoleMapper, never()).batchUserRole(anyList());
    }

    @Test
    @DisplayName("按员工停用：更新状态并回收该用户会话")
    void disableByEmployeeId() {
        when(userMapper.selectUserIdsByEmployeeId(1001L)).thenReturn(List.of(2001L));
        when(userMapper.updateStatusByEmployeeId(1001L, "1")).thenReturn(1);

        assertEquals(1, userService.disableByEmployeeId(1001L));
        verify(userMapper).updateStatusByEmployeeId(1001L, "1");
        assertEquals(0, userService.disableByEmployeeId(null));
    }

    @Test
    @DisplayName("按员工启用：恢复账号为正常状态（SEC-EIAM 身份目录启用联动）")
    void enableByEmployeeId() {
        when(userMapper.updateStatusByEmployeeId(1001L, "0")).thenReturn(1);

        assertEquals(1, userService.enableByEmployeeId(1001L));
        verify(userMapper).updateStatusByEmployeeId(1001L, "0");
        assertEquals(0, userService.enableByEmployeeId(null));
    }

    @Test
    @DisplayName("解绑：清除账号与员工的关联")
    void unbindEmployee() {
        when(userMapper.updateUserEmployeeUnbind(2001L)).thenReturn(1);
        assertEquals(1, userService.unbindEmployee(2001L));
        verify(userMapper).updateUserEmployeeUnbind(2001L);
    }

    @Test
    @DisplayName("可绑定员工选项：排除已绑定账号的员工")
    void listEmployeeOptionsExcludesBound() {
        when(orgEmployeeService.getActiveEmployees("E1")).thenReturn(List.of(
                option(1001L, "E1001", "张三"),
                option(1002L, "E1002", "李四")));
        when(userMapper.selectBoundEmployeeIds(anyList())).thenReturn(List.of(1001L));

        List<EmployeeOptionExService> options = userService.listEmployeeOptions("E1");

        assertEquals(1, options.size());
        assertEquals(1002L, options.get(0).getEmployeeId());
    }

    @Test
    @DisplayName("可绑定员工选项：上游无返回时返回空列表，不报错")
    void listEmployeeOptionsWithUpstreamUnavailable() {
        when(orgEmployeeService.getActiveEmployees(any())).thenReturn(null);
        assertTrue(userService.listEmployeeOptions("x").isEmpty());
    }

    @Test
    @DisplayName("错误码常量与设计一致")
    void errorCodes() {
        assertEquals("100101", MptUserErrorCode.USER_BINDING_MISSING.getCode());
        assertEquals("100102", MptUserErrorCode.USER_NAME_CONFLICT.getCode());
        assertEquals("100103", MptUserErrorCode.USER_DISABLED.getCode());
        assertEquals("100104", MptUserErrorCode.PWD_RESET_REQUIRED.getCode());
    }

    private EmployeeOptionExService option(Long id, String code, String name) {
        return EmployeeOptionExService.builder()
                .employeeId(id)
                .employeeCode(code)
                .employeeName(name)
                .build();
    }
}
