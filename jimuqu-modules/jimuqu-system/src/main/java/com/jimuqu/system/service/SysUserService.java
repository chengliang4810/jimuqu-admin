package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.vo.SysUserImportVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.domain.query.SysUserQuery;

import java.util.Collection;
import java.util.List;

/**
 * 用户信息Service接口
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
public interface SysUserService {

    /**
     * 根据主键查询用户信息
     *
     * @param id 用户信息主键
     * @return {@link SysUserVo } 用户信息视图对象
     */
   SysUserVo queryById(Long id);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    SysUserVo selectUserByUserName(String userName);

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    SysUserVo selectUserByPhonenumber(String phonenumber);

    /**
     * 根据用户ID查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    String selectUserRoleGroup(String userName);

    /**
     * 根据用户ID查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
     */
    String selectUserPostGroup(String userName);

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    Page<SysUserVo> selectAllocatedList(SysUserQuery user, PageQuery pageQuery);

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    Page<SysUserVo> selectUnallocatedList(SysUserQuery user, PageQuery pageQuery);

    /**
     * 查询用户信息分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysUserVo }> 用户信息分页对象
     */
    Page<SysUserVo> queryPageList(SysUserQuery query, PageQuery pageQuery);

   /**
     * 查询用户信息列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysUserVo }> 用户信息列表
     */
    List<SysUserVo> queryList(SysUserQuery query);

    /**
     * 通过部门id查询当前部门所有用户
     *
     * @param deptId 部门id
     * @return {@link List }<{@link SysUserVo }> 用户信息列表
     */
    List<SysUserVo> selectUserListByDept(Long deptId);

    /** 按用户ID和部门筛选用户选项。 */
    List<SysUserVo> selectUserByIds(Collection<Long> userIds, Long deptId);

    /**
     * 新增用户信息
     *
     * @param bo 用户信息业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysUserBo bo);

    /**
     * 注册用户信息
     *
     * @param bo 用户信息
     * @return 结果
     */
    boolean registerUser(SysUserBo bo);

    /**
     * 导入用户数据。任一行失败时整批回滚。
     *
     * @param users 导入行
     * @param updateSupport 是否覆盖已存在用户
     * @return 导入结果消息
     */
    String importUsers(List<SysUserImportVo> users, boolean updateSupport);

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    void insertUserAuth(Long userId, Long[] roleIds);

    /**
     * 更新用户信息
     *
     * @param bo 用户信息业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysUserBo bo);

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    int updateUserProfile(SysUserBo user);

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 帐号状态
     * @return 结果
     */
    boolean updateUserStatus(Long userId, String status);

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    boolean updateUserAvatar(Long userId, Long avatar);

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    boolean resetUserPwd(Long userId, String password);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 用户信息主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean checkUserNameUnique(SysUserBo user);

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean checkPhoneUnique(SysUserBo user);

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean checkEmailUnique(SysUserBo user);

    /**
     * 校验用户是否允许操作
     *
     * @param userId 用户ID
     */
    void checkUserAllowed(Long userId);

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    void checkUserDataScope(Long userId);
}
