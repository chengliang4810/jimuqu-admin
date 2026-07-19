package com.jimuqu.common.mybatis.service;

import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeAccess;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;

import java.util.List;

/**
 * 系统数据权限服务接口
 * <p>
 * 提供数据权限相关的业务方法
 * 实现类必须命名为 "sysDataScopeService" 以便SpEL表达式调用
 *
 * @author chengliang4810
 * @version 1.0
 */
public interface ISysDataScopeService {

    /**
     * 聚合用户所有启用角色的数据范围。任何计算异常均返回拒绝规则。
     */
    DataScopeRule resolveUserDataScope(Long userId);

    /**
     * 仅聚合满足当前访问约束的启用角色数据范围。
     */
    DataScopeRule resolveUserDataScope(Long userId, DataScopeAccess access);

    /**
     * 解析当前访问约束下的逐角色写规则。
     */
    DataScopeWriteRule resolveUserWriteDataScope(Long userId);

    /**
     * 仅使用满足指定访问约束的启用角色解析逐角色写规则。
     */
    DataScopeWriteRule resolveUserWriteDataScope(Long userId, DataScopeAccess access);

    /**
     * 获取角色自定义权限部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> getRoleCustom(Long roleId);

    /**
     * 获取部门及以下部门ID列表
     *
     * @param deptId 部门ID
     * @return 部门ID列表
     */
    List<Long> getDeptAndChild(Long deptId);

    /**
     * 获取用户数据权限部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getUserDataScope(Long userId);

    /**
     * 检查用户是否有部门数据权限
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否有权限
     */
    boolean checkUserDataScope(Long userId, Long deptId);

    /**
     * 检查一条同时具有用户和部门归属的数据。
     */
    boolean checkUserDataScope(Long userId, Long recordUserId, Long deptId);

}
