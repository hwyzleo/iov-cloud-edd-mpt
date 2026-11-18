package net.hwyz.iov.cloud.edd.mpt.service.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.ServiceException;
import net.hwyz.iov.cloud.framework.common.util.BeanValidators;
import net.hwyz.iov.cloud.framework.web.util.SpringUtil;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.framework.datascope.annotation.DataScope;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.edd.mpt.api.constant.UserConstants;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysRole;
import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import net.hwyz.iov.cloud.edd.mpt.api.enums.MptUserErrorCode;
import net.hwyz.iov.cloud.edd.mpt.service.domain.SysPost;
import net.hwyz.iov.cloud.edd.mpt.service.domain.SysUserPost;
import net.hwyz.iov.cloud.edd.mpt.service.domain.SysUserRole;
import net.hwyz.iov.cloud.edd.mpt.service.mapper.*;
import net.hwyz.iov.cloud.edd.mpt.service.service.ISysConfigService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ISysDeptService;
import net.hwyz.iov.cloud.edd.mpt.service.service.ISysUserService;
import net.hwyz.iov.cloud.edd.org.api.service.OrgEmployeeService;
import net.hwyz.iov.cloud.edd.org.api.vo.EmployeeOptionExService;
import net.hwyz.iov.cloud.framework.common.constant.CacheConstants;
import net.hwyz.iov.cloud.framework.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户 业务层处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class SysUserServiceImpl implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPostMapper postMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysUserPostMapper userPostMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysDeptService deptService;

    /**
     * 会话回收时最多扫描的令牌键数量（防止大规模扫描）
     */
    private static final int MAX_SESSION_SCAN = 5000;

    @Autowired
    private OrgEmployeeService orgEmployeeService;

    @Autowired
    private RedisService redisService;

    @Autowired
    protected Validator validator;

    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user) {
        return userMapper.selectUserList(user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectAllocatedList(SysUser user) {
        return userMapper.selectAllocatedList(user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUnallocatedList(SysUser user) {
        return userMapper.selectUnallocatedList(user);
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        return userMapper.selectUserByUserName(userName);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    /**
     * 查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName) {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list)) {
            return StrUtil.EMPTY;
        }
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName) {
        List<SysPost> list = postMapper.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list)) {
            return StrUtil.EMPTY;
        }
        return list.stream().map(SysPost::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user) {
        Long userId = ObjUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUserName());
        if (ObjUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(SysUser user) {
        Long userId = ObjUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (ObjUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(SysUser user) {
        Long userId = ObjUtil.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (ObjUtil.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (ObjUtil.isNotNull(user.getUserId()) && user.isAdmin()) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (!SysUser.isAdmin(SecurityUtils.getUserId())) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = SpringUtil.getAopProxy(this).selectUserList(user);
            if (ArrayUtil.isEmpty(users)) {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user) {
        // 新增用户信息
        int rows = userMapper.insertUser(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return rows;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user) {
        return userMapper.insertUser(user) > 0;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user) {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPostByUserId(userId);
        // 新增用户与岗位管理
        insertUserPost(user);
        return userMapper.updateUser(user);
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds) {
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean updateUserProfile(SysUser user) {
        return userMapper.updateUser(user) > 0;
    }

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(String userName, String password) {
        return userMapper.resetUserPwd(userName, password);
    }

    /**
     * 新增用户角色信息
     *
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user) {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     *
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user) {
        Long[] posts = user.getPostIds();
        if (ArrayUtil.isNotEmpty(posts)) {
            // 新增用户与岗位管理
            List<SysUserPost> list = new ArrayList<SysUserPost>();
            for (Long postId : posts) {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            userPostMapper.batchUserPost(list);
        }
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId) {
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 删除用户与岗位表
        userPostMapper.deleteUserPostByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPost(userIds);
        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据
     *
     * @param userList        用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
        if (ObjUtil.isNull(userList) || userList.size() == 0) {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUser user : userList) {
            try {
                // 验证是否存在这个用户
                SysUser u = userMapper.selectUserByUserName(user.getUserName());
                if (ObjUtil.isNull(u)) {
                    BeanValidators.validateWithException(validator, user);
                    deptService.checkDeptDataScope(user.getDeptId());
                    String password = configService.selectConfigByKey("sys.user.initPassword");
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    userMapper.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(u);
                    checkUserDataScope(u.getUserId());
                    deptService.checkDeptDataScope(user.getDeptId());
                    user.setUserId(u.getUserId());
                    user.setUpdateBy(operName);
                    userMapper.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    public SysUser selectUserByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return userMapper.selectUserByEmployeeId(employeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser provisionUser(SysUser req) {
        Long employeeId = req.getEmployeeId();
        if (employeeId == null) {
            throw new ServiceException("employeeId 不能为空");
        }
        SysUser exist = userMapper.selectUserByEmployeeId(employeeId);
        if (ObjUtil.isNotNull(exist)) {
            // 幂等：已开通则直接返回既有账号，不改密码、不改角色
            return exist;
        }
        String userName = StrUtil.trim(req.getUserName());
        if (StrUtil.isBlank(userName)) {
            throw new ServiceException("工号不能为空");
        }
        if (ObjUtil.isNotNull(userMapper.checkUserNameUnique(userName))) {
            throw new ServiceException(MptUserErrorCode.USER_NAME_CONFLICT.getMessage());
        }
        SysUser user = new SysUser();
        user.setEmployeeId(employeeId);
        user.setUserName(userName);
        user.setNickName(StrUtil.isBlank(req.getNickName()) ? userName : req.getNickName());
        user.setPhonenumber(StrUtil.trim(req.getPhonenumber()));
        user.setEmail(StrUtil.trim(req.getEmail()));
        // 随机初始密码：不返回明文，账号标记必须改密，由管理员重置密码后线下告知
        user.setPassword(SecurityUtils.encryptPassword(RandomUtil.randomString(16)));
        user.setStatus(UserConstants.NORMAL);
        user.setPwdResetRequired(1);
        user.setCreateBy("eiam");
        user.setRemark("由员工主数据自动开通（EDD-MPT-DSN-CR-002）");
        userMapper.insertUser(user);
        log.info("自动开通账号：employeeId={}, userId={}", employeeId, user.getUserId());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int disableByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return 0;
        }
        List<Long> userIds = userMapper.selectUserIdsByEmployeeId(employeeId);
        int rows = userMapper.updateStatusByEmployeeId(employeeId, UserConstants.USER_DISABLE);
        revokeSessionsQuietly(userIds);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int enableByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return 0;
        }
        int rows = userMapper.updateStatusByEmployeeId(employeeId, UserConstants.NORMAL);
        log.info("按员工启用账号：employeeId={}, rows={}", employeeId, rows);
        return rows;
    }

    @Override
    public int unbindEmployee(Long userId) {
        return userMapper.updateUserEmployeeUnbind(userId);
    }

    /**
     * 查询可绑定账号的在职员工（排除已绑定账号的员工）
     *
     * @param key 关键词（工号/姓名/手机号）
     * @return 员工选项
     */
    public List<EmployeeOptionExService> listEmployeeOptions(String key) {
        List<EmployeeOptionExService> employees = orgEmployeeService.getActiveEmployees(key);
        if (CollectionUtils.isEmpty(employees)) {
            return Collections.emptyList();
        }
        List<Long> employeeIds = employees.stream()
                .map(EmployeeOptionExService::getEmployeeId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return employees;
        }
        Set<Long> boundIds = new HashSet<>(userMapper.selectBoundEmployeeIds(employeeIds));
        return employees.stream()
                .filter(employee -> !boundIds.contains(employee.getEmployeeId()))
                .collect(Collectors.toList());
    }

    /**
     * 回收指定用户的在线会话（尽力而为，失败仅告警）
     *
     * @param userIds 用户ID列表
     */
    private void revokeSessionsQuietly(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        try {
            Set<String> keys = redisService.redisTemplate.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            if (keys.size() > MAX_SESSION_SCAN) {
                log.warn("会话回收跳过：令牌键数量过多（{}）", keys.size());
                return;
            }
            for (String key : keys) {
                Object value = redisService.getCacheObject(key);
                if (value instanceof net.hwyz.iov.cloud.edd.mpt.api.model.LoginUser loginUser
                        && loginUser.getUserid() != null && userIds.contains(loginUser.getUserid())) {
                    redisService.deleteObject(key);
                }
            }
        } catch (Exception e) {
            log.warn("会话回收失败，userId={}", userIds, e);
        }
    }
}
