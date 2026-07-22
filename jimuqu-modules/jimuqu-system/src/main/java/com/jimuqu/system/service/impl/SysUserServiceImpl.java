package com.jimuqu.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.*;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.query.SysUserQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.domain.vo.SysUserExportVo;
import com.jimuqu.system.domain.vo.SysUserImportVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.*;
import com.jimuqu.system.service.SysFileService;
import com.jimuqu.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.validation.ValidUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 用户信息Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserPostMapper sysUserPostMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysPostMapper sysPostMapper;
    private final SysFileService sysFileService;
    private final SysConfigMapper sysConfigMapper;
    private final ISysDataScopeService dataScopeService;
    private final SysUserImportRowTransactionExecutor importRowTransactionExecutor;

    /**
     * 查询用户信息
     */
    @Override
    public SysUserVo queryById(Long id) {
        SysUserVo userVo = sysUserMapper.getVoById(id);
        return enrichUser(userVo);
    }

    /**
     * 查询用户信息分页列表
     */
    @Override
    public Page<SysUserVo> queryPageList(SysUserQuery query, PageQuery pageQuery) {
        Page<SysUserVo> page = pageQuery.applyOrder(buildQueryChain(query))
                .select(SysUser.class, SysDept.class)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .returnType(SysUserVo.class)
                .paging(pageQuery.build());
        page.getRows().forEach(this::enrichUser);
        return page;
    }

    /**
     * 查询用户信息列表
     */
    @Override
    public List<SysUserVo> queryList(SysUserQuery query) {
        List<SysUserVo> users = buildQueryChain(query)
                .select(SysUser.class, SysDept.class)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .returnType(SysUserVo.class)
                .list();
        users.forEach(this::enrichUser);
        return users;
    }

    @Override
    public List<SysUserExportVo> selectUserExportList(SysUserQuery query) {
        List<SysUserVo> users = buildQueryChain(query)
                .select(SysUser.class)
                .returnType(SysUserVo.class)
                .list();
        List<SysUserExportVo> exports = MapstructUtil.convert(users, SysUserExportVo.class);
        Set<Long> deptIds = StreamUtil.toSet(users, SysUserVo::getDeptId);
        if (deptIds.isEmpty()) {
            return exports;
        }

        Map<Long, SysDept> departments = StreamUtil.toIdentityMap(QueryChain.of(sysDeptMapper)
                .select(SysDept::getId, SysDept::getLeader)
                .in(SysDept::getId, deptIds)
                .list(), SysDept::getId);
        Set<Long> leaderIds = StreamUtil.toSet(departments.values(), SysDept::getLeader);
        Map<Long, String> leaderNames = leaderIds.isEmpty() ? Map.of() : StreamUtil.toMap(
                QueryChain.of(sysUserMapper)
                        .select(SysUser::getId, SysUser::getUserName)
                        .in(SysUser::getId, leaderIds)
                        .list(),
                SysUser::getId, SysUser::getUserName);

        for (SysUserExportVo export : exports) {
            SysDept dept = departments.get(export.getDeptId());
            export.setLeaderName(dept == null ? null : leaderNames.get(dept.getLeader()));
        }
        return exports;
    }

    private SysUserVo enrichUser(SysUserVo userVo) {
        if (ObjectUtil.isNull(userVo)) {
            return null;
        }
        userVo.setRoles(sysRoleMapper.selectRolesByUserId(userVo.getId()));
        if (ObjectUtil.isNotNull(userVo.getDeptId())) {
            SysDeptVo dept = sysDeptMapper.getById(userVo.getDeptId(), SysDeptVo.class);
            userVo.setDept(dept);
            userVo.setDeptName(ObjectUtil.isNull(dept) ? null : dept.getDeptName());
        }
        if (ObjectUtil.isNotNull(userVo.getAvatar())) {
            userVo.setAvatarUrl(sysFileService.selectUrlByIds(String.valueOf(userVo.getAvatar())));
        }
        return userVo;
    }

    /**
     * 构建查询条件
     *
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysUser> buildQueryChain(SysUserQuery query) {
        QueryChain<SysUser> queryChain = QueryChain.of(sysUserMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysUser::getId);

        if (ObjectUtil.isNotNull(query.getDeptId())) {
            List<Long> deptIdList = sysDeptMapper.selectListByParentId(query.getDeptId());
            deptIdList.add(query.getDeptId());
            queryChain.in(SysUser::getDeptId, deptIdList);
        }

        applyUserDataScope(queryChain);

        return queryChain;
    }

    /**
     * 将当前登录用户的多个角色数据范围以并集方式应用到用户查询。
     */
    private void applyUserDataScope(QueryChain<SysUser> queryChain) {
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (rule.allAccess()) {
            return;
        }

        boolean hasDepartments = !rule.departmentIds().isEmpty();
        boolean hasSelf = rule.selfAccess() && rule.userId() != null;
        if (hasDepartments && hasSelf) {
            queryChain.andNested(scope -> scope
                    .in(SysUser::getDeptId, rule.departmentIds())
                    .or()
                    .eq(SysUser::getCreateBy, rule.userId()));
        } else if (hasDepartments) {
            queryChain.in(SysUser::getDeptId, rule.departmentIds());
        } else if (hasSelf) {
            queryChain.eq(SysUser::getCreateBy, rule.userId());
        } else {
            // 使用不可同时成立的 typed 条件保证异常或空范围时拒绝全部数据。
            queryChain.andNested(scope -> scope
                    .eq(SysUser::getId, 0L)
                    .and()
                    .ne(SysUser::getId, 0L));
        }
    }

    /**
     * 新增用户信息
     */
    @Override
    @Transaction
    public Boolean insertByBo(SysUserBo bo) {
        SysUser sysUser = MapstructUtil.convert(bo, SysUser.class);
        boolean flag = sysUserMapper.save(sysUser) > 0;
        bo.setId(sysUser.getId());
        // 新增用户岗位关联
        insertUserPost(bo, false);
        // 新增用户与角色管理
        insertUserRole(bo, false);
        return flag;
    }

    /**
     * 修改用户信息
     */
    @Override
    @Transaction
    public Boolean updateByBo(SysUserBo bo) {
        // 新增用户与角色管理
        insertUserRole(bo, true);
        // 新增用户与岗位管理
        insertUserPost(bo, true);
        SysUser sysUser = MapstructUtil.convert(bo, SysUser.class);

        int num = updateUserWithDataScope(sysUser);
        Assert.gtZero(num, "更新用户失败");
        return num > 0;
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUserBo user) {
        SysUser sysUser = new SysUser()
                .setId(user.getId())
                .setNickName(user.getNickName())
                .setEmail(user.getEmail())
                .setPhonenumber(user.getPhonenumber())
                .setSex(user.getSex())
                .setAvatar(user.getAvatar());
        return updateUserWithDataScope(sysUser, DataScopeWriteRule.all());
    }

    /**
     * 个人中心修改本人密码，不受管理端写数据权限约束。
     */
    @Override
    public boolean resetOwnUserPwd(Long userId, String password) {
        return updateUserWithDataScope(
                new SysUser().setId(userId).setPassword(password),
                DataScopeWriteRule.all()) > 0;
    }

    /**
     * 新增用户岗位信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserPost(SysUserBo user, boolean clear) {
        if (CollUtil.isEmpty(user.getPostIds())) {
            return;
        }
        List<Long> postIds = validatePostIds(user.getPostIds());
        if (clear) {
            // 删除用户与岗位关联
            sysUserPostMapper.delete(where -> where.eq(SysUserPost::getUserId, user.getId()));
        }
        if (CollUtil.isEmpty(postIds)) {
            return;
        }
        // 新增用户与岗位管理
        List<SysUserPost> sysUserPostList = StreamUtil.toList(postIds, postId -> {
            SysUserPost up = new SysUserPost();
            up.setUserId(user.getId());
            up.setPostId(postId);
            return up;
        });
        sysUserPostMapper.saveBatch(sysUserPostList);
    }

    /**
     * 新增用户角色信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserRole(SysUserBo user, boolean clear) {
        this.insertUserRole(user.getId(), user.getRoleIds(), clear);
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     * @param clear   清除已存在的关联数据
     */
    private void insertUserRole(Long userId, List<Long> roleIds, boolean clear) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<Long> roleList = validateRoleIds(userId, roleIds);
        if (clear) {
            // 删除用户与角色关联
            sysUserRoleMapper.delete(where -> where.eq(SysUserRole::getUserId, userId));
        }
        if (roleList.isEmpty()) {
            return;
        }
        // 新增用户与角色管理
        List<SysUserRole> list = StreamUtil.toList(roleList, roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        });
        sysUserRoleMapper.saveBatch(list);
    }


    /**
     * 批量删除用户信息
     */
    @Override
    @Transaction
    public Integer deleteByIds(Collection<Long> ids) {
        for (Long userId : ids) {
            checkUserAllowed(userId);
            checkUserDataScope(userId);
        }
        sysUserRoleMapper.delete(where -> where.in(SysUserRole::getUserId, ids));
        sysUserPostMapper.delete(where -> where.in(SysUserPost::getUserId, ids));
        int num = sysUserMapper.deleteByIds(ids);
        Assert.gtZero(num, "删除用户失败");
        return num;
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByUserName(String userName) {
        SysUserVo user = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .eq(SysUser::getUserName, userName)
                .returnType(SysUserVo.class)
                .get();
        return enrichUser(user);
    }

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByPhonenumber(String phonenumber) {
        SysUserVo user = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .eq(SysUser::getPhonenumber, phonenumber)
                .returnType(SysUserVo.class)
                .get();
        return enrichUser(user);
    }

    /**
     * 根据用户ID查询用户所属角色组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(Long userId) {
        List<SysRoleVo> roles = sysRoleMapper.selectRolesByUserId(userId);
        return roles.stream()
                .map(SysRoleVo::getRoleName)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    /**
     * 根据用户ID查询用户所属岗位组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(Long userId) {
        // 查询用户关联的岗位
        List<SysPostVo> posts = QueryChain.of(sysPostMapper)
                .select(SysPost.class)
                .join(SysPost::getPostId, SysUserPost::getPostId)
                .eq(SysUserPost::getUserId, userId)
                .eq(SysPost::getStatus, "0")
                .orderBy(SysPost::getPostSort)
                .returnType(SysPostVo.class)
                .list();

        // 拼接岗位名称
        return posts.stream()
                .map(SysPostVo::getPostName)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user      用户信息
     * @param pageQuery
     * @return 用户信息集合信息
     */
    @Override
    public Page<SysUserVo> selectAllocatedList(SysUserQuery query, PageQuery pageQuery) {
        List<Long> userIds = roleUserIds(query.getRoleId());
        QueryChain<SysUser> queryChain = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .where(query)
                .in(!userIds.isEmpty(), SysUser::getId, userIds)
                .eq(userIds.isEmpty(), SysUser::getId, -1L);
        applyUserDataScope(queryChain);
        Page<SysUserVo> page = pageQuery.applyOrder(queryChain)
                .orderBy(SysUser::getId)
                .returnType(SysUserVo.class)
                .paging(pageQuery.build());
        page.getRows().forEach(this::enrichUser);
        return page;
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user      用户信息
     * @param pageQuery
     * @return 用户信息集合信息
     */
    @Override
    public Page<SysUserVo> selectUnallocatedList(SysUserQuery query, PageQuery pageQuery) {
        List<Long> userIds = roleUserIds(query.getRoleId());
        QueryChain<SysUser> queryChain = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .where(query)
                .notIn(!userIds.isEmpty(), SysUser::getId, userIds);
        applyUserDataScope(queryChain);
        Page<SysUserVo> page = pageQuery.applyOrder(queryChain)
                .orderBy(SysUser::getId)
                .returnType(SysUserVo.class)
                .paging(pageQuery.build());
        page.getRows().forEach(this::enrichUser);
        return page;
    }

    private List<Long> roleUserIds(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return QueryChain.of(sysUserRoleMapper)
                .select(SysUserRole::getUserId)
                .eq(SysUserRole::getRoleId, roleId)
                .list()
                .stream()
                .map(SysUserRole::getUserId)
                .toList();
    }

    /**
     * 通过部门id查询当前部门所有用户
     *
     * @param deptId 部门id
     * @return {@link List }<{@link SysUserVo }> 用户信息列表
     */
    @Override
    public List<SysUserVo> selectUserListByDept(Long deptId) {
        List<SysUserVo> users = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .eq(SysUser::getDeptId, deptId)
                .orderBy(SysUser::getId)
                .returnType(SysUserVo.class).list();
        users.forEach(this::enrichUser);
        return users;
    }

    @Override
    public List<SysUserVo> selectUserByIds(Collection<Long> userIds, Long deptId) {
        QueryChain<SysUser> queryChain = QueryChain.of(sysUserMapper)
                .select(SysUser.class)
                .in(CollUtil.isNotEmpty(userIds), SysUser::getId, userIds)
                .eq(ObjectUtil.isNotNull(deptId), SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, UserConstants.USER_NORMAL)
                .orderBy(SysUser::getId);
        applyUserDataScope(queryChain);
        List<SysUserVo> users = queryChain.returnType(SysUserVo.class).list();
        users.forEach(this::enrichUser);
        return users;
    }

    /**
     * 注册用户信息
     *
     * @param bo 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUserBo bo) {
        SysUser user = prepareRegistrationUser(MapstructUtil.convert(bo, SysUser.class));
        return sysUserMapper.save(user) > 0;
    }

    static SysUser prepareRegistrationUser(SysUser user) {
        user.setCreateBy(0L);
        user.setUpdateBy(0L);
        return user;
    }

    @Override
    public String importUsers(List<SysUserImportVo> users, boolean updateSupport) {
        if (CollUtil.isEmpty(users)) {
            return "恭喜您，数据已全部导入成功！共 0 条，数据如下：";
        }

        String initialPassword = QueryChain.of(sysConfigMapper)
                .select(SysConfig::getConfigValue)
                .eq(SysConfig::getConfigKey, "sys.user.initPassword")
                .returnType(String.class)
                .get();
        if (StringUtil.isBlank(initialPassword)) {
            throw new ServiceException("用户初始密码参数未配置");
        }
        String encodedPassword = BCrypt.hashpw(initialPassword);
        return processImportedUsers(users, updateSupport, encodedPassword);
    }

    String processImportedUsers(List<SysUserImportVo> users, boolean updateSupport, String encodedPassword) {
        List<String> failures = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        int successCount = 0;

        for (int index = 0; index < users.size(); index++) {
            SysUserImportVo imported = users.get(index);
            String userName = StringUtil.trim(imported.getUserName());
            imported.setUserName(userName);
            try {
                boolean[] updated = {false};
                importRowTransactionExecutor.execute(() ->
                        updated[0] = importUser(imported, updateSupport, encodedPassword));
                successCount++;
                String safeName = StringUtil.cleanHtmlTag(StringUtil.defaultIfBlank(userName, "<空>"));
                successes.add(StringUtil.format("{}、账号 {} {}成功",
                        successCount, safeName, updated[0] ? "更新" : "导入"));
            } catch (RuntimeException exception) {
                String safeName = StringUtil.cleanHtmlTag(StringUtil.defaultIfBlank(userName, "<空>"));
                failures.add(StringUtil.format("{}、账号 {} 导入失败：{}",
                        failures.size() + 1, safeName,
                        StringUtil.defaultIfBlank(exception.getMessage(), "未知错误")));
            }
        }

        if (!failures.isEmpty()) {
            throw new ServiceException("很抱歉，导入失败！共 " + failures.size()
                    + " 条数据格式不正确，错误如下：<br/>" + String.join("<br/>", failures));
        }
        String details = successes.isEmpty() ? "" : "<br/>" + String.join("<br/>", successes);
        return StringUtil.format("恭喜您，数据已全部导入成功！共 {} 条，数据如下：{}",
                successCount, details);
    }

    boolean importUser(SysUserImportVo imported, boolean updateSupport, String encodedPassword) {
        SysDept dept = sysDeptMapper.getById(imported.getDeptId());
        if (dept == null) {
            throw new ServiceException("部门不存在");
        }
        if (!dataScopeService.checkUserDataScope(LoginHelper.getUserId(), imported.getDeptId())) {
            throw new ServiceException("没有权限导入该部门用户");
        }

        SysUser existing = QueryChain.of(sysUserMapper)
                .eq(SysUser::getUserName, imported.getUserName())
                .get();
        SysUserBo bo = toImportBo(imported);
        ValidUtils.validateEntity(bo, AddGroup.class);
        if (existing == null) {
            assertImportUnique(bo);
            bo.setPassword(encodedPassword);
            if (!insertByBo(bo)) {
                throw new ServiceException("新增用户失败");
            }
            return false;
        }
        if (!updateSupport) {
            throw new ServiceException("账号已存在");
        }

        checkUserAllowed(existing.getId());
        bo.setId(existing.getId());
        assertImportUnique(bo);
        SysUser update = new SysUser()
                .setId(existing.getId())
                .setDeptId(bo.getDeptId())
                .setUserName(bo.getUserName())
                .setNickName(bo.getNickName())
                .setEmail(bo.getEmail())
                .setPhonenumber(bo.getPhonenumber())
                .setSex(bo.getSex())
                .setStatus(bo.getStatus());
        if (updateUserWithDataScope(update) <= 0) {
            throw new ServiceException("更新用户失败");
        }
        return true;
    }

    private SysUserBo toImportBo(SysUserImportVo imported) {
        SysUserBo bo = new SysUserBo();
        bo.setDeptId(imported.getDeptId());
        bo.setUserName(imported.getUserName());
        bo.setNickName(StringUtil.trim(imported.getNickName()));
        bo.setEmail(StringUtil.trim(imported.getEmail()));
        bo.setPhonenumber(StringUtil.trim(imported.getPhonenumber()));
        bo.setSex(StringUtil.defaultIfBlank(imported.getSex(), "2"));
        bo.setStatus(StringUtil.defaultIfBlank(imported.getStatus(), UserConstants.USER_NORMAL));
        bo.setUserType(UserType.SYS_USER.getUserType());
        return bo;
    }

    private void assertImportUnique(SysUserBo user) {
        if (!checkUserNameUnique(user)) {
            throw new ServiceException("登录账号已存在");
        }
        if (StringUtil.isNotEmpty(user.getPhonenumber()) && !checkPhoneUnique(user)) {
            throw new ServiceException("手机号码已存在");
        }
        if (StringUtil.isNotEmpty(user.getEmail()) && !checkEmailUnique(user)) {
            throw new ServiceException("邮箱账号已存在");
        }
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transaction
    public void insertUserAuth(Long userId, Long[] roleIds) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }
        List<Long> roleList = roleIds == null ? List.of() : Arrays.asList(roleIds);
        insertUserRole(userId, roleList, true);
    }

    private List<Long> validateRoleIds(Long userId, List<Long> roleIds) {
        List<Long> requested = validateDistinctIds(roleIds, "角色");
        if (requested.isEmpty()) {
            return requested;
        }
        List<Long> assignable = new ArrayList<>(requested);
        if (!LoginHelper.isSuperAdmin(userId)) {
            assignable.remove(UserConstants.SUPER_ADMIN_ID);
            if (assignable.isEmpty()) {
                throw new ServiceException("不允许为普通用户分配超级管理员角色，请至少选择一个其他角色");
            }
        }
        List<SysRole> roles = QueryChain.of(sysRoleMapper)
                .in(SysRole::getId, assignable)
                .eq(SysRole::getDelFlag, "0")
                .list();
        if (roles.size() != assignable.size()) {
            throw new ServiceException("角色不存在或已被删除");
        }
        if (!LoginHelper.isSuperAdmin()) {
            DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
            if (roles.stream().anyMatch(role -> !rule.permits(role.getCreateBy(), role.getCreateDept()))) {
                throw new ServiceException("没有权限访问角色的数据");
            }
        }
        return assignable;
    }

    private List<Long> validatePostIds(List<Long> postIds) {
        List<Long> requested = validateDistinctIds(postIds, "岗位");
        if (requested.isEmpty()) {
            return requested;
        }
        List<SysPost> posts = QueryChain.of(sysPostMapper)
                .in(SysPost::getPostId, requested)
                .list();
        if (posts.size() != requested.size()) {
            throw new ServiceException("岗位不存在或已被删除");
        }
        if (!LoginHelper.isSuperAdmin()) {
            DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
            if (posts.stream().anyMatch(post -> !rule.permits(post.getCreateBy(), post.getDeptId()))) {
                throw new ServiceException("没有权限访问岗位的数据");
            }
        }
        return requested;
    }

    private List<Long> validateDistinctIds(List<Long> ids, String relationName) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        if (ids.stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new ServiceException(relationName + "ID不能为空或重复");
        }
        return List.copyOf(ids);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 帐号状态
     * @return 结果
     */
    @Override
    public boolean updateUserStatus(Long userId, String status) {
        return updateUserWithDataScope(new SysUser().setId(userId).setStatus(status)) > 0;
    }

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, Long avatar) {
        return updateUserWithDataScope(new SysUser().setId(userId).setAvatar(avatar)) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public boolean resetUserPwd(Long userId, String password) {
        return updateUserWithDataScope(new SysUser().setId(userId).setPassword(password)) > 0;
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUserBo user) {
        return !sysUserMapper.exists(where -> where
                .eq(SysUser::getUserName, user.getUserName())
                .ne(ObjectUtil.isNotNull(user.getId()), SysUser::getId, user.getId())
        );
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkPhoneUnique(SysUserBo user) {
        return !sysUserMapper.exists(where -> where
                .eq(SysUser::getPhonenumber, user.getPhonenumber())
                .ne(ObjectUtil.isNotNull(user.getId()), SysUser::getId, user.getId())
        );
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkEmailUnique(SysUserBo user) {
        return !sysUserMapper.exists(where -> where
                .eq(SysUser::getEmail, user.getEmail())
                .ne(ObjectUtil.isNotNull(user.getId()), SysUser::getId, user.getId())
        );
    }

    /**
     * 校验用户是否允许操作
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserAllowed(Long userId) {
        if (ObjectUtil.isNotNull(userId) && LoginHelper.isSuperAdmin(userId)) {
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
        if (ObjectUtil.isNull(userId)) {
            return;
        }

        SysUser targetUser = sysUserMapper.getById(userId);
        if (targetUser == null) {
            throw new ServiceException("用户不存在！");
        }

        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (!permitsUser(rule, targetUser)) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }

    static boolean permitsUser(DataScopeRule rule, SysUser user) {
        return rule.permits(user.getCreateBy(), user.getDeptId());
    }

    private int updateUserWithDataScope(SysUser user) {
        DataScopeWriteRule writeRule = resolveWriteDataScope(LoginHelper.getLoginUser());
        return updateUserWithDataScope(user, writeRule);
    }

    DataScopeWriteRule resolveWriteDataScope(LoginUser loginUser) {
        return loginUser == null
                ? DataScopeWriteRule.all()
                : dataScopeService.resolveUserWriteDataScope(loginUser.getUserId());
    }

    int updateUserWithDataScope(SysUser user, DataScopeWriteRule writeRule) {
        if (user == null || user.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        return sysUserMapper.updateWithDataScope(user, writeRule);
    }
}
