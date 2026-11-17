package net.hwyz.iov.cloud.edd.mpt.service.mapper;

import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 数据层
 * 
 * @author hwyz_leo
 */
public interface SysUserMapper
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);

    /**
     * 通过员工ID查询用户（EDD-MPT-DSN-CR-002）
     *
     * @param employeeId ORG 员工ID
     * @return 用户
     */
    public SysUser selectUserByEmployeeId(Long employeeId);

    /**
     * 查询给定员工中已被账号绑定的员工ID集合
     *
     * @param employeeIds 员工ID集合
     * @return 已绑定员工ID
     */
    public List<Long> selectBoundEmployeeIds(@Param("employeeIds") List<Long> employeeIds);

    /**
     * 解绑用户与员工的关联
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int updateUserEmployeeUnbind(Long userId);

    /**
     * 按员工ID更新账号状态
     *
     * @param employeeId 员工ID
     * @param status     状态（0 正常 1 停用）
     * @return 结果
     */
    public int updateStatusByEmployeeId(@Param("employeeId") Long employeeId, @Param("status") String status);

    /**
     * 按员工ID查询用户ID列表（用于会话回收）
     *
     * @param employeeId 员工ID
     * @return 用户ID列表
     */
    public List<Long> selectUserIdsByEmployeeId(Long employeeId);
}
